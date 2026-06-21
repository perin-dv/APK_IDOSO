const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

const { MercadoPagoConfig, Preference, Payment } = require("mercadopago");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

const mpToken = defineSecret("MP_ACCESS_TOKEN");

function getMpToken() {
  const token = String(mpToken.value() || "").trim();

  if (
    !token ||
    token.length < 40 ||
    (!token.startsWith("TEST-") && !token.startsWith("APP_USR-"))
  ) {
    throw new HttpsError(
      "failed-precondition",
      "MP_ACCESS_TOKEN inválido. Configure o Access Token puro do Mercado Pago, começando com TEST- ou APP_USR-."
    );
  }

  return token;
}

function getClient() {
  const token = getMpToken();

  return new MercadoPagoConfig({
    accessToken: token,
  });
}

function arredondar(valor) {
  return Number(Number(valor).toFixed(2));
}

function validarString(valor, nomeCampo) {
  if (!valor || typeof valor !== "string" || valor.trim() === "") {
    throw new HttpsError("invalid-argument", `${nomeCampo} obrigatório`);
  }

  return valor.trim();
}

function calcularSplit(valorTotal) {
  const taxaPlataforma = 0.09;
  const valorComissao = arredondar(valorTotal * taxaPlataforma);
  const valorLiquidoCuidador = arredondar(valorTotal - valorComissao);

  return {
    taxaPlataforma,
    valorComissao,
    valorLiquidoCuidador,
  };
}

function dataDocumento() {
  return new Date().toISOString().slice(0, 10).replace(/-/g, "");
}

function pagamentoDocId(prefixo, id) {
  return `${dataDocumento()}_${prefixo}_${String(id).replace(/[^\w-]/g, "")}`;
}

function getContratacaoRef(contratacaoId, contratacaoOwnerId = "") {
  const ownerId = String(contratacaoOwnerId || "").trim();

  if (ownerId) {
    return db
      .collection("clientes")
      .doc(ownerId)
      .collection("contratacoes")
      .doc(contratacaoId);
  }

  return db.collection("contratacoes").doc(contratacaoId);
}

async function salvarTentativaPagamento(contratacaoRef, docId, dados) {
  await contratacaoRef.collection("pagamentos").doc(docId).set(
    {
      ...dados,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
}

function normalizarPaymentId(valor) {
  if (valor === undefined || valor === null) {
    return "";
  }

  return String(valor).trim();
}

async function buscarPagamentoMercadoPago(paymentId) {
  const id = normalizarPaymentId(paymentId);

  if (!id) {
    throw new HttpsError("invalid-argument", "paymentId obrigatório");
  }

  const client = getClient();
  const payment = new Payment(client);

  return payment.get({ id });
}

function tratarErroMercadoPago(error, acao) {
  const message = String(error?.message || "");
  const code = String(error?.code || error?.error || "");
  const causeText = JSON.stringify(error?.cause || "");
  const textoCompleto = `${message} ${code} ${causeText}`.toLowerCase();

  if (
    code === "unauthorized" ||
    textoCompleto.includes("unauthorized") ||
    textoCompleto.includes("authorization value not present")
  ) {
    throw new HttpsError(
      "failed-precondition",
      `Mercado Pago recusou a credencial ao ${acao}. Para testes use o Access Token TEST- da sua aplicação; para produção use um APP_USR de conta real habilitada.`
    );
  }

  throw new HttpsError("internal", message || `Erro ao ${acao}`);
}

function isErroCredencialMercadoPago(error) {
  const message = String(error?.message || "");
  const code = String(error?.code || error?.error || "");
  const causeText = JSON.stringify(error?.cause || "");
  const textoCompleto = `${message} ${code} ${causeText}`.toLowerCase();

  return (
    code === "unauthorized" ||
    textoCompleto.includes("unauthorized") ||
    textoCompleto.includes("authorization value not present")
  );
}

async function criarCheckoutContratacao({
  contratacaoId,
  contratacaoOwnerId = "",
  contratacao,
  metodoPreferido = "cartao",
}) {
  const valorTotal = Number(contratacao.valorTotal);
  const client = getClient();
  const preference = new Preference(client);
  const nomeCuidador = contratacao.cuidadorNome || "Cuidador";
  const { valorComissao, valorLiquidoCuidador } = calcularSplit(valorTotal);

  const body = {
      items: [
        {
          title: `Serviço de cuidador: ${nomeCuidador}`,
          quantity: 1,
          unit_price: valorTotal,
          currency_id: "BRL",
        },
      ],
      external_reference: contratacaoId,
      metadata: {
        contratacaoId,
        contratacaoOwnerId,
        splitEmpresaPercentual: 9,
        splitCuidadorPercentual: 91,
      },
      notification_url:
        "https://us-central1-cuidar-proximo-e6d51.cloudfunctions.net/webhookMercadoPago",
      back_urls: {
        success: `cuidarproximo://pagamento/cartao?resultado=sucesso&contratacaoId=${contratacaoId}&contratacaoOwnerId=${contratacaoOwnerId}`,
        failure: `cuidarproximo://pagamento/cartao?resultado=erro&contratacaoId=${contratacaoId}&contratacaoOwnerId=${contratacaoOwnerId}`,
        pending: `cuidarproximo://pagamento/cartao?resultado=pendente&contratacaoId=${contratacaoId}&contratacaoOwnerId=${contratacaoOwnerId}`,
      },
      auto_return: "approved",
  };

  if (metodoPreferido === "pix") {
    body.payment_methods = {
      excluded_payment_types: [
        { id: "credit_card" },
        { id: "debit_card" },
        { id: "ticket" },
      ],
      installments: 1,
    };
  } else if (metodoPreferido === "cartao") {
    body.payment_methods = {
      excluded_payment_types: [
        { id: "bank_transfer" },
        { id: "ticket" },
        { id: "atm" },
      ],
      installments: 1,
    };
  }

  const response = await preference.create({ body });
  const token = getMpToken();
  const checkoutUrl =
    token.startsWith("TEST-") && response?.sandbox_init_point
      ? response.sandbox_init_point
      : response?.init_point;

  if (!response?.id || !checkoutUrl) {
    throw new Error("Falha ao criar checkout Mercado Pago");
  }

  return {
    preferenceId: String(response.id),
    initPoint: checkoutUrl,
    sandbox: checkoutUrl === response.sandbox_init_point,
    valorComissao,
    valorLiquidoCuidador,
  };
}

async function confirmarPagamentoReal({
  contratacaoId,
  contratacaoOwnerId = "",
  paymentId,
  uid = null,
  metodoPagamento,
}) {
  const pagamento = await buscarPagamentoMercadoPago(paymentId);
  const status = pagamento?.status || "unknown";
  const externalReference = pagamento?.external_reference || "";
  const valorPago = Number(pagamento?.transaction_amount);

  if (status !== "approved") {
    throw new HttpsError(
      "failed-precondition",
      `Pagamento Mercado Pago ainda não aprovado: ${status}`
    );
  }

  if (externalReference !== contratacaoId) {
    throw new HttpsError(
      "failed-precondition",
      "Pagamento não pertence a esta contratação"
    );
  }

  const contratacaoRef = getContratacaoRef(contratacaoId, contratacaoOwnerId);

  await db.runTransaction(async (transaction) => {
    const contratacaoSnap = await transaction.get(contratacaoRef);

    if (!contratacaoSnap.exists) {
      throw new HttpsError("not-found", "Contratação não encontrada");
    }

    const contratacao = contratacaoSnap.data();

    if (uid && contratacao.clienteId !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Você não pode confirmar esta contratação"
      );
    }

    if (contratacao.repasseConfirmado === true) {
      return;
    }

    const cuidadorId = contratacao.cuidadorId;
    const valorTotal = arredondar(Number(contratacao.valorTotal));

    if (!cuidadorId || !valorTotal || valorTotal <= 0) {
      throw new HttpsError(
        "failed-precondition",
        "Dados da contratação inválidos"
      );
    }

    if (!valorPago || arredondar(valorPago) !== valorTotal) {
      throw new HttpsError(
        "failed-precondition",
        "Valor pago não confere com a contratação"
      );
    }

    const { taxaPlataforma, valorComissao, valorLiquidoCuidador } =
      calcularSplit(valorTotal);

    const walletRef = db.collection("wallets").doc(cuidadorId);
    const platformRef = db.collection("platform").doc("earnings");

    transaction.set(
      walletRef,
      {
        pendente: FieldValue.increment(valorLiquidoCuidador),
        totalGanho: FieldValue.increment(valorLiquidoCuidador),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    transaction.set(
      platformRef,
      {
        total: FieldValue.increment(valorComissao),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    const metodoFinal =
      metodoPagamento === "auto"
        ? pagamento?.payment_method_id === "pix" ? "pix" : "cartao"
        : metodoPagamento;

    transaction.update(contratacaoRef, {
      status: "pago",
      pagamentoStatus: "approved",
      mercadoPagoStatus: status,
      mercadoPagoStatusDetail: pagamento?.status_detail || null,
      metodoPagamento: metodoFinal,
      paymentId: String(pagamento.id),
      paymentMethodId: pagamento?.payment_method_id || null,
      repasseConfirmado: true,
      taxaPlataforma,
      valorComissao,
      valorLiquidoCuidador,
      paidAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    const pagamentoRef = contratacaoRef
      .collection("pagamentos")
      .doc(pagamentoDocId("pay", pagamento.id));

    transaction.set(
      pagamentoRef,
      {
        tipo: "confirmacao",
        metodo: metodoFinal,
        status,
        paymentId: String(pagamento.id),
        paymentMethodId: pagamento?.payment_method_id || null,
        mercadoPagoStatusDetail: pagamento?.status_detail || null,
        valorTotal,
        valorComissao,
        valorLiquidoCuidador,
        confirmedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });

  return {
    ok: true,
    paymentId: String(pagamento.id),
    status,
  };
}


// ======================================================
// PIX - CRIA PAGAMENTO SEM DUPLICAR
// ======================================================
exports.criarPagamentoMarketplace = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const contratacaoId = validarString(
        request.data?.contratacaoId,
        "contratacaoId"
      );
      const contratacaoOwnerId = String(request.data?.contratacaoOwnerId || "").trim();

      const contratacaoRef = getContratacaoRef(contratacaoId, contratacaoOwnerId);
      const contratacaoSnap = await contratacaoRef.get();

      if (!contratacaoSnap.exists) {
        throw new HttpsError("not-found", "Contratação não encontrada");
      }

      const contratacao = contratacaoSnap.data();

      if (contratacao.clienteId !== uid) {
        throw new HttpsError(
          "permission-denied",
          "Você não pode pagar esta contratação"
        );
      }

      if (!contratacao.cuidadorId) {
        throw new HttpsError("failed-precondition", "Cuidador inválido");
      }

      const valorTotal = Number(contratacao.valorTotal);

      if (!valorTotal || valorTotal <= 0) {
        throw new HttpsError("failed-precondition", "Valor da contratação inválido");
      }

      // Se já existe PIX pendente, retorna o mesmo pagamento.
      // Isso evita criar vários PIX para a mesma contratação.
      if (
        contratacao.paymentId &&
        contratacao.qrString &&
        contratacao.qrBase64 &&
        contratacao.pagamentoStatus === "pending"
      ) {
        return {
          paymentId: contratacao.paymentId,
          status: contratacao.pagamentoStatus,
          qr_base64: contratacao.qrBase64,
          qr_string: contratacao.qrString,
          reutilizado: true,
        };
      }

      if (contratacao.pagamentoStatus === "approved") {
        throw new HttpsError(
          "failed-precondition",
          "Esta contratação já foi paga"
        );
      }

      const nomeCuidador = contratacao.cuidadorNome || "Cuidador";
      const emailPagador =
        request.data?.payerEmail ||
        contratacao.clienteEmail ||
        "pagador@email.com";

      const idempotencyKey = `pix-${contratacaoId}`;
      let response;

      try {
        const client = getClient();
        const payment = new Payment(client);

        response = await payment.create({
          body: {
            transaction_amount: valorTotal,
            description: `Serviço de cuidador: ${nomeCuidador}`,
            payment_method_id: "pix",
            external_reference: contratacaoId,
            metadata: {
              contratacaoId,
              contratacaoOwnerId,
            },
            payer: {
              email: emailPagador,
            },
          },
          requestOptions: {
            idempotencyKey,
          },
        });
      } catch (mpError) {
        if (isErroCredencialMercadoPago(mpError)) {
          logger.warn("PIX QR indisponível para esta credencial.", {
            contratacaoId,
            code: mpError?.code || mpError?.error || null,
            message: mpError?.message || null,
            cause: mpError?.cause || null,
          });

          throw new HttpsError(
            "failed-precondition",
            "Mercado Pago recusou a credencial ao criar PIX via API. Use um Access Token de conta vendedora habilitada para PIX API; sem isso o QR Code/copia e cola não é retornado."
          );
        }

        logger.error("Erro Mercado Pago ao criar PIX.", {
          contratacaoId,
          code: mpError?.code || mpError?.error || null,
        });

        throw mpError;
      }

      const qr = response?.point_of_interaction?.transaction_data;

      if (!response?.id || !qr?.qr_code || !qr?.qr_code_base64) {
        throw new Error("QR Code PIX não foi gerado pelo Mercado Pago");
      }

      const { taxaPlataforma, valorComissao, valorLiquidoCuidador } =
        calcularSplit(valorTotal);

      await contratacaoRef.update({
        paymentId: String(response.id),
        preferenceId: null,
        checkoutCartaoUrl: null,
        metodoPagamento: "pix",
        pagamentoStatus: response.status || "pending",
        status: "aguardando_pagamento",

        qrString: qr.qr_code,
        qrBase64: qr.qr_code_base64,

        taxaPlataforma,
        valorComissao,
        valorLiquidoCuidador,

        updatedAt: FieldValue.serverTimestamp(),
      });

      await salvarTentativaPagamento(
        contratacaoRef,
        pagamentoDocId("pix", response.id),
        {
          tipo: "pix",
          metodo: "pix",
          status: response.status || "pending",
          paymentId: String(response.id),
          qrString: qr.qr_code,
          qrBase64: qr.qr_code_base64,
          valorTotal,
          valorComissao,
          valorLiquidoCuidador,
          createdAt: FieldValue.serverTimestamp(),
        }
      );

      return {
        paymentId: String(response.id),
        status: response.status || "pending",
        qr_base64: qr.qr_code_base64,
        qr_string: qr.qr_code,
        reutilizado: false,
        split: {
          empresa: valorComissao,
          cuidador: valorLiquidoCuidador,
        },
      };

    } catch (error) {
      logger.error("ERRO AO CRIAR PIX:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "criar PIX");
    }
  }
);

exports.criarPagamentoPix = exports.criarPagamentoMarketplace;


// ======================================================
// VERIFICAR STATUS DO PIX
// ======================================================
exports.verificarStatusPagamento = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const contratacaoId = validarString(
        request.data?.contratacaoId,
        "contratacaoId"
      );
      const contratacaoOwnerId = String(request.data?.contratacaoOwnerId || "").trim();

      const contratacaoRef = getContratacaoRef(contratacaoId, contratacaoOwnerId);
      const contratacaoSnap = await contratacaoRef.get();

      if (!contratacaoSnap.exists) {
        throw new HttpsError("not-found", "Contratação não encontrada");
      }

      const contratacao = contratacaoSnap.data();

      if (contratacao.clienteId !== uid && contratacao.cuidadorId !== uid) {
        throw new HttpsError(
          "permission-denied",
          "Você não pode consultar este pagamento"
        );
      }

      if (!contratacao.paymentId) {
        throw new HttpsError("failed-precondition", "Pagamento ainda não criado");
      }

      const client = getClient();
      const payment = new Payment(client);

      const response = await payment.get({
        id: contratacao.paymentId,
      });

      const statusAtual = response.status || "pending";

      await contratacaoRef.update({
        pagamentoStatus: statusAtual,
        status: statusAtual === "approved"
          ? "pago"
          : statusAtual === "rejected"
            ? "pagamento_recusado"
            : "aguardando_pagamento",
        updatedAt: FieldValue.serverTimestamp(),
      });

      return {
        status: statusAtual,
      };

    } catch (error) {
      logger.error("ERRO AO VERIFICAR STATUS:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "verificar status do pagamento");
    }
  }
);


// ======================================================
// CONFIRMAR PAGAMENTO E LIBERAR CARTEIRA
// Chame esta função depois que verificarStatusPagamento retornar approved.
// Depois podemos trocar isso por webhook.
// ======================================================
exports.confirmarPagamentoContratacao = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const contratacaoId = validarString(
        request.data?.contratacaoId,
        "contratacaoId"
      );
      const contratacaoOwnerId = String(request.data?.contratacaoOwnerId || "").trim();

      const contratacaoRef = getContratacaoRef(contratacaoId, contratacaoOwnerId);
      const contratacaoSnap = await contratacaoRef.get();

      if (!contratacaoSnap.exists) {
        throw new HttpsError("not-found", "Contratação não encontrada");
      }

      const contratacao = contratacaoSnap.data();

      return await confirmarPagamentoReal({
        contratacaoId,
        contratacaoOwnerId,
        paymentId: contratacao.paymentId,
        uid,
        metodoPagamento: "pix",
      });

    } catch (error) {
      logger.error("ERRO AO CONFIRMAR PAGAMENTO:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "confirmar pagamento");
    }
  }
);

// ======================================================
// CHECKOUT PRO - CARTÃO AMARRADO À CONTRATAÇÃO
// ======================================================
exports.criarCheckoutCartaoContratacao = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const contratacaoId = validarString(
        request.data?.contratacaoId,
        "contratacaoId"
      );
      const contratacaoOwnerId = String(request.data?.contratacaoOwnerId || "").trim();

      const contratacaoRef = getContratacaoRef(contratacaoId, contratacaoOwnerId);
      const contratacaoSnap = await contratacaoRef.get();

      if (!contratacaoSnap.exists) {
        throw new HttpsError("not-found", "Contratação não encontrada");
      }

      const contratacao = contratacaoSnap.data();

      if (contratacao.clienteId !== uid) {
        throw new HttpsError(
          "permission-denied",
          "Você não pode pagar esta contratação"
        );
      }

      if (contratacao.pagamentoStatus === "approved") {
        throw new HttpsError(
          "failed-precondition",
          "Esta contratação já foi paga"
        );
      }

      const valorTotal = Number(contratacao.valorTotal);

      if (!valorTotal || valorTotal <= 0) {
        throw new HttpsError("failed-precondition", "Valor da contratação inválido");
      }

      const checkout = await criarCheckoutContratacao({
        contratacaoId,
        contratacaoOwnerId,
        contratacao,
        metodoPreferido: "cartao",
      });

      await contratacaoRef.update({
        preferenceId: checkout.preferenceId,
        metodoPagamento: "cartao",
        pagamentoStatus: "pending",
        status: "aguardando_pagamento",
        taxaPlataforma: 0.09,
        valorComissao: checkout.valorComissao,
        valorLiquidoCuidador: checkout.valorLiquidoCuidador,
        checkoutCartaoUrl: checkout.initPoint,
        checkoutSandbox: checkout.sandbox,
        updatedAt: FieldValue.serverTimestamp(),
      });

      await salvarTentativaPagamento(
        contratacaoRef,
        pagamentoDocId("pref", checkout.preferenceId),
        {
          tipo: "checkout_pro",
          metodo: "cartao",
          status: "pending",
          preferenceId: checkout.preferenceId,
          checkoutUrl: checkout.initPoint,
          sandbox: checkout.sandbox,
          valorTotal,
          valorComissao: checkout.valorComissao,
          valorLiquidoCuidador: checkout.valorLiquidoCuidador,
          createdAt: FieldValue.serverTimestamp(),
        }
      );

      return {
        preferenceId: checkout.preferenceId,
        init_point: checkout.initPoint,
        sandbox: checkout.sandbox,
        split: {
          empresa: checkout.valorComissao,
          cuidador: checkout.valorLiquidoCuidador,
        },
      };

    } catch (error) {
      logger.error("ERRO AO CRIAR CHECKOUT CARTÃO:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "criar checkout do cartão");
    }
  }
);

exports.confirmarPagamentoCartaoMercadoPago = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const contratacaoId = validarString(
        request.data?.contratacaoId,
        "contratacaoId"
      );
      const contratacaoOwnerId = String(request.data?.contratacaoOwnerId || "").trim();

      const paymentId = validarString(
        request.data?.paymentId,
        "paymentId"
      );

      return await confirmarPagamentoReal({
        contratacaoId,
        contratacaoOwnerId,
        paymentId,
        uid,
        metodoPagamento: "auto",
      });

    } catch (error) {
      logger.error("ERRO AO CONFIRMAR CARTÃO MERCADO PAGO:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "confirmar cartão");
    }
  }
);

exports.confirmarPagamentoCartaoTeste = exports.confirmarPagamentoCartaoMercadoPago;

exports.webhookMercadoPago = onRequest(
  { secrets: [mpToken] },
  async (req, res) => {
    try {
      const paymentId =
        normalizarPaymentId(req.query?.id) ||
        normalizarPaymentId(req.query?.["data.id"]) ||
        normalizarPaymentId(req.body?.id) ||
        normalizarPaymentId(req.body?.data?.id);

      const topic =
        req.query?.topic ||
        req.query?.type ||
        req.body?.topic ||
        req.body?.type ||
        "";

      if (!paymentId || !String(topic).includes("payment")) {
        return res.status(200).json({ ok: true, ignored: true });
      }

      const pagamento = await buscarPagamentoMercadoPago(paymentId);
      const contratacaoId = pagamento?.external_reference;
      const contratacaoOwnerId =
        pagamento?.metadata?.contratacaoOwnerId ||
        pagamento?.metadata?.contratacao_owner_id ||
        "";

      if (!contratacaoId || pagamento?.status !== "approved") {
        return res.status(200).json({
          ok: true,
          status: pagamento?.status || "unknown",
        });
      }

      await confirmarPagamentoReal({
        contratacaoId,
        contratacaoOwnerId,
        paymentId,
        metodoPagamento:
          pagamento?.payment_method_id === "pix" ? "pix" : "cartao",
      });

      return res.status(200).json({ ok: true });
    } catch (error) {
      logger.error("ERRO WEBHOOK MERCADO PAGO:", error);

      return res.status(200).json({
        ok: false,
        error: error.message || "Erro interno",
      });
    }
  }
);


// ======================================================
// CHECKOUT PRO - CARTÃO / REDIRECIONAMENTO
// Mantive, mas limpei validação básica.
// ======================================================
exports.criarPreference = onCall(
  { secrets: [mpToken] },
  async (request) => {
    try {
      const uid = request.auth?.uid;

      if (!uid) {
        throw new HttpsError("unauthenticated", "Usuário não autenticado");
      }

      const nome = validarString(request.data?.nome, "nome");
      const valorTotal = Number(request.data?.valor);

      if (isNaN(valorTotal) || valorTotal <= 0) {
        throw new HttpsError("invalid-argument", "Valor inválido");
      }

      const client = getClient();
      const preference = new Preference(client);

      const response = await preference.create({
        body: {
          items: [
            {
              title: `Serviço de ${nome}`,
              quantity: 1,
              unit_price: valorTotal,
              currency_id: "BRL",
            },
          ],
          back_urls: {
            success: "cuidarproximo://pagamento/sucesso",
            failure: "cuidarproximo://pagamento/erro",
            pending: "cuidarproximo://pagamento/pendente",
          },
          auto_return: "approved",
        },
      });

      if (!response?.id) {
        throw new Error("Falha ao criar preferência Mercado Pago");
      }

      const { valorComissao, valorLiquidoCuidador } =
        calcularSplit(valorTotal);

      return {
        preferenceId: response.id,
        init_point: response.init_point,
        taxaEmpresa: valorComissao,
        valorCuidador: valorLiquidoCuidador,
      };

    } catch (error) {
      logger.error("CHECKOUT ERROR:", error);

      if (error instanceof HttpsError) {
        throw error;
      }

      tratarErroMercadoPago(error, "criar preferência");
    }
  }
);


// ======================================================
// HTTP CHECKOUT
// Eu deixaria desativado se o app usa onCall.
// HTTP público é mais fácil de abusar.
// ======================================================
exports.criarPreferenceHTTP = onRequest(
  { secrets: [mpToken] },
  async (req, res) => {
    try {
      return res.status(403).json({
        error: "Use a função callable criarPreference pelo app autenticado.",
      });
    } catch (error) {
      logger.error(error);
      return res.status(500).json({ error: error.message });
    }
  }
);
