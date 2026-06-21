package com.mesawa.cuidarproximo.ui.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.BaseActivity
import com.mesawa.cuidarproximo.R

class FaleConoscoActivity : BaseActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: "sem_login"

    private lateinit var telefone: EditText
    private lateinit var mensagem: EditText
    private lateinit var inputChatIa: EditText
    private lateinit var chatContainerIa: LinearLayout
    private lateinit var chatScrollIa: ScrollView
    private var assuntoSelecionado = "Atendimento"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F8FB"))
            setPadding(dp(16), dp(16), dp(16), dp(28))
        }

        root.addView(hero())
        root.addView(canaisCard())
        root.addView(mensagemCard())
        root.addView(iaCard())

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun hero(): CardView =
        card("#DDF5F6", 26).apply {
            val row = LinearLayout(this@FaleConoscoActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }
            row.addView(ImageView(this@FaleConoscoActivity).apply {
                setImageResource(R.drawable.logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(104), dp(104))
            })
            row.addView(LinearLayout(this@FaleConoscoActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(text("Fale conosco", "#0F2F3D", 27f, true))
                addView(text("SAC, retorno por telefone, chat e assistente para duvidas rapidas.", "#40606D", 14f, false).apply {
                    setPadding(0, dp(6), 0, 0)
                })
            })
            addView(row)
        }

    private fun canaisCard(): CardView =
        card("#FFFFFF", 24).apply {
            val content = content()
            content.addView(text("Suporte Cuidar Proximo", "#111827", 20f, true))
            content.addView(text("Escolha o melhor canal para falar com a gente.", "#6B7280", 14f, false).apply {
                setPadding(0, dp(5), 0, dp(12))
            })

            val row = LinearLayout(this@FaleConoscoActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(actionButton("Ligar SAC", "#085EAA") {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+5544999999999")))
            })
            row.addView(actionButton("Chat", "#10B7C4") {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/5544999999999?text=${Uri.encode("Ola, preciso de suporte no Cuidar Proximo.")}")
                    )
                )
            })
            content.addView(row)
            content.addView(text("Telefone SAC: (44) 99999-9999", "#374151", 15f, true).apply {
                setPadding(0, dp(14), 0, 0)
            })
            content.addView(text("Email: suporte@cuidarproximo.com", "#4B5563", 14f, false).apply {
                setPadding(0, dp(5), 0, 0)
            })
            addView(content)
        }

    private fun mensagemCard(): CardView =
        card("#FFFFFF", 24).apply {
            val content = content()
            content.addView(text("Pedir retorno", "#111827", 20f, true))
            content.addView(text("Informe seu telefone e conte o que aconteceu. O SAC retorna com contexto.", "#6B7280", 14f, false).apply {
                setPadding(0, dp(5), 0, dp(10))
            })

            content.addView(assuntosRow())
            telefone = input("Telefone para retorno", InputType.TYPE_CLASS_PHONE)
            mensagem = input("Mensagem para o SAC", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, 4)
            content.addView(telefone)
            content.addView(mensagem)
            content.addView(Button(this@FaleConoscoActivity).apply {
                text = "Enviar mensagem"
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                background = rounded("#085EAA", 18)
                setPadding(0, dp(12), 0, dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(14) }
                setOnClickListener { enviarMensagem() }
            })
            addView(content)
        }

    private fun iaCard(): CardView =
        card("#EAF7F8", 24).apply {
            val content = content()
            content.addView(text("Chat com assistente IA", "#0F2F3D", 20f, true))
            content.addView(text("Pergunte sobre pedidos, pagamentos, endereco, acompanhamento ou avaliacoes.", "#40606D", 14f, false).apply {
                setPadding(0, dp(5), 0, dp(10))
            })

            chatScrollIa = ScrollView(this@FaleConoscoActivity).apply {
                background = rounded("#FFFFFF", 20)
                isFillViewport = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(300)
                ).apply { topMargin = dp(12) }
            }

            chatContainerIa = LinearLayout(this@FaleConoscoActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            chatScrollIa.addView(chatContainerIa)
            content.addView(chatScrollIa)

            adicionarMensagemIa(
                "Oi, eu sou a assistente do Cuidar Proximo. Posso te ajudar com pagamento, pedido, endereco, acompanhamento e avaliacoes."
            )

            val row = LinearLayout(this@FaleConoscoActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
            }

            inputChatIa = EditText(this@FaleConoscoActivity).apply {
                hint = "Digite sua duvida..."
                inputType = InputType.TYPE_CLASS_TEXT
                setSingleLine(true)
                setTextColor(Color.parseColor("#111827"))
                setHintTextColor(Color.parseColor("#7B8794"))
                background = stroke("#FFFFFF", "#C8E5EA", 18)
                setPadding(dp(14), 0, dp(14), 0)
                layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f)
            }

            row.addView(inputChatIa)
            row.addView(Button(this@FaleConoscoActivity).apply {
                text = "Enviar"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                background = rounded("#10B7C4", 18)
                layoutParams = LinearLayout.LayoutParams(dp(92), dp(54)).apply {
                    marginStart = dp(8)
                }
                setOnClickListener { enviarMensagemIa() }
            })
            content.addView(row)
            addView(content)
        }

    private fun assuntosRow(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(chip("Atendimento"))
            addView(chip("Pagamento"))
            addView(chip("Melhoria"))
        }

    private fun chip(texto: String): TextView =
        TextView(this).apply {
            text = texto
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#085EAA"))
            background = rounded("#EEF2FF", 18)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = dp(8); bottomMargin = dp(8) }
            setOnClickListener {
                assuntoSelecionado = texto
                Toast.makeText(this@FaleConoscoActivity, "Assunto: $texto", Toast.LENGTH_SHORT).show()
            }
        }

    private fun input(hint: String, type: Int, linhas: Int = 1): EditText =
        EditText(this).apply {
            this.hint = hint
            inputType = type
            minLines = linhas
            setSingleLine(linhas == 1)
            minHeight = if (linhas > 1) dp(112) else dp(54)
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#7B8794"))
            background = stroke("#FFFFFF", "#D6E2EA", 16)
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }

    private fun actionButton(text: String, color: String, click: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(color, 18)
            layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f).apply { marginEnd = dp(8) }
            setOnClickListener { click() }
        }

    private fun enviarMensagem() {
        val tel = telefone.text.toString().trim()
        val msg = mensagem.text.toString().trim()

        if (tel.isBlank() || msg.isBlank()) {
            Toast.makeText(this, "Informe telefone e mensagem", Toast.LENGTH_LONG).show()
            return
        }

        firestore.collection("suporte_solicitacoes").add(
            hashMapOf(
                "usuarioId" to uid,
                "telefone" to tel,
                "assunto" to assuntoSelecionado,
                "mensagem" to msg,
                "status" to "aberto",
                "criadoEm" to Timestamp.now()
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Mensagem enviada ao SAC", Toast.LENGTH_SHORT).show()
            mensagem.text.clear()
        }.addOnFailureListener {
            Toast.makeText(this, "Nao foi possivel enviar agora", Toast.LENGTH_LONG).show()
        }
    }

    private fun enviarMensagemIa() {
        val perguntaOriginal = inputChatIa.text.toString().trim()
        if (perguntaOriginal.isBlank()) return

        adicionarMensagemUsuario(perguntaOriginal)
        inputChatIa.text.clear()
        adicionarMensagemIa(responderIa(perguntaOriginal))
    }

    private fun responderIa(perguntaOriginal: String): String {
        val pergunta = perguntaOriginal.lowercase()
        return when {
            pergunta.contains("pagamento") || pergunta.contains("pix") || pergunta.contains("cartao") ->
                "Pagamentos ficam em Perfil > Pagamentos. Se um PIX ou cartao estiver pendente, confira o status e tente gerar novamente pelo pedido."
            pergunta.contains("endereco") ->
                "O endereco do idoso fica em Perfil > Endereco. Ele precisa ter rua, numero, bairro e cidade para liberar um atendimento mais seguro."
            pergunta.contains("pedido") || pergunta.contains("contrat") ->
                "A area Pedidos mostra contratacoes atuais, pendentes, concluidas e recibos. O cuidado atual fica em Cuidado."
            pergunta.contains("cuidador") || pergunta.contains("acompanha") || pergunta.contains("gps") ->
                "Em Cuidado voce acompanha o atendimento atual. A rota depende do cuidador compartilhar a localizacao em tempo real."
            pergunta.contains("avali") ->
                "Depois do atendimento, voce pode registrar uma avaliacao no Perfil > Avaliacoes."
            else ->
                "Posso ajudar com pagamentos, endereco, pedidos, acompanhamento, cuidador e avaliacoes. Se preferir, envie uma mensagem para o SAC retornar."
        }
    }

    private fun adicionarMensagemUsuario(texto: String) {
        chatContainerIa.addView(bolhaChat(texto, enviadaPorUsuario = true))
        rolarChatParaFim()
    }

    private fun adicionarMensagemIa(texto: String) {
        chatContainerIa.addView(bolhaChat(texto, enviadaPorUsuario = false))
        rolarChatParaFim()
    }

    private fun bolhaChat(texto: String, enviadaPorUsuario: Boolean): TextView =
        TextView(this).apply {
            text = texto
            textSize = 14.5f
            setTextColor(Color.parseColor(if (enviadaPorUsuario) "#FFFFFF" else "#0F2F3D"))
            background = rounded(if (enviadaPorUsuario) "#085EAA" else "#E7F6F7", 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.72f).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
                gravity = if (enviadaPorUsuario) {
                    android.view.Gravity.END
                } else {
                    android.view.Gravity.START
                }
            }
        }

    private fun rolarChatParaFim() {
        chatScrollIa.post {
            chatScrollIa.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun card(color: String, radius: Int): CardView =
        CardView(this).apply {
            this.radius = dp(radius).toFloat()
            cardElevation = dp(3).toFloat()
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

    private fun rounded(color: String, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radius).toFloat()
        }

    private fun stroke(color: String, stroke: String, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            setStroke(dp(1), Color.parseColor(stroke))
            cornerRadius = dp(radius).toFloat()
        }
}
