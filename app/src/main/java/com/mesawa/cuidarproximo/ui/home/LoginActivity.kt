package com.mesawa.cuidarproximo.ui.home


import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.mesawa.cuidarproximo.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.cadastros.CadastroActivity
import com.mesawa.cuidarproximo.ui.home.HomeActivity

class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Configuração do login do Google
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        val googleLoginButton = findViewById<Button>(R.id.googleLoginButton)
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }

        // Configuração do login por e-mail e senha
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val eyeIcon = findViewById<ImageView>(R.id.eyeIcon)

        // Toggle de visibilidade da senha
        eyeIcon.setOnClickListener {
            if (passwordEditText.transformationMethod == HideReturnsTransformationMethod.getInstance()) {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                eyeIcon.setImageResource(R.drawable.ic_eye) // Altere para o ícone do olho fechado
            } else {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                eyeIcon.setImageResource(R.drawable.ic_eye_open) // Altere para o ícone do olho aberto
            }
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login bem-sucedido
                        val user = auth.currentUser
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        // Falha no login, exibe a mensagem detalhada do erro
                        val errorMessage = task.exception?.message ?: "Erro desconhecido!"
                        Toast.makeText(this, "Erro no login: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Link para cadastro
        val linkToCadastro = findViewById<TextView>(R.id.linkToCadastro)
        linkToCadastro.setOnClickListener {
            // Redireciona para a tela de cadastro
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }
    }

    // Iniciar o login com o Google
    private fun signInWithGoogle() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 9001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 9001) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google login falhou", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido
                    val user = auth.currentUser
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    // Falha ao autenticar com o Google
                    Toast.makeText(this, "Falha na autenticação do Google", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

