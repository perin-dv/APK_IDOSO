package com.mesawa.cuidarproximo.ui.medicamentos

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import java.util.Calendar
import com.mesawa.cuidarproximo.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.notificacao.MedicamentoReceiver
import com.mesawa.cuidarproximo.ui.profile.Medicamento

class MedicamentosActivity : BaseActivity() {

    private lateinit var recyclerMedicamentos: RecyclerView

    private lateinit var btnAdicionar: FloatingActionButton

    private lateinit var adapter: MedicamentoAdapter

    private val listaMedicamentos =
        mutableListOf<MedicamentoItem>()

    private val firestore =
        FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_medicamentos
        )

        recyclerMedicamentos =
            findViewById(R.id.recyclerMedicamentos)

        btnAdicionar =
            findViewById(R.id.btnAddMedicamento)

        recyclerMedicamentos.layoutManager =
            LinearLayoutManager(this)

        adapter =
            MedicamentoAdapter(

                listaMedicamentos,

                onDelete = { medicamento ->

                    deletarMedicamento(medicamento)
                },

                onEdit = { medicamento ->

                    editarMedicamento(medicamento)
                }
            )

        recyclerMedicamentos.adapter =
            adapter

        carregarMedicamentos()

        btnAdicionar.setOnClickListener {

            abrirDialogAdicionar()
        }
    }

    // =========================
    // DIALOG
    // =========================

    private fun abrirDialogAdicionar() {

        val dialogView = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_add_medicamento,
                null
            )

        val editNome =
            dialogView.findViewById<EditText>(
                R.id.editNomeMedicamento
            )

        val editDosagem =
            dialogView.findViewById<EditText>(
                R.id.editDosagemMedicamento
            )

        val editHorario =
            dialogView.findViewById<EditText>(
                R.id.editHorarioMedicamento
            )

        val editObs =
            dialogView.findViewById<EditText>(
                R.id.editObsMedicamento
            )

        AlertDialog.Builder(this)
            .setTitle("Novo medicamento")
            .setView(dialogView)

            .setPositiveButton("Salvar") { _, _ ->

                val nome =
                    editNome.text.toString()

                val dosagem =
                    editDosagem.text.toString()

                val horario =
                    editHorario.text.toString()

                val obs =
                    editObs.text.toString()

                if (nome.isNotEmpty()) {

                    salvarMedicamento(
                        nome,
                        dosagem,
                        horario,
                        obs
                    )
                }
            }

            .setNegativeButton(
                "Cancelar",
                null
            )

            .show()
    }

    // =========================
    // FIREBASE
    // =========================

    private fun salvarMedicamento(
        nome: String,
        dosagem: String,
        horario: String,
        observacao: String
    ) {

        val medicamento = hashMapOf(

            "nome" to nome,

            "dosagem" to dosagem,

            "horario" to horario,

            "observacao" to observacao
        )

        firestore
            .collection("medicamentos")

            .add(medicamento)

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Medicamento salvo",
                    Toast.LENGTH_SHORT
                ).show()

                val item = MedicamentoItem(

                    nome = nome,

                    dosagem = dosagem,

                    horario = horario,

                    observacao = observacao
                )

                agendarMedicamento(item)
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Erro ao salvar",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================
    // CARREGAR
    // =========================

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarMedicamentos() {

        firestore
            .collection("medicamentos")

            .addSnapshotListener { value, error ->

                if (error != null) {

                    Toast.makeText(
                        this,
                        "Erro ao carregar",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addSnapshotListener
                }

                listaMedicamentos.clear()

                value?.documents?.forEach { document ->

                    val item =
                        document.toObject(
                            MedicamentoItem::class.java
                        )

                    item?.id = document.id

                    if (item != null) {

                        listaMedicamentos.add(item)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun deletarMedicamento(
        medicamento: MedicamentoItem
    ) {

        firestore
            .collection("medicamentos")
            .document(medicamento.id)

            .delete()

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Removido",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun agendarMedicamento(
        medicamento: MedicamentoItem
    ){
        val partes =
            medicamento.horario.split(":")

        val hora =
            partes[0].toInt()

        val minuto =
            partes[1].toInt()


        val intent = Intent(
            this,
            MedicamentoReceiver::class.java
        )

        intent.putExtra(
            "titulo",
            medicamento.nome
        )

        val pendingIntent =
            PendingIntent.getBroadcast(

                this,

                medicamento.nome.hashCode(),

                intent,

                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmManager =
            getSystemService(
                ALARM_SERVICE
            ) as AlarmManager

        val calendar = Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            hora
        )

        calendar.set(
            Calendar.MINUTE,
            minuto
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        alarmManager.setExactAndAllowWhileIdle(

            AlarmManager.RTC_WAKEUP,

            calendar.timeInMillis,

            pendingIntent
        )
    }



    private fun editarMedicamento(
        medicamento: MedicamentoItem
    ) {

        val dialogView = LayoutInflater.from(this)
            .inflate(
                R.layout.dialog_add_medicamento,
                null
            )

        val editNome =
            dialogView.findViewById<EditText>(
                R.id.editNomeMedicamento
            )

        val editDosagem =
            dialogView.findViewById<EditText>(
                R.id.editDosagemMedicamento
            )

        val editHorario =
            dialogView.findViewById<EditText>(
                R.id.editHorarioMedicamento
            )

        val editObs =
            dialogView.findViewById<EditText>(
                R.id.editObsMedicamento
            )

        editNome.setText(medicamento.nome)

        editDosagem.setText(medicamento.dosagem)

        editHorario.setText(medicamento.horario)

        editObs.setText(medicamento.observacao)

        AlertDialog.Builder(this)

            .setTitle("Editar medicamento")

            .setView(dialogView)

            .setPositiveButton("Salvar") { _, _ ->

                val atualizado = hashMapOf(

                    "nome" to editNome.text.toString(),

                    "dosagem" to editDosagem.text.toString(),

                    "horario" to editHorario.text.toString(),

                    "observacao" to editObs.text.toString()
                )

                firestore
                    .collection("medicamentos")
                    .document(medicamento.id)

                    .update(atualizado as Map<String, Any>)

                val atualizadoItem = MedicamentoItem(

                    id = medicamento.id,

                    nome = editNome.text.toString(),

                    dosagem = editDosagem.text.toString(),

                    horario = editHorario.text.toString(),

                    observacao = editObs.text.toString()
                )

                agendarMedicamento(atualizadoItem)
            }

            .setNegativeButton(
                "Cancelar",
                null
            )

            .show()
    }


}