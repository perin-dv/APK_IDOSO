package com.mesawa.cuidarproximo.ui.andamento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.mesawa.cuidarproximo.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoActivity
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoCartaoActivity
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoPixActivity
import java.text.NumberFormat
import java.util.Locale

class EmAndamentoActivity : BaseActivity() {

    private lateinit var txtCodigoContratacao: TextView
    private lateinit var txtNomeCuidador: TextView
    private lateinit var txtStatusAtendimento: TextView
    private lateinit var txtResumoAtendimento: TextView
    private lateinit var txtETA: TextView
    private lateinit var txtIdosoAtendimento: TextView
    private lateinit var txtEnderecoAtendimento: TextView
    private lateinit var txtObservacaoAtendimento: TextView
    private lateinit var txtOrigemRota: TextView
    private lateinit var txtDestinoRota: TextView

    private lateinit var cardLigar: CardView
    private lateinit var cardChat: CardView
    private lateinit var cardMapaRota: CardView
    private lateinit var btnContinuarPagamento: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private val moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    private var contratacaoId = ""
    private var contratacaoOwnerId = ""
    private var telefoneCuidador = ""
    private var origemRota = ""
    private var destinoRota = ""
    private var cuidadorNomeAtual = ""
    private var valorAtual = 0.0
    private var horasAtual = 0
    private var metodoPagamentoAtual = ""
    private var qrBase64Atual = ""
    private var qrStringAtual = ""
    private var checkoutCartaoUrlAtual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_em_andamento)
        supportActionBar?.hide()

        txtCodigoContratacao = findViewById(R.id.txtCodigoContratacao)
        txtNomeCuidador = findViewById(R.id.txtNomeCuidador)
        txtStatusAtendimento = findViewById(R.id.txtStatusAtendimento)
        txtResumoAtendimento = findViewById(R.id.txtResumoAtendimento)
        txtETA = findViewById(R.id.txtETA)
        txtIdosoAtendimento = findViewById(R.id.txtIdosoAtendimento)
        txtEnderecoAtendimento = findViewById(R.id.txtEnderecoAtendimento)
        txtObservacaoAtendimento = findViewById(R.id.txtObservacaoAtendimento)
        txtOrigemRota = findViewById(R.id.txtOrigemRota)
        txtDestinoRota = findViewById(R.id.txtDestinoRota)

        cardLigar = findViewById(R.id.cardLigar)
        cardChat = findViewById(R.id.cardChat)
        cardMapaRota = findViewById(R.id.cardMapaRota)
        btnContinuarPagamento = findViewById(R.id.btnContinuarPagamento)

        contratacaoId = intent.getStringExtra("contratacaoId").orEmpty()
        contratacaoOwnerId = intent.getStringExtra("contratacaoOwnerId").orEmpty()

        cardLigar.setOnClickListener {
            val numero = telefoneCuidador.ifBlank {
                Toast.makeText(this, "Telefone do cuidador nao informado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$numero")
            startActivity(intent)
        }

        cardChat.setOnClickListener {
            val numero = telefoneCuidador.ifBlank {
                Toast.makeText(this, "Telefone do cuidador nao informado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mensagem = Uri.encode("Ola, estou acompanhando o atendimento contratado.")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/$numero?text=$mensagem")
            startActivity(intent)
        }

        cardMapaRota.setOnClickListener {
            abrirRotaNoMapa()
        }

        btnContinuarPagamento.setOnClickListener {
            abrirPagamentoPendente()
        }

        acompanharStatusRealtime()
    }

    private fun acompanharStatusRealtime() {
        if (contratacaoId.isBlank()) {
            Toast.makeText(this, "Contratação inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val docRef = if (contratacaoOwnerId.isNotBlank()) {
            firestore.collection("clientes")
                .document(contratacaoOwnerId)
                .collection("contratacoes")
                .document(contratacaoId)
        } else {
            firestore.collection("contratacoes")
                .document(contratacaoId)
        }

        listener = docRef
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val nomeCuidador = snapshot.getString("cuidadorNome") ?: "Cuidador"
                val status = traduzirStatus(snapshot.getString("status").orEmpty())
                val statusRaw = snapshot.getString("status").orEmpty()
                val pagamentoRaw = snapshot.getString("pagamentoStatus").orEmpty()
                val eta = snapshot.getString("eta") ?: "12 min"
                val idoso = snapshot.getString("idosoNome").orEmpty().ifBlank { "Idoso nao informado" }
                val endereco = snapshot.getString("endereco").orEmpty().ifBlank { "Endereco nao informado" }
                val observacao = snapshot.getString("observacao").orEmpty().ifBlank { "Sem observacoes adicionais" }
                val horas = snapshot.getLong("horas")?.toInt() ?: 0
                val valor = snapshot.getDouble("valorTotal") ?: 0.0
                cuidadorNomeAtual = nomeCuidador
                horasAtual = horas
                valorAtual = valor
                metodoPagamentoAtual = snapshot.getString("metodoPagamento").orEmpty()
                qrBase64Atual = snapshot.getString("qrBase64").orEmpty()
                qrStringAtual = snapshot.getString("qrString").orEmpty()
                checkoutCartaoUrlAtual = snapshot.getString("checkoutCartaoUrl").orEmpty()
                val origemTexto = snapshot.getString("origemCuidador")
                    ?: snapshot.getString("cuidadorEnderecoOrigem")
                    ?: ""
                val cuidadorLat = snapshot.getDouble("cuidadorLatitude")
                    ?: snapshot.getDouble("cuidadorLat")
                    ?: snapshot.getDouble("latitudeCuidador")
                val cuidadorLng = snapshot.getDouble("cuidadorLongitude")
                    ?: snapshot.getDouble("cuidadorLng")
                    ?: snapshot.getDouble("longitudeCuidador")
                val destinoLat = snapshot.getDouble("destinoLatitude")
                    ?: snapshot.getDouble("idosoLatitude")
                val destinoLng = snapshot.getDouble("destinoLongitude")
                    ?: snapshot.getDouble("idosoLongitude")

                telefoneCuidador =
                    snapshot.getString("cuidadorTelefone")
                        ?: snapshot.getString("telefoneCuidador")
                        ?: ""

                origemRota = when {
                    cuidadorLat != null && cuidadorLng != null -> "$cuidadorLat,$cuidadorLng"
                    origemTexto.isNotBlank() -> origemTexto
                    else -> ""
                }

                destinoRota = when {
                    destinoLat != null && destinoLng != null -> "$destinoLat,$destinoLng"
                    endereco.isNotBlank() && endereco != "Endereco nao informado" -> endereco
                    else -> ""
                }

                txtCodigoContratacao.text = "Contrato ${snapshot.getString("codigoContratacao") ?: snapshot.id}"
                txtNomeCuidador.text = nomeCuidador
                txtStatusAtendimento.text = status
                txtETA.text = eta
                txtResumoAtendimento.text = "${horas}h • ${moeda.format(valor)}"
                txtIdosoAtendimento.text = "Idoso: $idoso"
                txtEnderecoAtendimento.text = "Endereco: $endereco"
                txtObservacaoAtendimento.text = "Observacao: $observacao"
                txtOrigemRota.text =
                    if (origemRota.isBlank()) {
                        "Origem: aguardando localizacao do cuidador"
                    } else {
                        "Origem: cuidador localizado"
                    }
                txtDestinoRota.text = "Destino: $endereco"
                atualizarPagamentoPendente(statusRaw, pagamentoRaw)
            }
    }

    private fun atualizarPagamentoPendente(status: String, pagamento: String) {
        val pendente =
            status.lowercase(Locale.ROOT) == "aguardando_pagamento" ||
                pagamento.lowercase(Locale.ROOT) == "pending" ||
                pagamento.isBlank()
        btnContinuarPagamento.visibility = if (pendente) View.VISIBLE else View.GONE
        btnContinuarPagamento.text = when (metodoPagamentoAtual.lowercase(Locale.ROOT)) {
            "pix" -> "Voltar para o PIX"
            "cartao" -> "Continuar pagamento no cartao"
            else -> "Continuar pagamento"
        }
    }

    private fun abrirPagamentoPendente() {
        when {
            metodoPagamentoAtual.lowercase(Locale.ROOT) == "pix" &&
                qrBase64Atual.isNotBlank() &&
                qrStringAtual.isNotBlank() -> {
                startActivity(
                    Intent(this, PagamentoPixActivity::class.java)
                        .putExtra("contratacaoId", contratacaoId)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerId)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorNomeAtual)
                        .putExtra("qr_base64", qrBase64Atual)
                        .putExtra("qr_string", qrStringAtual)
                )
            }
            metodoPagamentoAtual.lowercase(Locale.ROOT) == "cartao" &&
                checkoutCartaoUrlAtual.isNotBlank() -> {
                startActivity(
                    Intent(this, PagamentoCartaoActivity::class.java)
                        .putExtra("contratacaoId", contratacaoId)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerId)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorNomeAtual)
                        .putExtra("init_point", checkoutCartaoUrlAtual)
                )
            }
            else -> {
                startActivity(
                    Intent(this, PagamentoActivity::class.java)
                        .putExtra("contratacaoId", contratacaoId)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerId)
                        .putExtra("valor", valorAtual)
                        .putExtra("cuidadorNome", cuidadorNomeAtual)
                        .putExtra("horas", horasAtual)
                )
            }
        }
    }

    private fun abrirRotaNoMapa() {
        if (destinoRota.isBlank()) {
            Toast.makeText(this, "Destino do atendimento nao informado", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = if (origemRota.isBlank()) {
            Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(destinoRota)}"
            )
        } else {
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${Uri.encode(origemRota)}" +
                    "&destination=${Uri.encode(destinoRota)}" +
                    "&travelmode=driving"
            )
        }

        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun traduzirStatus(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "aguardando_pagamento" -> "Aguardando pagamento"
            "aguardando_cuidador" -> "Aguardando cuidador"
            "em_andamento" -> "Atendimento em andamento"
            "finalizado" -> "Atendimento finalizado"
            "pagamento_recusado" -> "Pagamento recusado"
            else -> status.ifBlank { "Acompanhamento ativo" }
        }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
