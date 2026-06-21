package com.mesawa.cuidarproximo.ui.model

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.mesawa.cuidarproximo.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.pagamento.PagamentoActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ContratacaoActivity : BaseActivity() {

    private lateinit var btnContratar: MaterialButton
    private lateinit var txtHoras: TextView
    private lateinit var txtTotal: TextView
    private lateinit var txtValorHora: TextView
    private lateinit var editEndereco: TextInputEditText
    private lateinit var editObs: TextInputEditText

    private var horas = 4
    private var valorHora = 0.0
    private var nomeProfissional = ""
    private var cuidadorId = ""

    private var salvando = false

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contratacao)

        supportActionBar?.hide()

        btnContratar = findViewById(R.id.btnContratar)
        txtHoras = findViewById(R.id.txtHoras)
        txtTotal = findViewById(R.id.txtTotal)

        // Atenção: esse ID precisa existir no XML.
        txtValorHora = findViewById(R.id.txtValorHora)

        editEndereco = findViewById(R.id.editEndereco)
        editObs = findViewById(R.id.editObs)

        val txtNomeProfissional: TextView = findViewById(R.id.txtNome)
        val txtEspecialidade: TextView = findViewById(R.id.txtEspecialidade)

        val btnMais: MaterialButton = findViewById(R.id.btnMais)
        val btnMenos: MaterialButton = findViewById(R.id.btnMenos)

        nomeProfissional = intent.getStringExtra("nome") ?: "Cuidador"
        valorHora = intent.getDoubleExtra("valorHora", 0.0)
        cuidadorId = intent.getStringExtra("cuidadorId") ?: ""
        val especialidade = intent.getStringExtra("especialidade") ?: ""

        txtNomeProfissional.text = nomeProfissional
        txtEspecialidade.text = especialidade
        txtValorHora.text = "R$ %.2f/h".format(valorHora)

        atualizarValores()
        carregarEnderecoCadastrado()

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

        btnContratar.setOnClickListener {
            criarContratacao()
        }
    }

    private fun atualizarValores() {
        txtHoras.text = horas.toString()
        val total = horas * valorHora
        txtTotal.text = "R$ %.2f".format(total)
    }

    private fun carregarEnderecoCadastrado() {
        val userId = auth.currentUser?.uid ?: return

        buscarClienteDocumentoId(userId) { clienteDocumentoId ->
            if (clienteDocumentoId.isBlank()) return@buscarClienteDocumentoId

            firestore.collection("clientes")
                .document(clienteDocumentoId)
                .collection("endereco")
                .document("principal")
                .get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) return@addOnSuccessListener

                    val rua = doc.getString("rua").orEmpty().trim()
                    val numero = doc.getString("numero").orEmpty().trim()
                    val bairro = doc.getString("bairro").orEmpty().trim()
                    val cidade = doc.getString("cidade").orEmpty().trim()
                    val referencia = doc.getString("referencia").orEmpty().trim()
                    val observacoes = doc.getString("observacoes").orEmpty().trim()

                    val endereco = listOfNotNull(
                        rua.takeIf { it.isNotBlank() }?.let {
                            if (numero.isNotBlank()) "$it, $numero" else it
                        },
                        bairro.takeIf { it.isNotBlank() },
                        cidade.takeIf { it.isNotBlank() }
                    ).joinToString(" - ")

                    if (endereco.isNotBlank()) {
                        editEndereco.setText(endereco)
                    }

                    val detalhes = listOfNotNull(
                        referencia.takeIf { it.isNotBlank() }?.let { "Referencia: $it" },
                        observacoes.takeIf { it.isNotBlank() }
                    ).joinToString("\n")

                    if (detalhes.isNotBlank() && editObs.text.isNullOrBlank()) {
                        editObs.setText(detalhes)
                    }
                }
        }
    }

    private fun criarContratacao() {
        if (salvando) return

        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }

        if (cuidadorId.isBlank()) {
            Toast.makeText(this, "Cuidador inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val endereco = editEndereco.text?.toString()?.trim().orEmpty()
        val observacao = editObs.text?.toString()?.trim().orEmpty()

        if (endereco.isBlank()) {
            Toast.makeText(this, "Informe o endereço", Toast.LENGTH_SHORT).show()
            return
        }

        salvando = true
        btnContratar.isEnabled = false
        btnContratar.text = "Aguarde..."

        firestore.collection("clientes")
            .whereEqualTo("sistema.uid_auth", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                val clienteDoc = query.documents.firstOrNull()
                val info = clienteDoc?.get("info") as? Map<*, *>
                val responsavel = clienteDoc?.get("responsavel") as? Map<*, *>

                salvarContratacao(
                    userId = userId,
                    clienteDocumentoId = clienteDoc?.id.orEmpty(),
                    clienteNome = responsavel?.get("nome_responsavel") as? String ?: "",
                    clienteEmail = responsavel?.get("email") as? String ?: "",
                    idosoNome = info?.get("nome_idoso") as? String ?: "",
                    endereco = endereco,
                    observacao = observacao
                )
            }
            .addOnFailureListener {
                salvarContratacao(
                    userId = userId,
                    clienteDocumentoId = "",
                    clienteNome = "",
                    clienteEmail = auth.currentUser?.email.orEmpty(),
                    idosoNome = "",
                    endereco = endereco,
                    observacao = observacao
                )
            }
    }

    private fun salvarContratacao(
        userId: String,
        clienteDocumentoId: String,
        clienteNome: String,
        clienteEmail: String,
        idosoNome: String,
        endereco: String,
        observacao: String
    ) {
        val valorTotal = horas * valorHora
        val taxaPlataforma = 0.09
        val valorComissao = valorTotal * taxaPlataforma
        val valorLiquidoCuidador = valorTotal - valorComissao

        val contratacaoId = gerarContratacaoId(userId, endereco, idosoNome)
        val clienteContratacaoId = clienteDocumentoId.ifBlank { userId }
        val docRef = firestore.collection("clientes")
            .document(clienteContratacaoId)
            .collection("contratacoes")
            .document(contratacaoId)

        val dados = hashMapOf<String, Any?>(
            "id" to contratacaoId,
            "codigoContratacao" to contratacaoId,
            "contratacaoOwnerId" to clienteContratacaoId,
            "clienteDocId" to clienteContratacaoId,
            "clienteId" to userId,
            "clienteDocumentoId" to clienteDocumentoId,
            "clienteNome" to clienteNome,
            "clienteEmail" to clienteEmail,
            "cuidadorId" to cuidadorId,
            "cuidadorNome" to nomeProfissional,
            "idosoNome" to idosoNome,
            "endereco" to endereco,
            "observacao" to observacao,
            "horas" to horas,
            "valorTotal" to valorTotal,
            "taxaPlataforma" to taxaPlataforma,
            "valorComissao" to valorComissao,
            "valorLiquidoCuidador" to valorLiquidoCuidador,
            "status" to "aguardando_pagamento",
            "pagamentoStatus" to "pending",
            "metodoPagamento" to "pix",
            "paymentId" to null,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.get()
            .addOnSuccessListener {
                val docParaSalvar =
                    if (it.exists() && it.getString("pagamentoStatus") == "approved") {
                        firestore.collection("clientes")
                            .document(clienteContratacaoId)
                            .collection("contratacoes")
                            .document("${contratacaoId}-${gerarSufixoHorario()}")
                    } else {
                        docRef
                    }

                val idFinal = docParaSalvar.id
                dados["id"] = idFinal
                dados["codigoContratacao"] = idFinal

                if (!it.exists() || docParaSalvar.id != docRef.id) {
                    dados["createdAt"] = FieldValue.serverTimestamp()
                }

                docParaSalvar.set(dados, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Redirecionando para pagamento...",
                            Toast.LENGTH_SHORT
                        ).show()

                        abrirPagamento(
                            contratacaoId = idFinal,
                            contratacaoOwnerId = clienteContratacaoId,
                            valor = valorTotal,
                            cuidadorNome = nomeProfissional
                        )
                    }
                    .addOnFailureListener {
                        tratarErroSalvarContratacao()
                    }
            }
            .addOnFailureListener {
                tratarErroSalvarContratacao()
            }
    }

    private fun tratarErroSalvarContratacao() {
        salvando = false
        btnContratar.isEnabled = true
        btnContratar.text = "Contratar"

        Toast.makeText(this, "Erro ao criar contratação", Toast.LENGTH_SHORT).show()
    }

    private fun gerarContratacaoId(userId: String, endereco: String, idosoNome: String): String {
        val data = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val sufixo = userId.takeLast(6).uppercase(Locale.US)
        val chave = "$userId|$cuidadorId|$endereco|$idosoNome|$horas|$valorHora"
        val resumo = abs(chave.hashCode()).toString(36).uppercase(Locale.US)
        return "CTR-$data-$sufixo-$resumo"
    }

    private fun gerarSufixoHorario(): String {
        return SimpleDateFormat("HHmmss", Locale.US).format(Date())
    }

    private fun abrirPagamento(
        contratacaoId: String,
        contratacaoOwnerId: String,
        valor: Double,
        cuidadorNome: String
    ) {
        val intent = Intent(this, PagamentoActivity::class.java).apply {
            putExtra("contratacaoId", contratacaoId)
            putExtra("contratacaoOwnerId", contratacaoOwnerId)
            putExtra("valor", valor)
            putExtra("cuidadorNome", cuidadorNome)
            putExtra("horas", horas)
        }

        startActivity(intent)
        finish()
    }

    private fun buscarClienteDocumentoId(userId: String, callback: (String) -> Unit) {
        firestore.collection("clientes")
            .whereEqualTo("sistema.uid_auth", userId)
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
