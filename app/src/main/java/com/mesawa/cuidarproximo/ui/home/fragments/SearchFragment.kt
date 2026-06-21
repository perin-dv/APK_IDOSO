package com.mesawa.cuidarproximo.ui.home.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.databinding.FragmentSearchBinding
import com.mesawa.cuidarproximo.model.Profissional
import com.mesawa.cuidarproximo.model.ProfissionalAdapter
import com.mesawa.cuidarproximo.ui.model.ContratacaoActivity
import java.text.NumberFormat
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val todosProfissionais = mutableListOf<Profissional>()
    private lateinit var adapter: ProfissionalAdapter

    private var categoriaAtual = "todos"
    private var somenteBemAvaliados = false
    private var somenteAte100 = false
    private var ordenacaoAtual = "relevancia"
    private val moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        adapter = ProfissionalAdapter(emptyList()) { prof ->
            startActivity(
                Intent(requireContext(), ContratacaoActivity::class.java).apply {
                    putExtra("nome", prof.nome)
                    putExtra("especialidade", prof.especialidade)
                    putExtra("valorHora", prof.valorHora)
                    putExtra("cuidadorId", prof.cuidadorId)
                }
            )
        }

        binding.recyclerResultados.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResultados.adapter = adapter

        configurarBusca()
        carregarProfissionais()

        return binding.root
    }

    private fun configurarBusca() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                aplicarFiltros()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnFiltroCategoria.setOnClickListener {
            abrirFiltroCategoria()
        }

        binding.btnFiltroAvancado.setOnClickListener {
            abrirFiltrosAvancados()
        }

        binding.btnFiltroOrdenar.setOnClickListener {
            abrirOrdenacao()
        }
    }

    private fun abrirFiltroCategoria() {
        val opcoes = arrayOf("Todos", "Cuidadores", "Medicos", "Fisioterapia", "Psicologia")
        val valores = arrayOf("todos", "cuidador", "medico", "fisio", "psico")
        val marcado = valores.indexOf(categoriaAtual).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Categoria")
            .setSingleChoiceItems(opcoes, marcado) { dialog, which ->
                categoriaAtual = valores[which]
                aplicarFiltros()
                dialog.dismiss()
            }
            .show()
    }

    private fun abrirFiltrosAvancados() {
        val opcoes = arrayOf("4.5+ estrelas", "Ate R$100/h")
        val marcados = booleanArrayOf(somenteBemAvaliados, somenteAte100)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros")
            .setMultiChoiceItems(opcoes, marcados) { _, which, isChecked ->
                when (which) {
                    0 -> somenteBemAvaliados = isChecked
                    1 -> somenteAte100 = isChecked
                }
            }
            .setPositiveButton("Aplicar") { _, _ -> aplicarFiltros() }
            .setNegativeButton("Limpar") { _, _ ->
                somenteBemAvaliados = false
                somenteAte100 = false
                aplicarFiltros()
            }
            .show()
    }

    private fun abrirOrdenacao() {
        val opcoes = arrayOf("Relevancia", "Menor preco", "Mais atendimentos")
        val valores = arrayOf("relevancia", "menor_preco", "mais_atendidos")
        val marcado = valores.indexOf(ordenacaoAtual).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ordenar por")
            .setSingleChoiceItems(opcoes, marcado) { dialog, which ->
                ordenacaoAtual = valores[which]
                aplicarFiltros()
                dialog.dismiss()
            }
            .show()
    }

    private fun carregarProfissionais() {
        binding.txtResumoBusca.text = "Carregando profissionais..."

        firestore.collection("cuidadores")
            .document("profissionais")
            .get()
            .addOnSuccessListener { doc ->
                todosProfissionais.clear()

                val medicosMap = doc.get("medicos") as? Map<*, *>
                medicosMap?.forEach { (idMedico, medicoValue) ->
                    val medico = medicoValue as? Map<*, *> ?: return@forEach
                    val ativo = when (val valor = medico["ativo"]) {
                        is Boolean -> valor
                        is String -> valor.toBoolean()
                        else -> false
                    }
                    if (!ativo) return@forEach

                    todosProfissionais.add(
                        Profissional(
                            cuidadorId = (medico["uid"] as? String)
                                ?.takeIf { it.isNotBlank() }
                                ?: idMedico.toString(),
                            nome = medico["nome"] as? String ?: "",
                            especialidade = medico["especialidade"] as? String ?: "",
                            avaliacao = (medico["avaliacao"] as? String)?.toDoubleOrNull()
                                ?: (medico["avaliacao"] as? Number)?.toDouble()
                                ?: 0.0,
                            atendimentos = (medico["atendimentos"] as? String)?.toIntOrNull()
                                ?: (medico["atendimentos"] as? Number)?.toInt()
                                ?: 0,
                            valorHora = (medico["valorHora"] as? String)?.toDoubleOrNull()
                                ?: (medico["valorHora"] as? Number)?.toDouble()
                                ?: 0.0,
                            ativo = ativo
                        )
                    )
                }

                aplicarFiltros()
            }
            .addOnFailureListener {
                todosProfissionais.clear()
                aplicarFiltros()
            }
    }

    private fun aplicarFiltros() {
        val termo = binding.editSearch.text?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()

        var filtrados = todosProfissionais.filter { prof ->
            val texto = "${prof.nome} ${prof.especialidade}".lowercase(Locale.ROOT)
            val bateBusca = termo.isBlank() || termo.split(" ").all { texto.contains(it) }
            val bateCategoria = when (categoriaAtual) {
                "cuidador" -> texto.contains("cuidad")
                "medico" -> texto.contains("medic") || texto.contains("geriat")
                "fisio" -> texto.contains("fisio")
                "psico" -> texto.contains("psico")
                else -> true
            }
            bateBusca && bateCategoria
        }

        if (somenteBemAvaliados) {
            filtrados = filtrados.filter { it.avaliacao >= 4.5 }
        }

        if (somenteAte100) {
            filtrados = filtrados.filter { it.valorHora <= 100.0 && it.valorHora > 0.0 }
        }

        filtrados = when (ordenacaoAtual) {
            "menor_preco" -> filtrados.sortedBy { it.valorHora }
            "mais_atendidos" -> filtrados.sortedByDescending { it.atendimentos }
            else -> filtrados.sortedWith(compareByDescending<Profissional> { it.avaliacao }.thenBy { it.valorHora })
        }

        adapter = ProfissionalAdapter(filtrados) { prof ->
            startActivity(
                Intent(requireContext(), ContratacaoActivity::class.java).apply {
                    putExtra("nome", prof.nome)
                    putExtra("especialidade", prof.especialidade)
                    putExtra("valorHora", prof.valorHora)
                    putExtra("cuidadorId", prof.cuidadorId)
                }
            )
        }
        binding.recyclerResultados.adapter = adapter

        binding.txtResumoBusca.text =
            if (filtrados.isEmpty()) {
                "Nenhum cuidador encontrado"
            } else {
                "${filtrados.size} profissional(is) • a partir de ${menorPreco(filtrados)}"
            }

        binding.cardSemResultados.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerResultados.visibility = if (filtrados.isEmpty()) View.GONE else View.VISIBLE
        atualizarBarraFiltros()
    }

    private fun atualizarBarraFiltros() {
        val categoriaTexto = when (categoriaAtual) {
            "cuidador" -> "Cuidadores"
            "medico" -> "Medicos"
            "fisio" -> "Fisioterapia"
            "psico" -> "Psicologia"
            else -> "Categoria"
        }

        val filtros = mutableListOf<String>()
        if (somenteBemAvaliados) filtros.add("4.5+")
        if (somenteAte100) filtros.add("Ate R$100/h")

        val ordenarTexto = when (ordenacaoAtual) {
            "menor_preco" -> "Menor preco"
            "mais_atendidos" -> "Mais atendidos"
            else -> "Relevancia"
        }

        binding.btnFiltroCategoria.text = categoriaTexto
        binding.btnFiltroAvancado.text =
            if (filtros.isEmpty()) "Filtros" else "Filtros (${filtros.size})"
        binding.btnFiltroOrdenar.text = ordenarTexto

        val categoriaResumo = if (categoriaAtual == "todos") "Todos os profissionais" else categoriaTexto
        val filtrosResumo = if (filtros.isEmpty()) "" else " • ${filtros.joinToString(", ")}"
        binding.txtFiltrosAtivos.text = "$categoriaResumo • $ordenarTexto$filtrosResumo"
    }

    private fun menorPreco(lista: List<Profissional>): String {
        val menor = lista.filter { it.valorHora > 0.0 }.minOfOrNull { it.valorHora } ?: 0.0
        return moeda.format(menor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
