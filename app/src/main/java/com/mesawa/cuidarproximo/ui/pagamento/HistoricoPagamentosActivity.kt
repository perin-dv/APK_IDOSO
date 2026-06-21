package com.mesawa.cuidarproximo.ui.pagamento

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.BaseActivity
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.profile.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoricoPagamentosActivity : BaseActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: "sem_login"
    private val formato = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    private lateinit var lista: LinearLayout
    private lateinit var periodo: TextView
    private lateinit var resumo: TextView
    private var inicio: Long? = null
    private var fim: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F8FB"))
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        root.addView(heroPagamentos())

        val filtroCard = card("#FFFFFF", 22)
        val filtro = content()
        filtro.addView(text("Filtros", "#111827", 18f, true))
        periodo = text("Periodo: todos", "#40606D", 14f, false).apply { setPadding(0, dp(6), 0, 0) }
        filtro.addView(periodo)

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        linha.addView(botaoData("Inicio") { escolherData(true) })
        linha.addView(botaoData("Fim") { escolherData(false) })
        filtro.addView(linha)
        filtro.addView(Button(this).apply {
            text = "Limpar filtros"
            setTextColor(Color.parseColor("#085EAA"))
            background = roundedStroke("#FFFFFF", "#BFD9EA", 14)
            setOnClickListener {
                inicio = null
                fim = null
                atualizarPeriodo()
                carregarPagamentos()
            }
        })
        filtroCard.addView(filtro)
        root.addView(filtroCard)

        resumo = text("Carregando...", "#111827", 18f, true).apply {
            setPadding(0, dp(22), 0, dp(2))
        }
        root.addView(resumo)

        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(lista)

        setContentView(ScrollView(this).apply { addView(root) })
        carregarPagamentos()
    }

    private fun botaoData(texto: String, clique: () -> Unit): Button =
        Button(this).apply {
            text = texto
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded("#085EAA", 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = dp(8) }
            setOnClickListener { clique() }
        }

    private fun escolherData(ehInicio: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                cal.set(ano, mes, dia, if (ehInicio) 0 else 23, if (ehInicio) 0 else 59, if (ehInicio) 0 else 59)
                if (ehInicio) inicio = cal.timeInMillis else fim = cal.timeInMillis
                atualizarPeriodo()
                carregarPagamentos()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun atualizarPeriodo() {
        val ini = inicio?.let { formato.format(Date(it)) } ?: "inicio"
        val ate = fim?.let { formato.format(Date(it)) } ?: "hoje"
        periodo.text = if (inicio == null && fim == null) "Periodo: todos" else "Periodo: $ini ate $ate"
    }

    private fun carregarPagamentos() {
        lista.removeAllViews()
        lista.addView(text("Carregando historico...", "#6B7280", 15f, false).apply {
            setPadding(0, dp(16), 0, 0)
        })

        carregarClienteDocumentoId { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) {
                lista.removeAllViews()
                resumo.text = "Nenhum pagamento encontrado"
                mostrarVazio()
                return@carregarClienteDocumentoId
            }

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("contratacoes")
                .get()
            .addOnSuccessListener { result ->
                lista.removeAllViews()
                val itens = result.documents
                    .map { doc ->
                        val data = doc.getTimestamp("pagoEm")
                            ?: doc.getTimestamp("updatedAt")
                            ?: doc.getTimestamp("createdAt")
                            ?: Timestamp(Date(0))
                        PagamentoResumo(
                            cuidador = doc.getString("cuidadorNome") ?: "Cuidador",
                            status = doc.getString("pagamentoStatus") ?: doc.getString("status") ?: "pendente",
                            metodo = doc.getString("paymentMethodId") ?: "nao informado",
                            valor = doc.getDouble("valorTotal") ?: 0.0,
                            data = data.toDate()
                        )
                    }
                    .filter { item ->
                        val time = item.data.time
                        (inicio == null || time >= inicio!!) && (fim == null || time <= fim!!)
                    }
                    .sortedByDescending { it.data.time }

                if (itens.isEmpty()) {
                    resumo.text = "Nenhum pagamento encontrado"
                    mostrarVazio()
                    return@addOnSuccessListener
                }

                val total = itens.sumOf { it.valor }
                resumo.text = "${itens.size} pagamento(s) - R$ %.2f".format(total)
                itens.forEach { lista.addView(cardPagamento(it)) }
            }
            .addOnFailureListener {
                lista.removeAllViews()
                resumo.text = "Erro ao carregar"
                lista.addView(text("Nao foi possivel carregar os pagamentos.", "#DC2626", 16f, false))
            }
        }
    }

    private fun carregarClienteDocumentoId(callback: (String) -> Unit) {
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

    private fun cardPagamento(item: PagamentoResumo) =
        card("#FFFFFF", 20).apply {
            val content = content()
            val top = LinearLayout(this@HistoricoPagamentosActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            top.addView(LinearLayout(this@HistoricoPagamentosActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(text("R$ %.2f".format(item.valor), "#085EAA", 24f, true))
                addView(text(item.cuidador, "#111827", 16f, true).apply { setPadding(0, dp(4), 0, 0) })
            })
            top.addView(badge(traduzirStatus(item.status), statusColor(item.status)))
            content.addView(top)
            content.addView(line())
            content.addView(text("Data: ${formato.format(item.data)}", "#4B5563", 14f, false))
            content.addView(text("Metodo: ${traduzirMetodo(item.metodo)}", "#4B5563", 14f, false).apply {
                setPadding(0, dp(5), 0, 0)
            })
            addView(content)
        }

    private fun mostrarVazio() {
        val card = card("#E7F6F7", 24)
        val content = content().apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
        content.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_elder_address)
            layoutParams = LinearLayout.LayoutParams(dp(104), dp(104))
        })
        content.addView(text("Sem historico por aqui", "#0F2F3D", 23f, true))
        content.addView(text("Quando uma contratacao for paga, o comprovante aparece aqui com data, valor, metodo e status.", "#40606D", 15f, false).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        card.addView(content)
        lista.addView(card)
    }

    private fun traduzirStatus(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "approved" -> "aprovado"
            "pending" -> "pendente"
            "rejected" -> "recusado"
            else -> status
        }

    private fun traduzirMetodo(metodo: String): String =
        when (metodo.lowercase(Locale.ROOT)) {
            "pix" -> "PIX"
            "visa", "master", "amex", "elo" -> "Cartao"
            "account_money" -> "Saldo Mercado Pago"
            "nao informado" -> "Nao informado"
            else -> metodo
        }

    private fun statusColor(status: String): String =
        when (status.lowercase(Locale.ROOT)) {
            "approved" -> "#16A34A"
            "rejected" -> "#DC2626"
            else -> "#D97706"
        }

    private fun heroPagamentos(): CardView =
        card("#085EAA", 24).apply {
            val content = content()
            content.addView(text("Pagamentos", "#FFFFFF", 28f, true))
            content.addView(text("Acompanhe valores, datas e status das suas contratacoes.", "#DDEDF7", 15f, false).apply {
                setPadding(0, dp(6), 0, 0)
            })
            addView(content)
        }

    private fun card(color: String, radius: Int): CardView =
        CardView(this).apply {
            this.radius = dp(radius).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.parseColor(color))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }

    private fun content(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

    private fun text(text: String, color: String, size: Float, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun badge(text: String, color: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(color))
            background = roundedStroke("#FFFFFF", color, 18)
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }

    private fun line(): android.view.View =
        android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#E5EEF3"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            }
        }

    private fun rounded(color: String, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radius).toFloat()
        }

    private fun roundedStroke(color: String, stroke: String, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            setStroke(dp(1), Color.parseColor(stroke))
            cornerRadius = dp(radius).toFloat()
        }

    private data class PagamentoResumo(
        val cuidador: String,
        val status: String,
        val metodo: String,
        val valor: Double,
        val data: Date
    )
}
