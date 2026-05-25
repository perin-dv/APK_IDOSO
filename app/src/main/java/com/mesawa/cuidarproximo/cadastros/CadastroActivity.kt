package com.mesawa.cuidarproximo.cadastros


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximo.R

class CadastroActivity : AppCompatActivity() {

    private lateinit var cadastroContaViewModel: CadastroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)
        supportActionBar?.hide()

        cadastroContaViewModel = ViewModelProvider(this)[CadastroViewModel::class.java]



        // Inicia com o fragmento de cadastro de conta
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CadastroContaFragment())
                .commit()
        }
    }

    // Função para trocar fragmentos (essencial para navegação)
    fun navegarPara(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Adiciona a transação ao back stack para permitir voltar
            .commit()
    }
}