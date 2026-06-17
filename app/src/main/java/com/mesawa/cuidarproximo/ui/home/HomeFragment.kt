package com.mesawa.cuidarproximo.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.databinding.FragmentHomeBinding
import com.mesawa.cuidarproximo.model.Profissional
import com.mesawa.cuidarproximo.model.ProfissionalAdapter
import com.mesawa.cuidarproximo.ui.model.ContratacaoActivity
import java.util.Date

class HomeFragment : Fragment() {


    private lateinit var cardStatusRotina: CardView

    private lateinit var txtTituloStatus: TextView

    private lateinit var txtDescricaoStatus: TextView
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        cardStatusRotina =
            binding.cardStatusRotina

        txtTituloStatus =
            binding.txtTituloStatus

        txtDescricaoStatus =
            binding.txtDescricaoStatus



        atualizarStatusIA()
        carregarDadosIdosoLogado()
        atualizardados()

        return root
    }


    private fun atualizarStatusIA() {

        // futuramente vem do Firestore + IA

        val tarefasAtrasadas = 2

        when {

            tarefasAtrasadas >= 3 -> {

                cardStatusRotina.setCardBackgroundColor(
                    Color.parseColor("#FEE2E2")
                )

                txtTituloStatus.text =
                    "⚠️ Atenção necessária"

                txtDescricaoStatus.text =
                    "Muitas tarefas atrasadas hoje"

                txtTituloStatus.setTextColor(
                    Color.parseColor("#991B1B")
                )

                txtDescricaoStatus.setTextColor(
                    Color.parseColor("#991B1B")
                )
            }

            tarefasAtrasadas >= 1 -> {

                cardStatusRotina.setCardBackgroundColor(
                    Color.parseColor("#FEF3C7")
                )

                txtTituloStatus.text =
                    "⏰ Pequenos atrasos"

                txtDescricaoStatus.text =
                    "Algumas tarefas precisam de atenção"

                txtTituloStatus.setTextColor(
                    Color.parseColor("#92400E")
                )

                txtDescricaoStatus.setTextColor(
                    Color.parseColor("#92400E")
                )
            }

            else -> {

                cardStatusRotina.setCardBackgroundColor(
                    Color.parseColor("#DCFCE7")
                )

                txtTituloStatus.text =
                    "✅ Rotina saudável"

                txtDescricaoStatus.text =
                    "Tudo sob controle hoje"

                txtTituloStatus.setTextColor(
                    Color.parseColor("#166534")
                )

                txtDescricaoStatus.setTextColor(
                    Color.parseColor("#166534")
                )
            }
        }
    }

    fun atualizardados() {
        val db = FirebaseFirestore.getInstance()

        // Pega o documento "profissionais"
        db.collection("cuidadores")
            .document("profissionais")
            .get()
            .addOnSuccessListener { doc ->
                Log.d("MEDICOS", "Documento profissionais lido: ${doc.exists()}")

                val profissionais = mutableListOf<Profissional>()

                // Aqui pegamos o map que está dentro do documento
                val medicosMap = doc.get("medicos") as? Map<*, *>
                if (medicosMap != null) {
                    for ((idMedico, medicoValue) in medicosMap) {
                        val medico = medicoValue as? Map<*, *> ?: continue

                        val ativo = when(val a = medico["ativo"]) {
                            is Boolean -> a
                            is String -> a.toBoolean()
                            else -> false
                        }
                        if (!ativo) continue

                        val nome = medico["nome"] as? String ?: ""
                        val especialidade = medico["especialidade"] as? String ?: ""
                        val avaliacao = (medico["avaliacao"] as? String)?.toDoubleOrNull() ?: 0.0
                        val atendimentos = (medico["atendimentos"] as? String)?.toIntOrNull() ?: 0
                        val valorHora = (medico["valorHora"] as? String)?.toDoubleOrNull() ?: 0.0
                        val cuidadorId =
                            (medico["uid"] as? String)
                                ?.takeIf { it.isNotBlank() }
                                ?: idMedico.toString()

                        profissionais.add(
                            Profissional(
                                cuidadorId = cuidadorId,
                                nome = nome,
                                especialidade = especialidade,
                                avaliacao = avaliacao,
                                atendimentos = atendimentos,
                                valorHora = valorHora,
                                ativo = ativo
                            )
                        )

                        Log.d("MEDICOS", "Medico adicionado: $nome")
                    }
                } else {
                    Log.d("MEDICOS", "Não encontrou map de médicos dentro do documento")
                }



                // Atualiza RecyclerView
                // Atualiza RecyclerView
                val recyclerView = binding.recyclerProfissionais
                recyclerView.layoutManager = LinearLayoutManager(requireContext())

                val adapter = ProfissionalAdapter(profissionais) { prof ->

                    val intent = Intent(
                        requireContext(),
                        ContratacaoActivity::class.java
                    )

                    intent.putExtra("nome", prof.nome)
                    intent.putExtra("especialidade", prof.especialidade)
                    intent.putExtra("valorHora", prof.valorHora)
                    intent.putExtra("cuidadorId", prof.cuidadorId)

                    startActivity(intent)
                }

                recyclerView.adapter = adapter
                binding.txtSemProfissionais.visibility =
                    if (profissionais.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerProfissionais.visibility =
                    if (profissionais.isEmpty()) View.GONE else View.VISIBLE

                Log.d("MEDICOS", "Total profissionais adicionados: ${profissionais.size}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao buscar profissionais", e)
            }
    }

    fun carregarDadosIdosoLogado() {
        val db = FirebaseFirestore.getInstance()
        val uidLogado = FirebaseAuth.getInstance().currentUser?.uid

        if (uidLogado == null) {
            Log.e("Firestore", "Usuário não está logado")
            binding.txtNomeIdoso.text = "Erro: usuário não logado"
            binding.txtIdadeCidade.text = ""
            binding.txtStatusIdoso.text = ""
            binding.txtAlertas.text = ""
            return
        }

        // Procurar no Firestore pelo cuidador que tem este uid
        db.collection("clientes")
            .whereEqualTo("sistema.uid_auth", uidLogado)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0] // Deve ter apenas 1 correspondente
                    val info = doc.get("info") as? Map<*, *>
                    val sistema = doc.get("sistema") as? Map<*, *>

                    val nomeIdoso = info?.get("nome_idoso") as? String ?: "Sem nome"
                    val idade = (info?.get("idade") as? Long)?.toInt() ?: 0
                    val cidade = info?.get("cidade") as? String ?: "Cidade desconhecida"
                    val condicao = info?.get("condicao") as? String ?: "Indefinido"

                    // Atualiza UI
                    binding.txtNomeIdoso.text = nomeIdoso
                    binding.txtIdadeCidade.text = "$idade anos • $cidade"
                    binding.txtStatusIdoso.text = "🟢 $condicao"

                    // Alertas
                    val timestamp = sistema?.get("timestamp") as? Long ?: 0L
                    binding.txtAlertas.text = if (timestamp > 0) "Última atualização: ${Date(timestamp)}" else "Nenhum alerta"
                } else {
                    // Não encontrou nenhum documento para o uid logado
                    Log.e("Firestore", "Nenhum documento encontrado para o uid logado")
                    binding.txtNomeIdoso.text = "Erro: idoso não encontrado"
                    binding.txtIdadeCidade.text = ""
                    binding.txtStatusIdoso.text = ""
                    binding.txtAlertas.text = ""
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao carregar dados do idoso logado", e)
                binding.txtNomeIdoso.text = "Erro ao carregar dados"
                binding.txtIdadeCidade.text = ""
                binding.txtStatusIdoso.text = ""
                binding.txtAlertas.text = ""
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
