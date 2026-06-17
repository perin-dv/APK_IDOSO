package com.mesawa.cuidarproximo.ui.pagamento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.functions.FirebaseFunctions
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity

class PagamentoActivity : AppCompatActivity() {

    private lateinit var txtCuidador: TextView
    private lateinit var txtHoras: TextView
    private lateinit var txtTotal: TextView

    private lateinit var cardPix: CardView
    private lateinit var cardCartao: CardView

    private var contratacaoId: String = ""
    private var valorTotal: Double = 0.0
    private var cuidadorNome: String = ""
    private var horas: Int = 0

    private lateinit var functions: FirebaseFunctions
    private var processandoPagamento = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagamento)

        supportActionBar?.hide()

        txtCuidador = findViewById(R.id.txtCuidador)
        txtHoras = findViewById(R.id.txtHorasPagamento)
        txtTotal = findViewById(R.id.txtTotalPagamento)
        cardPix = findViewById(R.id.cardPix)
        cardCartao = findViewById(R.id.cardCartao)

        // Dados recebidos
        contratacaoId = intent.getStringExtra("contratacaoId") ?: ""
        valorTotal = intent.getDoubleExtra("valor", 0.0)
        cuidadorNome = intent.getStringExtra("cuidadorNome") ?: "Cuidador"
        horas = intent.getIntExtra("horas", 1)

        // UI
        txtCuidador.text = cuidadorNome
        txtHoras.text = "${horas}h"
        txtTotal.text = "R$ %.2f".format(valorTotal)

        // Firebase Functions
        functions = FirebaseFunctions.getInstance("us-central1")

        if (tratarRetornoCartao(intent?.data)) {
            return
        }

        // PIX
        cardPix.setOnClickListener {
            gerarPixNoBackend()
        }

        // CARTÃO
        cardCartao.setOnClickListener {
            gerarCheckoutCartao()
        }
    }

    private fun gerarPixNoBackend() {
        if (processandoPagamento) return

        if (contratacaoId.isBlank()) {
            Toast.makeText(this, "Contratação inválida", Toast.LENGTH_LONG).show()
            return
        }

        setProcessando(true)
        Log.d("PagamentoActivity", "Gerando PIX contratacaoId=$contratacaoId")

        functions.getHttpsCallable("criarPagamentoMarketplace")
            .call(
                mapOf(
                    "contratacaoId" to contratacaoId
                )
            )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val paymentId = data?.get("paymentId")?.toString()
                val qrBase64 = data?.get("qr_base64") as? String
                val qrString = data?.get("qr_string") as? String
                Log.d(
                    "PagamentoActivity",
                    "Resposta PIX paymentId=$paymentId temQr=${!qrBase64.isNullOrEmpty()}"
                )

                if (!qrBase64.isNullOrEmpty() && !qrString.isNullOrEmpty()) {
                    val intent = Intent(this, PagamentoPixActivity::class.java)
                    intent.putExtra("contratacaoId", contratacaoId)
                    intent.putExtra("paymentId", paymentId)
                    intent.putExtra("valor", valorTotal)
                    intent.putExtra("cuidadorNome", cuidadorNome)
                    intent.putExtra("qr_base64", qrBase64)
                    intent.putExtra("qr_string", qrString)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Mercado Pago não retornou QR Code PIX",
                        Toast.LENGTH_LONG
                    ).show()
                }

                setProcessando(false)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao gerar PIX: ${it.message}", Toast.LENGTH_LONG).show()
                setProcessando(false)
            }
    }

    private fun gerarCheckoutCartao() {
        if (processandoPagamento) return

        if (contratacaoId.isBlank()) {
            Toast.makeText(this, "Contratação inválida", Toast.LENGTH_LONG).show()
            return
        }

        setProcessando(true)
        Log.d("PagamentoActivity", "Gerando checkout cartao contratacaoId=$contratacaoId")

        functions.getHttpsCallable("criarCheckoutCartaoContratacao")
            .call(
                mapOf(
                    "contratacaoId" to contratacaoId
                )
            )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val initPoint = data?.get("init_point") as? String
                Log.d(
                    "PagamentoActivity",
                    "Resposta cartao temInitPoint=${!initPoint.isNullOrBlank()}"
                )

                if (!initPoint.isNullOrBlank()) {
                    abrirCheckoutWeb(initPoint)
                } else {
                    Toast.makeText(this, "Erro ao abrir checkout do cartão", Toast.LENGTH_LONG).show()
                }

                setProcessando(false)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao abrir cartão: ${it.message}", Toast.LENGTH_LONG).show()
                setProcessando(false)
            }
    }

    private fun tratarRetornoCartao(data: Uri?): Boolean {
        if (data?.scheme != "cuidarproximo" || data.host != "pagamento") {
            return false
        }

        val resultado = data.getQueryParameter("resultado")
        val idRetorno = data.getQueryParameter("contratacaoId").orEmpty()

        if (resultado.isNullOrBlank() || idRetorno.isBlank()) {
            return false
        }

        contratacaoId = idRetorno
        txtCuidador.text = "Validando pagamento"
        txtHoras.text = "-"
        txtTotal.text = "-"

        val paymentId =
            data.getQueryParameter("payment_id")
                ?: data.getQueryParameter("collection_id")

        when (resultado) {
            "sucesso" -> confirmarCartaoAprovado(idRetorno, paymentId)
            "pendente" -> {
                Toast.makeText(this, "Pagamento pendente no cartão", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Pagamento não aprovado", Toast.LENGTH_LONG).show()
            }
        }

        return true
    }

    private fun confirmarCartaoAprovado(idRetorno: String, paymentId: String?) {
        if (paymentId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Mercado Pago não retornou o ID do pagamento",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        functions.getHttpsCallable("confirmarPagamentoCartaoMercadoPago")
            .call(
                mapOf(
                    "contratacaoId" to idRetorno,
                    "paymentId" to paymentId
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Pagamento aprovado!", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, EmAndamentoActivity::class.java)
                        .putExtra("contratacaoId", idRetorno)
                )
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao confirmar cartão: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setProcessando(processando: Boolean) {
        processandoPagamento = processando
        cardPix.isEnabled = !processando
        cardCartao.isEnabled = !processando
    }

    private fun abrirCheckoutWeb(url: String) {
        Log.d("PagamentoActivity", "Abrindo checkout web url=$url")
        val intent = Intent(this, PagamentoCartaoActivity::class.java).apply {
            putExtra("contratacaoId", contratacaoId)
            putExtra("valor", valorTotal)
            putExtra("cuidadorNome", cuidadorNome)
            putExtra("init_point", url)
        }
        startActivity(intent)
    }
}
