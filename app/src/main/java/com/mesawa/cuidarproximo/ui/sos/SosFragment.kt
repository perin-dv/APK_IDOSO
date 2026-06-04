package com.mesawa.cuidarproximo.ui.sos

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.mesawa.cuidarproximo.R

class SosFragment : Fragment() {

    private lateinit var cardSOS: CardView

    private lateinit var cardContatos: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_sos,
            container,
            false
        )

        cardSOS =
            view.findViewById(R.id.cardSOSPrincipal)

        cardContatos =
            view.findViewById(R.id.cardContatosSOS)

        // SOS
        cardSOS.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("🚨 Emergência")
                .setMessage(
                    "Deseja acionar ajuda agora?"
                )

                .setPositiveButton("SIM") { _, _ ->

                    val intent = Intent(
                        Intent.ACTION_DIAL
                    )

                    intent.data =
                        Uri.parse("tel:192")

                    startActivity(intent)
                }

                .setNegativeButton("Cancelar", null)
                .show()
        }

        // CONTATOS
        cardContatos.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    SOSContactsActivity::class.java
                )
            )
        }

        return view
    }
}