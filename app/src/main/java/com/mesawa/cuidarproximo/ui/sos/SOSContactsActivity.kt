package com.mesawa.cuidarproximo.ui.sos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.profile.ContatoSOS
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.mesawa.cuidarproximo.location.LocationHelper



class SOSContactsActivity : AppCompatActivity() {

    private lateinit var recyclerContatos: RecyclerView

    private lateinit var btnAdicionar: FloatingActionButton

    private lateinit var adapter: SOSContactsAdapter

    private val lista = mutableListOf<ContatoSOS>()

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sos_contacts)

        recyclerContatos =
            findViewById(R.id.recyclerContatosSOS)

        btnAdicionar =
            findViewById(R.id.btnAdicionarContato)

        recyclerContatos.layoutManager =
            LinearLayoutManager(this)

        adapter = SOSContactsAdapter(lista)

        recyclerContatos.adapter = adapter
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )
        }

        LocationHelper(this)
            .obterLocalizacao { lat, long ->

                println("LATITUDE: $lat")
                println("LONGITUDE: $long")
            }


        carregarContatos()

        btnAdicionar.setOnClickListener {

            abrirDialogContato()
        }
    }

    private fun carregarContatos() {

        firestore
            .collection("contatos_sos")

            .addSnapshotListener { value, _ ->

                lista.clear()

                value?.documents?.forEach { document ->

                    val contato =
                        document.toObject(ContatoSOS::class.java)

                    contato?.id = document.id

                    if (contato != null) {

                        lista.add(contato)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun abrirDialogContato() {

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_contato_sos, null)

        val editNome =
            dialogView.findViewById<EditText>(R.id.editNomeContato)

        val editTelefone =
            dialogView.findViewById<EditText>(R.id.editTelefoneContato)

        val editParentesco =
            dialogView.findViewById<EditText>(R.id.editParentesco)

        AlertDialog.Builder(this)
            .setTitle("Novo contato SOS")
            .setView(dialogView)

            .setPositiveButton("Salvar") { _, _ ->

                val contato = hashMapOf(

                    "nome" to editNome.text.toString(),

                    "telefone" to editTelefone.text.toString(),

                    "parentesco" to editParentesco.text.toString()
                )

                firestore
                    .collection("contatos_sos")
                    .add(contato)

                Toast.makeText(
                    this,
                    "Contato salvo",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .setNegativeButton("Cancelar", null)
            .show()
    }
}