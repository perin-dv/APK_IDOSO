package com.mesawa.cuidarproximo.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R

class ContatoSOSAdapter(
    private val lista: MutableList<ContatoSOS>
) : RecyclerView.Adapter<ContatoSOSAdapter.ContatoViewHolder>() {

    class ContatoViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val txtNome =
            view.findViewById<TextView>(R.id.txtNomeContato)

        val txtParentesco =
            view.findViewById<TextView>(R.id.txtParentesco)

        val txtTelefone =
            view.findViewById<TextView>(R.id.txtTelefoneContato)

        val btnExcluir =
            view.findViewById<ImageView>(R.id.btnExcluir)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContatoViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_contato_sos,
                parent,
                false
            )

        return ContatoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ContatoViewHolder,
        position: Int
    ) {

        val item = lista[position]

        holder.txtNome.text = item.nome
        holder.txtParentesco.text = item.parentesco
        holder.txtTelefone.text = item.telefone

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