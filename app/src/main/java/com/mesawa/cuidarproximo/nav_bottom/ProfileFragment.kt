package com.mesawa.cuidarproximo.ui.home.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.home.LoginActivity
import com.mesawa.cuidarproximo.ui.medicamentos.MedicamentosActivity
import com.mesawa.cuidarproximo.ui.sos.SOSContactsActivity

class ProfileFragment : Fragment() {

    private lateinit var cardMedicamentos: CardView
    private lateinit var cardSOS: CardView
    private lateinit var cardPagamento: CardView
    private lateinit var cardIA: CardView

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

        // CARDS

        cardMedicamentos =
            view.findViewById(R.id.cardMedicamentos)

        cardMedicamentos.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    MedicamentosActivity::class.java
                )
            )
        }

        cardSOS =
            view.findViewById(R.id.cardSOS)

        cardPagamento =
            view.findViewById(R.id.cardPagamento)

        cardIA =
            view.findViewById(R.id.cardIA)

        cardConfig =
            view.findViewById(R.id.cardConfig)

        cardPremium =
            view.findViewById(R.id.cardPremium)

        cardSair =
            view.findViewById(R.id.cardSair)

        cardSOS.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    SOSContactsActivity::class.java
                )
            )
        }

        // CLIQUES

        cardMedicamentos.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tela medicamentos em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardSOS.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tela SOS em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardPagamento.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tela pagamentos em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardIA.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tela IA em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardConfig.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tela configurações em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardPremium.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Área premium em breve",
                Toast.LENGTH_SHORT
            ).show()
        }

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
}
