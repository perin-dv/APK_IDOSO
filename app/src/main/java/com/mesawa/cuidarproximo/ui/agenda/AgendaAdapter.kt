package com.mesawa.cuidarproximo.ui.agenda

import android.graphics.Paint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val lista: MutableList<AgendaItem>
) : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>() {

    class AgendaViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val txtHora: TextView =
            view.findViewById(R.id.txtHora)

        val txtCategoria: TextView =
            view.findViewById(R.id.txtCategoria)

        val txtTitulo: TextView =
            view.findViewById(R.id.txtTitulo)

        val txtDescricao: TextView =
            view.findViewById(R.id.txtDescricao)

        val txtPrioridade: TextView =
            view.findViewById(R.id.txtPrioridade)

        val checkConcluido: CheckBox =
            view.findViewById(R.id.checkConcluido)

        val cardAgenda =
            view.findViewById<androidx.cardview.widget.CardView>(
                R.id.cardAgenda
            )

        val txtAtrasado: TextView =
            view.findViewById(R.id.txtAtrasado)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AgendaViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agenda, parent, false)

        return AgendaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AgendaViewHolder,
        position: Int
    ) {

        val item = lista[position]

        holder.txtHora.text = item.hora
        holder.txtCategoria.text = item.categoria
        holder.txtTitulo.text = item.titulo
        holder.txtDescricao.text = item.descricao

        holder.txtPrioridade.text =
            "${item.prioridade} prioridade"

        // REMOVE LISTENER ANTIGO
        holder.checkConcluido.setOnCheckedChangeListener(null)

        holder.checkConcluido.isChecked =
            item.concluido

        // =========================
        // COR PRIORIDADE
        // =========================

        when(item.prioridade) {

            "Alta" -> {

                holder.txtPrioridade.setTextColor(
                    Color.parseColor("#DC2626")
                )
            }

            "Média" -> {

                holder.txtPrioridade.setTextColor(
                    Color.parseColor("#F59E0B")
                )
            }

            else -> {

                holder.txtPrioridade.setTextColor(
                    Color.parseColor("#16A34A")
                )
            }
        }

        // =========================
        // TEXTO RISCADO
        // =========================

        if(item.concluido) {


            holder.cardAgenda.alpha = 0.5f


            holder.txtTitulo.paintFlags =
                holder.txtTitulo.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG

            holder.txtDescricao.paintFlags =
                holder.txtDescricao.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG

        } else {
            holder.cardAgenda.alpha = 1f


            holder.txtTitulo.paintFlags =
                holder.txtTitulo.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()

            holder.txtDescricao.paintFlags =
                holder.txtDescricao.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // =========================
        // CHECKBOX FIREBASE
        // =========================

        holder.checkConcluido
            .setOnCheckedChangeListener { _, isChecked ->

                FirebaseFirestore.getInstance()
                    .collection("agenda")
                    .document(item.id)
                    .update("concluido", isChecked)
            }

        // =========================
        // BOTAO ATRASO
        // =========================

        val formato =
            SimpleDateFormat("HH:mm", Locale.getDefault())

        val horaAtual =
            formato.format(Date())

        val atrasado =
            item.hora < horaAtual &&
                    !item.concluido

        if (atrasado) {

            holder.txtAtrasado.visibility =
                View.VISIBLE

        } else {

            holder.txtAtrasado.visibility =
                View.GONE
        }

    }

    override fun getItemCount(): Int {
        return lista.size
    }
}