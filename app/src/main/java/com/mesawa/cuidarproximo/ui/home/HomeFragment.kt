package com.mesawa.cuidarproximo.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.databinding.FragmentHomeBinding
import com.mesawa.cuidarproximo.model.Profissional
import com.mesawa.cuidarproximo.model.ProfissionalAdapter
import com.mesawa.cuidarproximo.ui.model.ContratacaoActivity
import com.mesawa.cuidarproximo.ui.profile.EnderecoIdosoActivity

class HomeFragment : Fragment() {

    private lateinit var layoutBannersSaude: LinearLayout
    private lateinit var txtResumoBannersSaude: TextView
    private lateinit var scrollBannersSaude: HorizontalScrollView
    private val carouselHandler = Handler(Looper.getMainLooper())
    private var carouselIndex = 0
    private var _binding: FragmentHomeBinding? = null
    private var avisoEnderecoDispensado = false

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

        layoutBannersSaude = binding.layoutBannersSaude
        txtResumoBannersSaude = binding.txtResumoBannersSaude
        scrollBannersSaude = binding.scrollBannersSaude
        montarBannersSaude(
            nome = "idoso",
            condicao = "não informada",
            dependencia = "não informada"
        )
        carregarDadosIdosoLogado()
        atualizardados()

        binding.cardAvisoEnderecoHome.setOnClickListener {
            startActivity(Intent(requireContext(), EnderecoIdosoActivity::class.java))
        }
        binding.btnFecharAvisoEndereco.setOnClickListener {
            avisoEnderecoDispensado = true
            binding.cardAvisoEnderecoHome.visibility = View.GONE
            ajustarEspacoAvisoEndereco(false)
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            verificarEnderecoCadastrado()
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
            montarBannersSaude("idoso", "não informada", "não informada")
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
                    val dependencia = info?.get("dependencia") as? String ?: "Não informada"
                    // Atualiza UI
                    binding.txtNomeIdoso.text = nomeIdoso
                    binding.txtIdadeCidade.text = "$idade anos • $cidade"
                    binding.txtStatusIdoso.text = "🟢 $condicao"
                    montarBannersSaude(
                        nome = nomeIdoso,
                        condicao = condicao,
                        dependencia = dependencia
                    )

                } else {
                    // Não encontrou nenhum documento para o uid logado
                    Log.e("Firestore", "Nenhum documento encontrado para o uid logado")
                    binding.txtNomeIdoso.text = "Erro: idoso não encontrado"
                    binding.txtIdadeCidade.text = ""
                    binding.txtStatusIdoso.text = ""
                    montarBannersSaude("idoso", "não informada", "não informada")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao carregar dados do idoso logado", e)
                binding.txtNomeIdoso.text = "Erro ao carregar dados"
                binding.txtIdadeCidade.text = ""
                binding.txtStatusIdoso.text = ""
                montarBannersSaude("idoso", "não informada", "não informada")
            }
    }

    private fun montarBannersSaude(
        nome: String,
        condicao: String,
        dependencia: String
    ) {
        val banners = bannersPorCondicao(nome, condicao, dependencia)
        txtResumoBannersSaude.text =
            if (condicao.equals("não informada", ignoreCase = true)) {
                "Cadastre a condição do idoso para deixar estes conteúdos ainda mais certeiros."
            } else {
                "Selecionamos orientações para $nome com foco em $condicao."
            }

        layoutBannersSaude.removeAllViews()
        banners.forEachIndexed { index, banner ->
            layoutBannersSaude.addView(criarBannerSaude(banner, index))
        }
        iniciarCarrosselBanners(banners.size)
    }

    private fun iniciarCarrosselBanners(total: Int) {
        carouselHandler.removeCallbacksAndMessages(null)
        if (total <= 1) return

        carouselIndex = 0
        val passo = dp(306)
        val runnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                carouselIndex = (carouselIndex + 1) % total
                scrollBannersSaude.smoothScrollTo(carouselIndex * passo, 0)
                carouselHandler.postDelayed(this, 4200)
            }
        }
        carouselHandler.postDelayed(runnable, 4200)
    }

    private fun bannersPorCondicao(
        nome: String,
        condicao: String,
        dependencia: String
    ): List<BannerSaude> {
        val c = condicao.lowercase()
        val principal = when {
            c.contains("diabetes") -> BannerSaude(
                "Diabetes",
                "Materia oficial da Organizacao Mundial da Saude.",
                "OMS / WHO",
                "https://www.who.int/news-room/fact-sheets/detail/diabetes",
                "#0F766E",
                "#99F6E4"
            )
            c.contains("hipert") -> BannerSaude(
                "Hipertensao",
                "Materia oficial da Organizacao Mundial da Saude.",
                "OMS / WHO",
                "https://www.who.int/news-room/fact-sheets/detail/hypertension",
                "#1D4ED8",
                "#BFDBFE"
            )
            c.contains("alzheimer") -> BannerSaude(
                "Alzheimer em casa",
                "Seguranca e cuidados, fonte oficial NIH/NIA.",
                "NIH / NIA",
                "https://www.nia.nih.gov/health/alzheimers-caregiving/home-safety-and-alzheimers-disease",
                "#7C3AED",
                "#DDD6FE"
            )
            c.contains("parkinson") -> BannerSaude(
                "Risco de quedas",
                "Prevencao de quedas em idosos, fonte oficial CDC.",
                "CDC",
                "https://www.cdc.gov/falls/about/index.html",
                "#B45309",
                "#FDE68A"
            )
            c.contains("mobilidade") || c.contains("acamado") -> BannerSaude(
                "Prevencao de quedas",
                "Informacao oficial para seguranca de idosos.",
                "CDC",
                "https://www.cdc.gov/falls/about/index.html",
                "#BE123C",
                "#FFE4E6"
            )
            else -> BannerSaude(
                "Envelhecimento e saude",
                "Materia oficial da Organizacao Mundial da Saude.",
                "OMS / WHO",
                "https://www.who.int/news-room/fact-sheets/detail/ageing-and-health",
                "#0F766E",
                "#CCFBF1"
            )
        }

        val autonomia = if (dependencia.lowercase().contains("total")) {
            BannerSaude(
                "Seguranca em casa",
                "Orientacao oficial para adaptar o ambiente.",
                "NIH / NIA",
                "https://www.nia.nih.gov/health/safety/home-safety-checklist-older-adults",
                "#9F1239",
                "#FFE4E6"
            )
        } else if (dependencia.lowercase().contains("parcial")) {
            BannerSaude(
                "Seguranca em casa",
                "Checklist oficial para idosos e familiares.",
                "NIH / NIA",
                "https://www.nia.nih.gov/health/safety/home-safety-checklist-older-adults",
                "#0369A1",
                "#E0F2FE"
            )
        } else {
            BannerSaude(
                "Vida ativa",
                "Envelhecimento saudavel com fonte oficial.",
                "OMS / WHO",
                "https://www.who.int/news-room/fact-sheets/detail/ageing-and-health",
                "#15803D",
                "#DCFCE7"
            )
        }

        return listOf(
            principal,
            autonomia,
            BannerSaude(
                "Evitar quedas",
                "Dados e orientacoes oficiais sobre quedas em idosos.",
                "CDC",
                "https://www.cdc.gov/falls/about/index.html",
                "#4338CA",
                "#E0E7FF"
            )
        )
    }

    private fun criarBannerSaude(banner: BannerSaude, index: Int): CardView {
        val context = requireContext()
        val card = CardView(context).apply {
            radius = dp(26).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.parseColor(banner.lightColor))
            layoutParams = LinearLayout.LayoutParams(dp(292), dp(198)).apply {
                marginEnd = dp(14)
            }
            isClickable = true
            isFocusable = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor(banner.lightColor), Color.WHITE)
            ).apply {
                cornerRadius = dp(26).toFloat()
            }
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_elder_address)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        }

        val tag = TextView(context).apply {
            text = "Fonte: ${banner.sourceName}"
            setTextColor(Color.parseColor(banner.darkColor))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                marginStart = dp(12)
            }
        }

        topRow.addView(icon)
        topRow.addView(tag)

        val title = TextView(context).apply {
            text = banner.title
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }

        val subtitle = TextView(context).apply {
            text = banner.subtitle
            setTextColor(Color.parseColor("#475569"))
            textSize = 14.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(8)
            }
        }

        val page = TextView(context).apply {
            text = "${index + 1}/3"
            setTextColor(Color.parseColor(banner.darkColor))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.END
        }

        content.addView(topRow)
        content.addView(title)
        content.addView(subtitle)
        content.addView(page)
        card.addView(content)

        card.setOnClickListener {
            abrirMateriaOficial(banner.sourceUrl)
        }

        return card
    }

    private fun abrirMateriaOficial(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(requireContext(), "Nao foi possivel abrir a materia oficial", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private data class BannerSaude(
        val title: String,
        val subtitle: String,
        val sourceName: String,
        val sourceUrl: String,
        val darkColor: String,
        val lightColor: String
    )

    private fun verificarEnderecoCadastrado() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            binding.cardAvisoEnderecoHome.visibility = View.GONE
            ajustarEspacoAvisoEndereco(false)
            return
        }

        if (avisoEnderecoDispensado) {
            binding.cardAvisoEnderecoHome.visibility = View.GONE
            ajustarEspacoAvisoEndereco(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("enderecos_idosos")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->
                val enderecoIncompleto =
                    !documento.exists() ||
                        documento.getString("rua").isNullOrBlank() ||
                        documento.getString("numero").isNullOrBlank() ||
                        documento.getString("bairro").isNullOrBlank() ||
                        documento.getString("cidade").isNullOrBlank()

                binding.cardAvisoEnderecoHome.visibility =
                    if (enderecoIncompleto) View.VISIBLE else View.GONE
                ajustarEspacoAvisoEndereco(enderecoIncompleto)
            }
            .addOnFailureListener {
                binding.cardAvisoEnderecoHome.visibility = View.VISIBLE
                ajustarEspacoAvisoEndereco(true)
            }
    }

    private fun ajustarEspacoAvisoEndereco(visivel: Boolean) {
        val top = if (visivel) dp(124) else 0
        binding.scrollHome.setPadding(
            binding.scrollHome.paddingLeft,
            top,
            binding.scrollHome.paddingRight,
            binding.scrollHome.paddingBottom
        )
        binding.scrollHome.clipToPadding = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carouselHandler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
