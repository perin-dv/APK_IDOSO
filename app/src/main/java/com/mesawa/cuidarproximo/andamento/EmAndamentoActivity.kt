package com.mesawa.cuidarproximo.ui.andamento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mesawa.cuidarproximo.R

class EmAndamentoActivity : AppCompatActivity() {

    private lateinit var txtNomeCuidador: TextView
    private lateinit var txtStatusAtendimento: TextView
    private lateinit var txtETA: TextView

    private lateinit var btnSOS: MaterialButton
    private lateinit var cardLigar: MaterialButton
    private lateinit var cardChat: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private var contratacaoId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_em_andamento)

        txtNomeCuidador = findViewById(R.id.txtNomeCuidador)
        txtStatusAtendimento = findViewById(R.id.txtStatusAtendimento)
        txtETA = findViewById(R.id.txtETA)

        btnSOS = findViewById(R.id.btnSOS)
        cardLigar = findViewById(R.id.cardLigar)
        cardChat = findViewById(R.id.cardChat)

        contratacaoId = intent.getStringExtra("contratacaoId").orEmpty()

        btnSOS.setOnClickListener {
            // Abrir número de emergência
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:192")
            startActivity(intent)
        }

        cardLigar.setOnClickListener {
            // Ligar pro cuidador
            val numero = "+5544XXXXXXX" // puxar do Firebase
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$numero")
            startActivity(intent)
        }

        cardChat.setOnClickListener {
            // Abrir WhatsApp
            val numero = "+5544XXXXXXX"
            val mensagem = "Olá, estou acompanhando a ida do cuidador."
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/$numero?text=$mensagem")
            startActivity(intent)
        }

        acompanharStatusRealtime()
    }

    private fun acompanharStatusRealtime() {
        if (contratacaoId.isBlank()) {
            Toast.makeText(this, "Contratação inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        listener = firestore.collection("contratacoes")
            .document(contratacaoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val nomeCuidador = snapshot.getString("cuidadorNome") ?: "Cuidador"
                val status = snapshot.getString("status") ?: "A caminho"
                val eta = snapshot.getString("eta") ?: "12 min"

                txtNomeCuidador.text = nomeCuidador
                txtStatusAtendimento.text = status
                txtETA.text = eta
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
