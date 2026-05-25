package com.mesawa.cuidarproximo.cadastros

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximo.R

class CadastroContaFragment : Fragment() {

    private lateinit var nomeResponsavel: EditText
    private lateinit var telefone: EditText
    private lateinit var email: EditText
    private lateinit var senha: EditText
    private lateinit var confirmSenha: EditText
    private lateinit var btnContinuar: Button
    private lateinit var txtEmailStatus: TextView
    private lateinit var txtSenhaStatus: TextView

    private lateinit var viewModel: CadastroViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cadastro_conta, container, false)

        viewModel = ViewModelProvider(requireActivity())[CadastroViewModel::class.java]

        nomeResponsavel = view.findViewById(R.id.editTextNomeResponsavel)
        telefone = view.findViewById(R.id.editTextTelefone)
        email = view.findViewById(R.id.editTextEmail)
        senha = view.findViewById(R.id.editTextPassword)
        confirmSenha = view.findViewById(R.id.editTextConfirmPassword)
        btnContinuar = view.findViewById(R.id.buttonContinuar)
        txtEmailStatus = view.findViewById(R.id.txtEmailStatus)
        txtSenhaStatus = view.findViewById(R.id.txtSenhaStatus)

        // Aplicar a máscara para o telefone
        telefone.addTextChangedListener(MascaraTelefone(telefone))

        // Validação do email
        email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val emailTexto = s.toString()

                if (Patterns.EMAIL_ADDRESS.matcher(emailTexto).matches()) {
                    txtEmailStatus.text = "Email válido ✔"
                    txtEmailStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                } else {
                    txtEmailStatus.text = "Email inválido"
                    txtEmailStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validação de senha
        confirmSenha.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val senhaTexto = senha.text.toString()
                val confirmTexto = confirmSenha.text.toString()

                if (senhaTexto.length < 6) {
                    txtSenhaStatus.text = "Senha fraca (mín 6 caracteres)"
                    txtSenhaStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    return
                }

                if (senhaTexto == confirmTexto) {
                    txtSenhaStatus.text = "Senhas corretas ✔"
                    txtSenhaStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                } else {
                    txtSenhaStatus.text = "Senhas não coincidem"
                    txtSenhaStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Observando o status do cadastro
        viewModel.cadastroStatus.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                "sucesso" -> {
                    (activity as CadastroActivity).navegarPara(CadastroIdosoFragment()) // Navega para o próximo fragmento
                }
                "erro" -> {
                    showToast("Erro desconhecido!")
                }
                "erro_campos_vazios" -> {
                    showToast("Preencha todos os campos!")
                }
                "erro_senhas_diferentes" -> {
                    showToast("As senhas não coincidem!")
                }
                "erro_senha_fraca" -> {
                    showToast("A senha deve ter no mínimo 6 caracteres!")
                }
                "erro_email_invalido" -> {
                    showToast("Email inválido!")
                }
            }
        })

        // Lógica do botão "Continuar"
        btnContinuar.setOnClickListener {
            // Coleta os dados mas não salva no Firestore ainda
            viewModel.nomeResponsavel = nomeResponsavel.text.toString()
            viewModel.telefone = telefone.text.toString()
            viewModel.email = email.text.toString()
            viewModel.senha = senha.text.toString()
            viewModel.confirmSenha = confirmSenha.text.toString()

            if (viewModel.validarCadastroConta()) {
                // Navega para o próximo fragmento sem salvar
                (activity as CadastroActivity).navegarPara(CadastroIdosoFragment())
            } else {
                showToast("Campos inválidos ou incompletos!")
            }
        }

        return view
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Máscara para o telefone
    class MascaraTelefone(private val telefone: EditText) : TextWatcher {

        private var isUpdating = false

        override fun afterTextChanged(s: Editable?) {
            if (s == null) return
            if (isUpdating) return

            isUpdating = true

            val str = s.toString().replace("[^\\d]".toRegex(), "")
            val formatted = StringBuilder()

            for (i in str.indices) {
                formatted.append(str[i])

                if (i == 1) formatted.append(") ")
                if (i == 6) formatted.append("-")
            }

            val result = if (str.length >= 2) {
                "(" + formatted.toString()
            } else {
                str
            }

            telefone.setText(result)
            telefone.setSelection(result.length)

            isUpdating = false
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}