package com.mesawa.cuidarproximo.ui.sos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.profile.ContatoSOS

class SOSContactsAdapter(
    private val lista: MutableList<ContatoSOS>
) : RecyclerView.Adapter<SOSContactsAdapter.ViewHolder>() {

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val txtNome =
            view.findViewById<TextView>(R.id.txtNomeContato)

        val txtTelefone =
            view.findViewById<TextView>(R.id.txtTelefoneContato)

        val txtParentesco =
            view.findViewById<TextView>(R.id.txtParentesco)

        val btnExcluir =
            view.findViewById<ImageView>(R.id.btnExcluir)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_contato_sos,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = lista[position]

        holder.txtNome.text = item.nome
        holder.txtTelefone.text = item.telefone
        holder.txtParentesco.text = item.parentesco

        holder.btnExcluir.setOnClickListener {

            FirebaseFirestore.getInstance()
                .collection("contatos_sos")
                .document(item.id)
                .delete()
        }
    }

    override fun getItemCount(): Int {
        return lista.size
    }
}