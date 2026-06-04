package com.mesawa.cuidarproximo.ui.model

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.mercadopago.android.px.core.MercadoPagoCheckout
import com.mercadopago.android.px.configuration.AdvancedConfiguration
import com.mercadopago.android.px.model.PaymentResult

class PagamentoActivity : AppCompatActivity() {

    private lateinit var txtCuidador: TextView
    private lateinit var txtHoras: TextView
    private lateinit var txtTotal: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private var contratacaoId: String = ""
    private var valorTotal: Double = 0.0
    private var cuidadorNome: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagamento)
        supportActionBar?.hide()

        txtCuidador = findViewById(R.id.txtCuidador)
        txtHoras = findViewById(R.id.txtHorasPagamento)
        txtTotal = findViewById(R.id.txtTotalPagamento)

        contratacaoId = intent.getStringExtra("contratacaoId") ?: ""
        valorTotal = intent.getDoubleExtra("valor", 0.0)
        cuidadorNome = intent.getStringExtra("cuidadorNome") ?: "Cuidador"

        txtCuidador.text = cuidadorNome
        txtTotal.text = "R$ %.2f".format(valorTotal)
        txtHoras.text = intent.getIntExtra("horas", 1).toString() + "h"

        iniciarCheckout()
    }

    private fun iniciarCheckout() {
        val url =
            "https://us-central1-cuidar-proximo-e6d51.cloudfunctions.net/criarPreferenceHTTP"

        val json = """
            {
                "nome": "$cuidadorNome",
                "valor": $valorTotal
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string()
                val preferenceId = JSONObject(result!!).getString("id")

                runOnUiThread {

                    val checkout = MercadoPagoCheckout.Builder(
                        "SUA_PUBLIC_KEY_AQUI",
                        preferenceId
                    )
                        .setAdvancedConfiguration(
                            AdvancedConfiguration.Builder()
                                .build()
                        )
                        .build()

                    checkout.startForPayment(this, 123)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erro pagamento: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123 && data != null) {

            val paymentResult = MercadoPagoCheckout.getPaymentResultFromIntent(data)

            if (paymentResult?.paymentStatus == com.mercadopago.android.px.model.Payment.Status.APPROVED) {

                firestore.collection("contratacoes")
                    .document(contratacaoId)
                    .update(
                        mapOf(
                            "status" to "aguardando_cuidador",
                            "pagamentoStatus" to "aprovado",
                            "metodoPagamento" to "CARTAO"
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pagamento aprovado!", Toast.LENGTH_SHORT).show()
                        abrirTelaAndamento()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao atualizar pagamento", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Pagamento não aprovado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirTelaAndamento() {
        val intent = android.content.Intent(this, EmAndamentoActivity::class.java)
        startActivity(intent)
        finish()
    }
}