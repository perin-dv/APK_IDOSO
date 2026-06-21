package com.mesawa.cuidarproximo.ui.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.BaseActivity

class AvaliacoesActivity : BaseActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: "sem_login"
    private lateinit var lista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = screenRoot(this)
        root.addView(title("Avaliacoes"))
        root.addView(subtitle("Registre sua experiencia com cuidadores e atendimentos."))

        val formCard = sectionCard(this)
        val form = cardContent(this)
        val rating = RatingBar(this).apply {
            numStars = 5
            stepSize = 1f
            rating = 5f
        }
        val comentario = EditText(this).apply {
            hint = "Escreva sua avaliacao"
            minLines = 3
        }
        val salvar = Button(this).apply {
            text = "Enviar avaliacao"
            setOnClickListener {
                salvarAvaliacao(rating.rating, comentario.text.toString())
                comentario.text.clear()
            }
        }
        form.addView(rating)
        form.addView(comentario)
        form.addView(salvar)
        formCard.addView(form)
        root.addView(formCard)

        root.addView(value("Historico", size = 20f).apply { setPadding(0, dp(22), 0, 0) })
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(lista)

        setContentView(ScrollView(this).apply { addView(root) })
        carregarAvaliacoes()
    }

    private fun salvarAvaliacao(nota: Float, comentario: String) {
        firestore.collection("avaliacoes").add(
            hashMapOf(
                "usuarioId" to uid,
                "nota" to nota.toInt(),
                "comentario" to comentario.trim(),
                "criadoEm" to Timestamp.now()
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Avaliacao enviada", Toast.LENGTH_SHORT).show()
            carregarAvaliacoes()
        }
    }

    private fun carregarAvaliacoes() {
        firestore.collection("avaliacoes").whereEqualTo("usuarioId", uid).get()
            .addOnSuccessListener { result ->
                lista.removeAllViews()
                val itens = result.documents.sortedByDescending { it.getTimestamp("criadoEm")?.toDate()?.time ?: 0L }
                if (itens.isEmpty()) {
                    lista.addView(emptyCard("Nenhuma avaliacao ainda", "Quando voce avaliar um atendimento, ele aparece aqui."))
                    return@addOnSuccessListener
                }
                itens.forEach { doc ->
                    val card = sectionCard(this)
                    val content = cardContent(this)
                    content.addView(value("Nota ${doc.getLong("nota") ?: 0}/5", "#085EAA", 18f))
                    content.addView(value(doc.getString("comentario").orEmpty().ifBlank { "Sem comentario" }))
                    card.addView(content)
                    lista.addView(card)
                }
            }
    }

    private fun emptyCard(titulo: String, texto: String) =
        sectionCard(this, "#EEF2FF").apply {
            val content = cardContent(this@AvaliacoesActivity)
            content.addView(value(titulo, "#312E81", 18f))
            content.addView(value(texto, "#4B5563", 14f))
            addView(content)
        }
}
