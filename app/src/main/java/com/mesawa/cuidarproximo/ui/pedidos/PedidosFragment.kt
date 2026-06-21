package com.mesawa.cuidarproximo.ui.pedidos

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity
import java.text.NumberFormat
import java.util.Locale

class PedidosFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    private lateinit var txtResumoPedidos: TextView
    private lateinit var lista: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_pedidos, container, false)
        txtResumoPedidos = view.findViewById(R.id.txtResumoPedidos)
        lista = view.findViewById(R.id.layoutListaPedidos)
        carregarPedidos()
        return view
    }

    override fun onResume() {
        super.onResume()
        if (::lista.isInitialized) carregarPedidos()
    }

    private fun carregarPedidos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            mostrarVazio()
            return
        }

        carregarClienteDocumentoId(uid) { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) {
                carregarPedidosLegados(uid)
                return@carregarClienteDocumentoId
            }

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("contratacoes")
                .get()
                .addOnSuccessListener { result ->
                    val pedidos = result.documents.sortedByDescending {
                        it.getTimestamp("updatedAt")?.toDate()?.time
                            ?: it.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }
                    lista.removeAllViews()
                    txtResumoPedidos.text = "${pedidos.size} servico(s) encontrado(s)"
                    if (pedidos.isEmpty()) {
                        carregarPedidosLegados(uid)
                        return@addOnSuccessListener
                    }
                    pedidos.forEach { lista.addView(cardPedido(it)) }
                }
                .addOnFailureListener { carregarPedidosLegados(uid) }
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

    private fun carregarPedidosLegados(uid: String) {
        firestore.collection("contratacoes")
            .whereEqualTo("clienteId", uid)
            .get()
            .addOnSuccessListener { result ->
                val pedidos = result.documents.sortedByDescending {
                    it.getTimestamp("updatedAt")?.toDate()?.time
                        ?: it.getTimestamp("createdAt")?.toDate()?.time
                        ?: 0L
                }
                lista.removeAllViews()
                txtResumoPedidos.text = "${pedidos.size} servico(s) encontrado(s)"
                if (pedidos.isEmpty()) {
                    mostrarVazio()
                    return@addOnSuccessListener
                }
                pedidos.forEach { lista.addView(cardPedido(it)) }
            }
            .addOnFailureListener { mostrarVazio() }
    }

    private fun cardPedido(doc: DocumentSnapshot): CardView {
        val status = traduzirStatus(doc.getString("status").orEmpty())
        val pagamento = traduzirPagamento(doc.getString("pagamentoStatus").orEmpty())
        val cuidador = doc.getString("cuidadorNome") ?: "Cuidador"
        val idoso = doc.getString("idosoNome").orEmpty().ifBlank { "Idoso" }
        val valor = moeda.format(doc.getDouble("valorTotal") ?: 0.0)
        val horas = doc.getLong("horas")?.toInt() ?: 0
        val id = doc.getString("id") ?: doc.id

        return CardView(requireContext()).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener {
                startActivity(
                    Intent(requireContext(), EmAndamentoActivity::class.java)
                        .putExtra("contratacaoId", id)
                        .putExtra("contratacaoOwnerId", doc.getString("contratacaoOwnerId").orEmpty())
                )
            }
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                addView(text(status, "#085EAA", 14f, true))
                addView(text(cuidador, "#111827", 22f, true).apply { setPadding(0, dp(6), 0, 0) })
                addView(text("$idoso • ${horas}h • $valor", "#374151", 15f, false).apply { setPadding(0, dp(6), 0, 0) })
                addView(text(pagamento, "#047857", 14f, true).apply {
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    background = rounded("#ECFDF5", 18)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = dp(12)
                    layoutParams = lp
                })
            })
        }
    }

    private fun mostrarVazio() {
        lista.removeAllViews()
        txtResumoPedidos.text = "Nenhum pedido encontrado"
        lista.addView(TextView(requireContext()).apply {
            text = "Quando voce contratar um cuidador, seus pedidos aparecem aqui com status, pagamento e recibo."
            textSize = 16f
            setTextColor(Color.parseColor("#40606D"))
            setPadding(dp(10), dp(24), dp(10), dp(10))
        })
    }

    private fun traduzirStatus(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "aguardando_pagamento" -> "Aguardando pagamento"
            "em_andamento" -> "Em andamento"
            "finalizado" -> "Finalizado"
            "pagamento_recusado" -> "Pagamento recusado"
            else -> status.ifBlank { "Pedido criado" }
        }

    private fun traduzirPagamento(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "approved" -> "Pagamento aprovado"
            "pending" -> "Pagamento pendente"
            "rejected" -> "Pagamento recusado"
            else -> status.ifBlank { "Pagamento nao informado" }
        }

    private fun text(text: String, color: String, size: Float, bold: Boolean): TextView =
        TextView(requireContext()).apply {
            this.text = text
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun rounded(color: String, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

}
