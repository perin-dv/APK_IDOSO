package com.mesawa.cuidarproximo.ui.medicamentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mesawa.cuidarproximo.R

class MedicamentoAdapter(

    private val lista: List<MedicamentoItem>,

    private val onDelete: (MedicamentoItem) -> Unit,

    private val onEdit: (MedicamentoItem) -> Unit

) : RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder>() {

    class MedicamentoViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val txtNome: TextView =
            itemView.findViewById(R.id.txtNomeMedicamento)

        val txtDosagem: TextView =
            itemView.findViewById(R.id.txtDosagemMedicamento)

        val txtHorario: TextView =
            itemView.findViewById(R.id.txtHorarioMedicamento)

        val txtObs: TextView =
            itemView.findViewById(R.id.txtObsMedicamento)

        val btnEditar: ImageButton =
            itemView.findViewById(R.id.btnEditarMedicamento)

        val btnDelete: ImageButton =
            itemView.findViewById(R.id.btnDeleteMedicamento)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MedicamentoViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_medicamento,
                parent,
                false
            )

        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MedicamentoViewHolder,
        position: Int
    ) {

        val item = lista[position]

        holder.txtNome.text = item.nome

        holder.txtDosagem.text =
            "💊 ${item.dosagem}"

        holder.txtHorario.text =
            "⏰ ${item.horario}"

        holder.txtObs.text =
            item.observacao

        holder.btnDelete.setOnClickListener {

            onDelete(item)
        }

        holder.btnEditar.setOnClickListener {

            onEdit(item)
        }
    }

    override fun getItemCount(): Int {

        return lista.size
    }
}