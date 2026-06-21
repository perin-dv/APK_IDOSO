package com.mesawa.cuidarproximo

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mesawa.cuidarproximo.BaseActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.mesawa.cuidarproximo.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar o Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Definir o BottomNavigationView
        val navView: BottomNavigationView = binding.navView

        // Configurar o NavController
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)
        supportActionBar?.hide()
    }
}
