package com.mesawa.cuidarproximo.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.nav_bottom.SOSFragment
import com.mesawa.cuidarproximo.ui.home.fragments.ProfileFragment
import com.mesawa.cuidarproximo.ui.home.fragments.SettingsFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()


        // Configura o BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Define o fragment inicial (Home)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Configura a navegação no BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.navigation_home -> replaceFragment(HomeFragment())

                R.id.navigation_profile -> replaceFragment(ProfileFragment())

                R.id.navigation_sos -> replaceFragment(SOSFragment())


              //  R.id.navigation_agenda -> replaceFragment(SOSFragment())

               // R.id.navigation_buscar -> replaceFragment(SOSFragment())

                else -> false
            }
        }
    }

    // Função para substituir os fragments
    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
}