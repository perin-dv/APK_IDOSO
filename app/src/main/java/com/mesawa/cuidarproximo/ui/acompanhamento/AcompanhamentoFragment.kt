package com.mesawa.cuidarproximo.ui.acompanhamento

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoActivity
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoCartaoActivity
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoPixActivity
import java.text.NumberFormat
import java.util.Locale

class AcompanhamentoFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    private lateinit var cardContrato: CardView
    private lateinit var cardVazio: CardView
    private lateinit var txtStatusContrato: TextView
    private lateinit var txtCuidador: TextView
    private lateinit var txtResumo: TextView
    private lateinit var txtHeroStatus: TextView
    private lateinit var txtCodigoPedido: TextView
    private lateinit var txtValorPedido: TextView
    private lateinit var txtPagamento: TextView
    private lateinit var txtEtapaPedido: TextView
    private lateinit var txtEtapaPagamento: TextView
    private lateinit var txtEtapaCuidado: TextView
    private lateinit var txtEndereco: TextView
    private lateinit var txtObservacao: TextView
    private lateinit var btnAbrirAndamento: MaterialButton

    private var contratacaoIdAtual = ""
    private var contratacaoOwnerIdAtual = ""
    private var cuidadorAtual = ""
    private var horasAtual = 0
    private var valorAtual = 0.0
    private var statusAtual = ""
    private var metodoPagamentoAtual = ""
    private var pagamentoStatusAtual = ""
    private var qrBase64Atual = ""
    private var qrStringAtual = ""
    private var checkoutCartaoUrlAtual = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_acompanhamento, container, false)

        cardContrato = view.findViewById(R.id.cardContrato)
        cardVazio = view.findViewById(R.id.cardVazio)
        txtHeroStatus = view.findViewById(R.id.txtHeroStatus)
        txtStatusContrato = view.findViewById(R.id.txtStatusContrato)
        txtCuidador = view.findViewById(R.id.txtCuidador)
        txtResumo = view.findViewById(R.id.txtResumo)
        txtCodigoPedido = view.findViewById(R.id.txtCodigoPedido)
        txtValorPedido = view.findViewById(R.id.txtValorPedido)
        txtPagamento = view.findViewById(R.id.txtPagamento)
        txtEtapaPedido = view.findViewById(R.id.txtEtapaPedido)
        txtEtapaPagamento = view.findViewById(R.id.txtEtapaPagamento)
        txtEtapaCuidado = view.findViewById(R.id.txtEtapaCuidado)
        txtEndereco = view.findViewById(R.id.txtEndereco)
        txtObservacao = view.findViewById(R.id.txtObservacao)
        btnAbrirAndamento = view.findViewById(R.id.btnAbrirAndamento)

        btnAbrirAndamento.setOnClickListener {
            abrirAcaoPrincipal()
        }

        carregarUltimaContratacao()

        return view
    }

    override fun onResume() {
        super.onResume()
        carregarUltimaContratacao()
    }

    private fun carregarUltimaContratacao() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            mostrarVazio()
            return
        }

        carregarClienteDocumentoId(uid) { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) {
                carregarUltimaContratacaoLegada(uid)
                return@carregarClienteDocumentoId
            }

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("contratacoes")
                .get()
                .addOnSuccessListener { result ->
                    val contrato = result.documents.maxByOrNull { doc ->
                        doc.getTimestamp("updatedAt")?.toDate()?.time
                            ?: doc.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }

                    if (contrato == null) {
                        carregarUltimaContratacaoLegada(uid)
                    } else {
                        mostrarContrato(contrato)
                    }
                }
                .addOnFailureListener {
                    carregarUltimaContratacaoLegada(uid)
                }
        }
    }

    private fun carregarClienteDocumentoId(uid: String, callback: (String) -> Unit) {
        firestore.collection("clientes")
            .whereEqualTo("sistema.uid_auth", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                callback(query.documents.firstOrNull()?.id.orEmpty())
            }
            .addOnFailureListener {
                callback("")
            }
    }

    private fun carregarUltimaContratacaoLegada(uid: String) {
        firestore.collection("contratacoes")
            .whereEqualTo("clienteId", uid)
            .get()
            .addOnSuccessListener { result ->
                val contrato = result.documents.maxByOrNull { doc ->
                    doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: 0L
                }

                if (contrato == null) {
                    mostrarVazio()
                } else {
                    mostrarContrato(contrato)
                }
            }
            .addOnFailureListener {
                mostrarVazio()
            }
    }

    private fun mostrarContrato(doc: DocumentSnapshot) {
        contratacaoIdAtual = doc.getString("id") ?: doc.id
        contratacaoOwnerIdAtual = doc.getString("contratacaoOwnerId").orEmpty()

        val cuidador = doc.getString("cuidadorNome") ?: "Cuidador"
        val idoso = doc.getString("idosoNome").orEmpty().ifBlank { "Idoso" }
        val horas = doc.getLong("horas")?.toInt() ?: 0
        val valor = doc.getDouble("valorTotal") ?: 0.0
        val status = traduzirStatus(doc.getString("status").orEmpty())
        val pagamentoRaw = doc.getString("pagamentoStatus").orEmpty()
        val pagamento = traduzirPagamento(pagamentoRaw)
        val endereco = doc.getString("endereco").orEmpty().ifBlank { "Endereco nao informado" }
        val observacao = doc.getString("observacao").orEmpty().ifBlank { "Sem observacoes adicionais." }
        val codigo = doc.getString("codigoContratacao") ?: doc.id
        cuidadorAtual = cuidador
        horasAtual = horas
        valorAtual = valor
        statusAtual = doc.getString("status").orEmpty()
        metodoPagamentoAtual = doc.getString("metodoPagamento").orEmpty()
        pagamentoStatusAtual = pagamentoRaw
        qrBase64Atual = doc.getString("qrBase64").orEmpty()
        qrStringAtual = doc.getString("qrString").orEmpty()
        checkoutCartaoUrlAtual = doc.getString("checkoutCartaoUrl").orEmpty()

        cardContrato.visibility = View.VISIBLE
        cardVazio.visibility = View.GONE

        txtHeroStatus.text = status
        txtStatusContrato.text = status
        txtCuidador.text = cuidador
        txtCodigoPedido.text = "Pedido $codigo"
        txtResumo.text = "$idoso\n${horas}h de cuidado"
        txtValorPedido.text = "${moeda.format(valor)}\nvalor total"
        txtPagamento.text = "Pagamento: $pagamento"
        txtEndereco.text = "Endereco: $endereco"
        txtObservacao.text = "Observacao: $observacao"
        atualizarEtapas(doc.getString("status").orEmpty(), pagamentoRaw)
        atualizarBotaoPrincipal(doc.getString("status").orEmpty(), pagamentoRaw)
    }

    private fun mostrarVazio() {
        contratacaoIdAtual = ""
        contratacaoOwnerIdAtual = ""
        cardContrato.visibility = View.GONE
        cardVazio.visibility = View.VISIBLE
    }

    private fun atualizarBotaoPrincipal(status: String, pagamento: String) {
        val aguardandoPagamento =
            status.lowercase(Locale.ROOT) == "aguardando_pagamento" ||
                pagamento.lowercase(Locale.ROOT) == "pending" ||
                pagamento.isBlank()

        btnAbrirAndamento.text =
            if (aguardandoPagamento) "Continuar pagamento" else "Acompanhar em tempo real"
    }

    private fun abrirAcaoPrincipal() {
        if (contratacaoIdAtual.isBlank()) return

        val aguardandoPagamento =
            statusAtual.lowercase(Locale.ROOT) == "aguardando_pagamento" ||
                pagamentoStatusAtual.lowercase(Locale.ROOT) == "pending" ||
                pagamentoStatusAtual.isBlank()

        if (aguardandoPagamento) {
            abrirPagamentoPendente()
        } else {
            abrirAcompanhamentoTempoReal()
        }
    }

    private fun abrirPagamentoPendente() {
        when {
            metodoPagamentoAtual.lowercase(Locale.ROOT) == "pix" &&
                qrBase64Atual.isNotBlank() &&
                qrStringAtual.isNotBlank() -> {
                startActivity(
                    Intent(requireContext(), PagamentoPixActivity::class.java)
                        .putExtra("contratacaoId", contratacaoIdAtual)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerIdAtual)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorAtual)
                        .putExtra("qr_base64", qrBase64Atual)
                        .putExtra("qr_string", qrStringAtual)
                )
            }
            metodoPagamentoAtual.lowercase(Locale.ROOT) == "cartao" &&
                checkoutCartaoUrlAtual.isNotBlank() -> {
                startActivity(
                    Intent(requireContext(), PagamentoCartaoActivity::class.java)
                        .putExtra("contratacaoId", contratacaoIdAtual)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerIdAtual)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorAtual)
                        .putExtra("init_point", checkoutCartaoUrlAtual)
                )
            }
            else -> {
                startActivity(
                    Intent(requireContext(), PagamentoActivity::class.java)
                        .putExtra("contratacaoId", contratacaoIdAtual)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerIdAtual)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorAtual)
                        .putExtra("horas", horasAtual)
                )
            }
        }
    }

    private fun abrirAcompanhamentoTempoReal() {
        startActivity(
            Intent(requireContext(), EmAndamentoActivity::class.java)
                .putExtra("contratacaoId", contratacaoIdAtual)
                .putExtra("contratacaoOwnerId", contratacaoOwnerIdAtual)
        )
    }

    private fun traduzirStatus(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "aguardando_pagamento" -> "Aguardando pagamento"
            "em_andamento" -> "Em andamento"
            "finalizado" -> "Finalizado"
            "pagamento_recusado" -> "Pagamento recusado"
            else -> status.ifBlank { "Acompanhamento" }
        }

    private fun traduzirPagamento(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "approved" -> "Pagamento aprovado"
            "pending" -> "Pagamento pendente"
            "rejected" -> "Pagamento recusado"
            else -> status.ifBlank { "Pagamento nao informado" }
        }

    private fun atualizarEtapas(status: String, pagamento: String) {
        val statusNormalizado = status.lowercase(Locale.ROOT)
        val pagamentoNormalizado = pagamento.lowercase(Locale.ROOT)

        txtEtapaPedido.text = "Pedido\ncriado"
        txtEtapaPagamento.text =
            if (pagamentoNormalizado == "approved") "Pagamento\naprovado" else "Pagamento\npendente"
        txtEtapaCuidado.text =
            when (statusNormalizado) {
                "em_andamento" -> "Cuidado\nem andamento"
                "finalizado" -> "Cuidado\nfinalizado"
                "aguardando_pagamento" -> "Cuidado\naguardando"
                else -> "Cuidado\nacompanhando"
            }
    }

}
