package com.mesawa.cuidarproximo.cadastros

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroViewModel : ViewModel() {

    // =========================
    // Dados da conta
    // =========================
    var nomeResponsavel: String = ""
    var telefone: String = ""
    var email: String = ""
    var senha: String = ""
    var confirmSenha: String = ""

    // =========================
    // Dados do idoso
    // =========================
    var nomeIdoso: String = ""
    var cpfIdoso: String = ""
    var dataNascimento: String = ""
    var genero: String = ""
    var cidade: String = ""
    var condicao: String = ""
    var dependencia: String = ""
    var idade: Int = 0

    // =========================
    // Dados extras
    // =========================
    var cpfCuidador: String = ""

    // =========================
    // Status do cadastro
    // =========================
    val cadastroStatus: MutableLiveData<String> = MutableLiveData()

    // =========================
    // Validação da conta
    // =========================
    fun validarCadastroConta(): Boolean {

        if (
            nomeResponsavel.isEmpty() ||
            telefone.isEmpty() ||
            email.isEmpty() ||
            senha.isEmpty() ||
            confirmSenha.isEmpty()
        ) {
            cadastroStatus.value = "erro_campos_vazios"
            Log.e("CADASTRO", "Campos da conta vazios")
            return false
        }

        if (senha != confirmSenha) {
            cadastroStatus.value = "erro_senhas_diferentes"
            Log.e("CADASTRO", "Senhas diferentes")
            return false
        }

        if (senha.length < 6) {
            cadastroStatus.value = "erro_senha_fraca"
            Log.e("CADASTRO", "Senha fraca")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            cadastroStatus.value = "erro_email_invalido"
            Log.e("CADASTRO", "Email inválido")
            return false
        }

        Log.d("CADASTRO", "Validação da conta OK")
        return true
    }

    // =========================
    // Validação do idoso
    // =========================
    fun validarCadastroIdoso(): Boolean {

        if (
            nomeIdoso.isEmpty() ||
            cpfIdoso.isEmpty() ||
            dataNascimento.isEmpty() ||
            cidade.isEmpty()
        ) {
            cadastroStatus.value = "erro_campos_idoso_vazios"
            Log.e("CADASTRO", "Campos do idoso vazios")
            return false
        }

        if (!isCPFValido(cpfIdoso)) {
            cadastroStatus.value = "erro_cpf_idoso_invalido"
            Log.e("CADASTRO", "CPF inválido")
            return false
        }

        Log.d("CADASTRO", "Validação do idoso OK")
        return true
    }

    // =========================
    // Validação CPF
    // =========================
    private fun isCPFValido(cpf: String): Boolean {

        val cleanCpf = cpf.replace("[^\\d]".toRegex(), "")

        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) {
            return false
        }

        return try {

            val numbers = cleanCpf.map { it.toString().toInt() }

            val sum1 = (0..8).sumOf {
                (10 - it) * numbers[it]
            }

            val digit1 = ((sum1 * 10) % 11).let {
                if (it == 10) 0 else it
            }

            val sum2 = (0..9).sumOf {
                (11 - it) * numbers[it]
            }

            val digit2 = ((sum2 * 10) % 11).let {
                if (it == 10) 0 else it
            }

            digit1 == numbers[9] && digit2 == numbers[10]

        } catch (e: Exception) {

            Log.e("CADASTRO", "Erro ao validar CPF", e)
            false
        }
    }

    // =========================
    // Salvar cadastro
    // =========================
    fun salvarCadastro() {

        Log.d("CADASTRO", "Iniciando validação")

        if (!validarCadastroConta() || !validarCadastroIdoso()) {

            Log.e("CADASTRO", "Validação falhou")
            return
        }

        Log.d("CADASTRO", "Validação OK")

        val auth = FirebaseAuth.getInstance()

        Log.d("CADASTRO", "Criando usuário Auth")

        auth.createUserWithEmailAndPassword(email, senha)

            .addOnSuccessListener { result ->

                Log.d("CADASTRO", "Usuário criado com sucesso")

                val firebaseUser = result.user

                if (firebaseUser == null) {

                    Log.e("CADASTRO", "FIREBASE USER NULL")

                    cadastroStatus.value = "erro"
                    return@addOnSuccessListener
                }

                val uid = firebaseUser.uid

                Log.d("CADASTRO", "UID GERADO: $uid")

                val nomeIdosoFormatado = nomeIdoso
                    .trim()
                    .lowercase()
                    .replace(" ", "_")

                val idDocumento = "${nomeIdosoFormatado}_$cpfIdoso"


                val partesData = dataNascimento.split("/")

                val idade = try {

                    val dia = partesData[0].toInt()
                    val mes = partesData[1].toInt()
                    val ano = partesData[2].toInt()

                    val hoje = java.util.Calendar.getInstance()

                    var idadeCalculada =
                        hoje.get(java.util.Calendar.YEAR) - ano

                    if (
                        hoje.get(java.util.Calendar.MONTH) + 1 < mes ||
                        (
                                hoje.get(java.util.Calendar.MONTH) + 1 == mes &&
                                        hoje.get(java.util.Calendar.DAY_OF_MONTH) < dia
                                )
                    ) {
                        idadeCalculada--
                    }

                    idadeCalculada

                } catch (e: Exception) {

                    Log.e("CADASTRO", "Erro ao calcular idade", e)

                    0
                }

                val userData = hashMapOf(

                    "info" to hashMapOf(
                        "nome_idoso" to nomeIdoso,
                        "cpf_idoso" to cpfIdoso,
                        "data_nascimento" to dataNascimento,
                        "genero" to genero,
                        "cidade" to cidade,
                        "idade" to idade,
                        "condicao" to condicao,
                        "dependencia" to dependencia
                    ),

                    "responsavel" to hashMapOf(
                        "nome_responsavel" to nomeResponsavel,
                        "telefone" to telefone,
                        "email" to email
                    ),

                    "cuidador" to hashMapOf(
                        "cpf_cuidador" to cpfCuidador
                    ),

                    "sistema" to hashMapOf(
                        "uid_auth" to uid,
                        "timestamp" to System.currentTimeMillis()
                    )
                )

                Log.d("CADASTRO", "Salvando Firestore")

                FirebaseFirestore.getInstance()
                    .collection("clientes")
                    .document(idDocumento)
                    .set(userData)

                    .addOnSuccessListener {

                        Log.d(
                            "CADASTRO",
                            "Firestore salvo com sucesso"
                        )

                        cadastroStatus.value = "sucesso"
                    }

                    .addOnFailureListener { exception ->

                        Log.e(
                            "CADASTRO",
                            "Erro Firestore",
                            exception
                        )

                        cadastroStatus.value = "erro"
                    }
            }

            .addOnFailureListener { e ->

                Log.e(
                    "CADASTRO",
                    "ERRO CREATE USER",
                    e
                )

                if (
                    e.message?.contains(
                        "email address is already in use",
                        true
                    ) == true
                ) {

                    cadastroStatus.value = "erro_email_existente"

                } else {

                    cadastroStatus.value = "erro"
                }
            }
    }
}