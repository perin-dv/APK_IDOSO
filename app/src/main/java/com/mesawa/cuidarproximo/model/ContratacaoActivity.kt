package com.mesawa.cuidarproximo.ui.model

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.model.Contratacao

class ContratacaoActivity : AppCompatActivity() {

    private lateinit var btnContratar: MaterialButton
    private lateinit var txtHoras: TextView
    private lateinit var txtTotal: TextView
    private lateinit var editEndereco: TextInputEditText
    private lateinit var editObs: TextInputEditText

    private var horas = 4
    private var valorHora = 0.0
    private var nomeProfissional = ""

    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contratacao)
        supportActionBar?.hide()

        btnContratar = findViewById(R.id.btnContratar)
        txtHoras = findViewById(R.id.txtHoras)
        txtTotal = findViewById(R.id.txtTotal)
        editEndereco = findViewById(R.id.editEndereco)
        editObs = findViewById(R.id.editObs)

        val txtNomeProfissional: TextView = findViewById(R.id.txtNome)
        val txtEspecialidade: TextView = findViewById(R.id.txtEspecialidade)
        val txtValorHora: TextView = findViewById(R.id.txtTotal)

        // BOTÕES + E -
        val btnMais: TextView = findViewById(R.id.btnMais)
        val btnMenos: TextView = findViewById(R.id.btnMenos)

        // DADOS RECEBIDOS
        nomeProfissional = intent.getStringExtra("nome") ?: "Cuidador"
        val especialidade = intent.getStringExtra("especialidade") ?: ""
        valorHora = intent.getDoubleExtra("valorHora", 0.0)

        // Preenche os TextViews
        txtNomeProfissional.text = nomeProfissional
        txtEspecialidade.text = especialidade
        txtValorHora.text = "R$ %.2f/h".format(valorHora)

        atualizarValores()

        btnMais.setOnClickListener {
            horas++
            atualizarValores()
        }

        btnMenos.setOnClickListener {
            if (horas > 1) {
                horas--
                atualizarValores()
            }
        }

        editEndereco.setText("Rua Exemplo, Maringá - PR")

        btnContratar.setOnClickListener {
            criarContratacao()
        }
    }

    private fun atualizarValores() {
        txtHoras.text = horas.toString()
        val total = horas * valorHora
        txtTotal.text = "%.2f".format(total)
    }

    private fun criarContratacao() {
        val endereco = editEndereco.text.toString()
        val observacao = editObs.text.toString()
        val valorTotal = horas * valorHora

        val contratacao = Contratacao(
            cuidadorId = "123",
            cuidadorNome = nomeProfissional, // usa o nome real
            familiarId = "456",
            idosoNome = "Maria Helena",
            endereco = endereco,
            observacao = observacao,
            horas = horas,
            valor = valorTotal,
            status = "aguardando_pagamento"
        )

        firestore.collection("contratacoes")
            .add(contratacao)
            .addOnSuccessListener { docRef ->
                Toast.makeText(this, "Abrindo pagamento...", Toast.LENGTH_SHORT).show()
                abrirPagamento(docRef.id, valorTotal, nomeProfissional)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao contratar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun abrirPagamento(contratacaoId: String, valor: Double, cuidadorNome: String) {
        val intent = Intent(this, PagamentoActivity::class.java)
        intent.putExtra("contratacaoId", contratacaoId)
        intent.putExtra("valor", valor)
        intent.putExtra("cuidadorNome", cuidadorNome) // envia o nome real
        startActivity(intent)
    }
}