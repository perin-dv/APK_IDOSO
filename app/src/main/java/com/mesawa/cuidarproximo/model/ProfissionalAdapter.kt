package com.mesawa.cuidarproximo.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mesawa.cuidarproximo.R

class ProfissionalAdapter(
    private val lista: List<Profissional>,
    private val onItemClick: (Profissional) -> Unit,
) :
    RecyclerView.Adapter<ProfissionalAdapter.ProfissionalViewHolder>() {

    class ProfissionalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.txtNome)
        val especialidade: TextView = itemView.findViewById(R.id.txtEspecialidade)
        val avaliacao: TextView = itemView.findViewById(R.id.txtAvaliacao)
        val valorHora: TextView = itemView.findViewById(R.id.txtValorHora)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfissionalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profissional, parent, false)
        return ProfissionalViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ProfissionalViewHolder, position: Int) {
        val prof = lista[position]
        holder.nome.text = prof.nome
        holder.especialidade.text = prof.especialidade
        holder.avaliacao.text = "${prof.avaliacao} ⭐ (${prof.atendimentos} atendimentos)"
        holder.valorHora.text = "R$${prof.valorHora}/h"

        holder.itemView.setOnClickListener {
            onItemClick(prof) // dispara o click
        }
    }

}