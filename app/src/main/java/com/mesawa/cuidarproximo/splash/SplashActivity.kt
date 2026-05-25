package com.mesawa.cuidarproximo.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.home.HomeActivity

import com.mesawa.cuidarproximo.home.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animação suave para a logo
        val logo = findViewById<ImageView>(R.id.logo_splash)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logo.startAnimation(fadeInAnimation)

        // Aguardar a animação terminar e verificar o login
        Handler().postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Usuário logado, vai para a tela principal
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // Usuário não logado, vai para o login
                startActivity(Intent(this, LoginActivity::class.java))
            }

            finish() // Finaliza a SplashActivity para não permitir voltar
        }, 2000) // 2 segundos de splash
    }
}