package com.mesawa.cuidarproximo.ui.profile

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.BaseActivity
import com.mesawa.cuidarproximo.R

class EnderecoIdosoActivity : BaseActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: "sem_login"

    private lateinit var rua: EditText
    private lateinit var numero: EditText
    private lateinit var bairro: EditText
    private lateinit var cidade: EditText
    private lateinit var referencia: EditText
    private lateinit var observacoes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F8FB"))
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        root.addView(heroEndereco())

        val card = CardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        content.addView(labelText("Dados principais"))
        rua = campo("Rua ou avenida")
        bairro = campo("Bairro")
        cidade = campo("Cidade")
        numero = campo("Numero")
        referencia = campo("Ponto de referencia")
        observacoes = campo("Observacoes de acesso", linhas = 3)

        content.addView(rua)
        content.addView(linhaCampos(bairro, numero))
        content.addView(cidade)
        content.addView(labelText("Detalhes para o cuidador").apply { setPadding(0, dp(18), 0, 0) })
        content.addView(referencia)
        content.addView(observacoes)

        val salvar = Button(this).apply {
            text = "Salvar endereco"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = rounded("#085EAA", 16)
            setPadding(0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
            setOnClickListener { salvarEndereco() }
        }
        content.addView(salvar)
        card.addView(content)
        root.addView(card)

        setContentView(ScrollView(this).apply { addView(root) })
        carregarEndereco()
    }

    private fun campo(hint: String, linhas: Int = 1): EditText =
        EditText(this).apply {
            this.hint = hint
            textSize = 15f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#7B8794"))
            background = roundedStroke("#FFFFFF", "#D6E2EA", 14)
            setPadding(dp(14), 0, dp(14), 0)
            minLines = linhas
            minHeight = if (linhas > 1) dp(104) else dp(54)
            inputType = if (linhas > 1) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            setSingleLine(linhas == 1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }

    private fun heroEndereco(): CardView =
        CardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.parseColor("#E7F6F7"))
            val row = LinearLayout(this@EnderecoIdosoActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }
            row.addView(ImageView(this@EnderecoIdosoActivity).apply {
                setImageResource(R.drawable.ic_elder_address)
                layoutParams = LinearLayout.LayoutParams(dp(86), dp(86))
            })
            row.addView(LinearLayout(this@EnderecoIdosoActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@EnderecoIdosoActivity).apply {
                    text = "Endereco do idoso"
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#0F2F3D"))
                })
                addView(TextView(this@EnderecoIdosoActivity).apply {
                    text = "Local correto evita atrasos e deixa o atendimento mais seguro."
                    textSize = 14f
                    setTextColor(Color.parseColor("#40606D"))
                    setPadding(0, dp(6), 0, 0)
                })
            })
            addView(row)
        }

    private fun linhaCampos(esquerda: EditText, direita: EditText): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(esquerda.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { topMargin = dp(10); marginEnd = dp(8) }
            })
            addView(direita.apply {
                layoutParams = LinearLayout.LayoutParams(dp(116), LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(10) }
            })
        }

    private fun labelText(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#085EAA"))
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

    private fun carregarEndereco() {
        buscarClienteDocumentoId { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) return@buscarClienteDocumentoId

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("endereco")
                .document("principal")
                .get()
                .addOnSuccessListener { doc ->
                    rua.setText(doc.getString("rua").orEmpty())
                    numero.setText(doc.getString("numero").orEmpty())
                    bairro.setText(doc.getString("bairro").orEmpty())
                    cidade.setText(doc.getString("cidade").orEmpty())
                    referencia.setText(doc.getString("referencia").orEmpty())
                    observacoes.setText(doc.getString("observacoes").orEmpty())
                }
        }
    }

    private fun salvarEndereco() {
        if (
            rua.text.isNullOrBlank() ||
            numero.text.isNullOrBlank() ||
            bairro.text.isNullOrBlank() ||
            cidade.text.isNullOrBlank()
        ) {
            Toast.makeText(this, "Preencha rua, numero, bairro e cidade", Toast.LENGTH_LONG).show()
            return
        }

        val dados = hashMapOf(
            "usuarioId" to uid,
            "rua" to rua.text.toString().trim(),
            "numero" to numero.text.toString().trim(),
            "bairro" to bairro.text.toString().trim(),
            "cidade" to cidade.text.toString().trim(),
            "referencia" to referencia.text.toString().trim(),
            "observacoes" to observacoes.text.toString().trim(),
            "atualizadoEm" to com.google.firebase.Timestamp.now()
        )

        buscarClienteDocumentoId { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) {
                Toast.makeText(this, "Cliente nao encontrado", Toast.LENGTH_LONG).show()
                return@buscarClienteDocumentoId
            }

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("endereco")
                .document("principal")
                .set(dados)
                .addOnSuccessListener {
                    Toast.makeText(this, "Endereco salvo", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar endereco", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun buscarClienteDocumentoId(callback: (String) -> Unit) {
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
}
