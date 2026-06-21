package com.mesawa.cuidarproximo.ui.home.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.home.LoginActivity
import com.mesawa.cuidarproximo.ui.pagamento.HistoricoPagamentosActivity
import com.mesawa.cuidarproximo.ui.profile.AvaliacoesActivity
import com.mesawa.cuidarproximo.ui.profile.ConfiguracoesActivity
import com.mesawa.cuidarproximo.ui.profile.EnderecoIdosoActivity
import com.mesawa.cuidarproximo.ui.profile.FaleConoscoActivity
import com.mesawa.cuidarproximo.ui.profile.PremiumActivity

class ProfileFragment : Fragment() {

    private lateinit var cardEndereco: CardView
    private lateinit var cardPagamento: CardView
    private lateinit var cardAvaliacoes: CardView
    private lateinit var cardFaleConosco: CardView
    private lateinit var cardConfig: CardView
    private lateinit var cardPremium: CardView
    private lateinit var cardSair: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_profile,
            container,
            false
        )

        cardEndereco = view.findViewById(R.id.cardEndereco)
        cardPagamento = view.findViewById(R.id.cardPagamento)
        cardAvaliacoes = view.findViewById(R.id.cardAvaliacoes)
        cardFaleConosco = view.findViewById(R.id.cardFaleConosco)
        cardConfig = view.findViewById(R.id.cardConfig)
        cardPremium = view.findViewById(R.id.cardPremium)
        cardSair = view.findViewById(R.id.cardSair)

        cardEndereco.setOnClickListener { abrir(EnderecoIdosoActivity::class.java) }
        cardPagamento.setOnClickListener { abrir(HistoricoPagamentosActivity::class.java) }
        cardAvaliacoes.setOnClickListener { abrir(AvaliacoesActivity::class.java) }
        cardFaleConosco.setOnClickListener { abrir(FaleConoscoActivity::class.java) }
        cardConfig.setOnClickListener { abrir(ConfiguracoesActivity::class.java) }
        cardPremium.setOnClickListener { abrir(PremiumActivity::class.java) }

        // LOGOUT

        cardSair.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            startActivity(
                Intent(
                    requireContext(),
                    LoginActivity::class.java
                )
            )

            requireActivity().finish()
        }

        return view
    }

    private fun abrir(activity: Class<*>) {
        startActivity(Intent(requireContext(), activity))
    }
}
