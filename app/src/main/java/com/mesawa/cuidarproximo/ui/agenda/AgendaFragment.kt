package com.mesawa.cuidarproximo.ui.agenda


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.ProgressBar
import java.util.Calendar
import android.widget.Button
import com.mesawa.cuidarproximo.ui.notificacao.ReminderReceiver
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import com.mesawa.cuidarproximo.ui.notificacao.NotificationHelper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R

class AgendaFragment : Fragment() {

    private lateinit var recyclerAgenda: RecyclerView
    private lateinit var btnAdicionar: FloatingActionButton

    private lateinit var adapter: AgendaAdapter

    private lateinit var txtConcluidas: TextView
    private lateinit var txtPendentes: TextView
    private lateinit var txtEmergencias: TextView

    private lateinit var progressRotina: ProgressBar

    private lateinit var txtProgressoRotina: TextView

    private val listaAgenda = mutableListOf<AgendaItem>()

    private val listaOriginal = mutableListOf<AgendaItem>()

    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_agenda,
            container,
            false
        )
        val swipe = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {

                val position = viewHolder.adapterPosition

                val item = listaAgenda[position]

                firestore
                    .collection("agenda")
                    .document(item.id)
                    .delete()
            }
        }


        recyclerAgenda =
            view.findViewById(R.id.recyclerAgenda)

        btnAdicionar =
            view.findViewById(R.id.btnAdicionarAgenda)


        val btnTodos =
            view.findViewById<Button>(R.id.btnTodos)

        val btnRemedios =
            view.findViewById<Button>(R.id.btnRemedios)

        val btnConsulta =
            view.findViewById<Button>(R.id.btnConsulta)

        val btnEmergencia =
            view.findViewById<Button>(R.id.btnEmergencia)

        ItemTouchHelper(swipe)
            .attachToRecyclerView(recyclerAgenda)


        txtConcluidas =
            view.findViewById(R.id.txtConcluidas)

        txtPendentes =
            view.findViewById(R.id.txtPendentes)

        txtEmergencias =
            view.findViewById(R.id.txtEmergencias)
        recyclerAgenda.layoutManager =
            LinearLayoutManager(requireContext())


        progressRotina =
            view.findViewById(R.id.progressRotina)

        txtProgressoRotina =
            view.findViewById(R.id.txtProgressoRotina)


        adapter = AgendaAdapter(listaAgenda)

        recyclerAgenda.adapter = adapter

        carregarAgenda()

        btnAdicionar.setOnClickListener {
            abrirDialogAdicionar()
        }

        btnTodos.setOnClickListener {

            listaAgenda.clear()
            listaAgenda.addAll(listaOriginal)

            adapter.notifyDataSetChanged()
        }

        btnRemedios.setOnClickListener {

            filtrarCategoria("Remédio")
        }

        btnConsulta.setOnClickListener {

            filtrarCategoria("Consulta")
        }

        btnEmergencia.setOnClickListener {

            filtrarCategoria("Emergência")
        }
        return view
    }

    private fun atualizarProgressoRotina() {

        val total =
            listaOriginal.size

        val concluidas =
            listaOriginal.count { it.concluido }

        if (total == 0) {

            progressRotina.progress = 0

            txtProgressoRotina.text =
                "Nenhuma tarefa hoje"

            return
        }

        val porcentagem =
            (concluidas * 100) / total

        progressRotina.progress =
            porcentagem

        txtProgressoRotina.text =
            "$porcentagem% concluído • $concluidas de $total tarefas"
    }

    private fun atualizarDashboard() {

        val concluidas =
            listaOriginal.count { it.concluido }

        val pendentes =
            listaOriginal.count { !it.concluido }

        val emergencias =
            listaOriginal.count {
                it.categoria == "Emergência"
            }

        txtConcluidas.text =
            concluidas.toString()

        txtPendentes.text =
            pendentes.toString()

        txtEmergencias.text =
            emergencias.toString()
    }
    // =========================
    // CARREGAR FIREBASE
    // =========================

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarAgenda() {

        firestore
            .collection("agenda")

            .addSnapshotListener { value, error ->

                if (error != null) {

                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar agenda",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addSnapshotListener
                }

                listaAgenda.clear()
                listaOriginal.clear()

                value?.documents?.forEach { document ->

                    val item =
                        document.toObject(AgendaItem::class.java)

                    item?.id = document.id

                    if (item != null) {
                        listaAgenda.add(item)
                        listaOriginal.add(item)
                    }
                }


                listaAgenda.sortBy { item ->

                    val partes = item.hora.split(":")

                    val hora =
                        partes.getOrNull(0)?.toIntOrNull() ?: 0

                    val minuto =
                        partes.getOrNull(1)?.toIntOrNull() ?: 0

                    hora * 60 + minuto
                }

                atualizarDashboard()
                atualizarProgressoRotina()
                adapter.notifyDataSetChanged()
            }
    }


    private fun filtrarCategoria(
        categoria: String
    ) {

        listaAgenda.clear()

        val filtrados = listaOriginal.filter {

            it.categoria == categoria
        }

        listaAgenda.addAll(filtrados)

        adapter.notifyDataSetChanged()
    }
    // =========================
    // DIALOG
    // =========================

    @SuppressLint("DefaultLocale")
    private fun abrirDialogAdicionar() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_add_agenda,
            null
        )

        val editHora =
            dialogView.findViewById<EditText>(R.id.editHora)

        editHora.isFocusable = false

        editHora.setOnClickListener {

            val calendar = Calendar.getInstance()

            val horaAtual =
                calendar.get(Calendar.HOUR_OF_DAY)

            val minutoAtual =
                calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                requireContext(),

                { _, horaSelecionada, minutoSelecionado ->

                    val horaFormatada =
                        String.format(
                            "%02d:%02d",
                            horaSelecionada,
                            minutoSelecionado
                        )

                    editHora.setText(horaFormatada)
                },

                horaAtual,
                minutoAtual,
                true
            )

            timePicker.show()
        }

        val editTitulo =
            dialogView.findViewById<EditText>(R.id.editTitulo)

        val editDescricao =
            dialogView.findViewById<EditText>(R.id.editDescricao)
        val spinnerCategoria =
            dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        val spinnerPrioridade =
            dialogView.findViewById<Spinner>(R.id.spinnerPrioridade)


        val spinnerRepeticao =
            dialogView.findViewById<Spinner>(
                R.id.spinnerRepeticao
            )


        spinnerCategoria.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Remédio",
                "Consulta",
                "Cuidador",
                "Exercício",
                "Alimentação",
                "Emergência"
            )
        )

        spinnerPrioridade.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Baixa",
                "Média",
                "Alta"
            )
        )

        spinnerRepeticao.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Uma vez",
                "Todo dia",
                "Toda semana"
            )
        )



        AlertDialog.Builder(requireContext())
            .setTitle("Nova tarefa")
            .setView(dialogView)

            .setPositiveButton("Salvar") { _, _ ->

                val hora = editHora.text.toString()
                val titulo = editTitulo.text.toString()
                val descricao = editDescricao.text.toString()
                val categoria =
                    spinnerCategoria.selectedItem.toString()

                val prioridade =
                    spinnerPrioridade.selectedItem.toString()

                val repeticao =
                    spinnerRepeticao.selectedItem.toString()

                if (
                    hora.isNotEmpty() &&
                    titulo.isNotEmpty()
                ) {

                    salvarAgendaFirebase(
                        hora,
                        titulo,
                        descricao,
                        categoria,
                        prioridade,
                        repeticao,
                    )
                }
            }

            .setNegativeButton("Cancelar", null)
            .show()
    }

    // =========================
    // SALVAR FIREBASE
    // =========================

    private fun salvarAgendaFirebase(
        hora: String,
        titulo: String,
        descricao: String,
        categoria: String,
        prioridade: String,
        repeticao: String
    ) {

        val agenda = hashMapOf(

            "hora" to hora,
            "titulo" to titulo,
            "descricao" to descricao,
            "timestamp" to System.currentTimeMillis(),
            "categoria" to categoria,
            "prioridade" to prioridade,
            "concluido" to false,
            "repeticao" to repeticao
        )

        firestore
            .collection("agenda")
            .add(agenda)

            .addOnSuccessListener {


                NotificationHelper(requireContext())
                    .mostrarNotificacao(
                        "Novo lembrete",
                        titulo
                    )


                agendarNotificacao(
                    hora,
                    titulo,
                    descricao
                )

                Toast.makeText(
                    requireContext(),
                    "Tarefa adicionada",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .addOnFailureListener {

                Toast.makeText(
                    requireContext(),
                    "Erro ao salvar",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    // =========================
    // NOTIFICACAO
    // =========================

    private fun agendarNotificacao(
        hora: String,
        titulo: String,
        descricao: String
    ) {

        try {

            val partes = hora.split(":")

            val horaInt = partes[0].toInt()
            val minutoInt = partes[1].toInt()

            val calendar = Calendar.getInstance()

            calendar.set(
                Calendar.HOUR_OF_DAY,
                horaInt
            )

            calendar.set(
                Calendar.MINUTE,
                minutoInt
            )

            calendar.set(Calendar.SECOND, 0)

            val intent = Intent(
                requireContext(),
                ReminderReceiver::class.java
            )

            intent.putExtra("titulo", titulo)
            intent.putExtra("descricao", descricao)

            val pendingIntent =
                PendingIntent.getBroadcast(
                    requireContext(),
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or
                            PendingIntent.FLAG_UPDATE_CURRENT
                )

            val alarmManager =
                requireContext().getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

                if (alarmManager.canScheduleExactAlarms()) {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

            } else {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}