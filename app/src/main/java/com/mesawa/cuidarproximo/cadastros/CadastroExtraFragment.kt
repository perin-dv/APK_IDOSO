package com.mesawa.cuidarproximo.cadastros

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximo.MainActivity
import com.mesawa.cuidarproximo.R
import com.mesawa.cuidarproximo.ui.home.HomeActivity
import com.mesawa.cuidarproximo.ui.home.HomeFragment

class CadastroExtraFragment : Fragment() {

    private lateinit var cpfCuidador: EditText
    private lateinit var txtCpfStatus: TextView
    private lateinit var checkBoxTerms: CheckBox
    private lateinit var checkBoxEmail: CheckBox
    private lateinit var btnFinalizar: Button
    private lateinit var btnVerTermos: Button

    private lateinit var viewModel: CadastroViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cadastro_extra, container, false)

        viewModel = ViewModelProvider(requireActivity())[CadastroViewModel::class.java]

        cpfCuidador = view.findViewById(R.id.editTextCpfCuidador)
        txtCpfStatus = view.findViewById(R.id.txtCpfStatus)
        checkBoxTerms = view.findViewById(R.id.checkBoxTerms)
        checkBoxEmail = view.findViewById(R.id.checkBoxEmail)
        btnFinalizar = view.findViewById(R.id.buttonFinalizar)
        btnVerTermos = view.findViewById(R.id.btnVerTermos)

        // 📞 MÁSCARA + VALIDAÇÃO CPF
        cpfCuidador.addTextChangedListener(MascaraCPF(cpfCuidador))
        cpfCuidador.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cpf = s.toString()
                val cpfValido = isCPFValido(cpf)
                txtCpfStatus.text = if (cpfValido) "CPF válido ✔" else "CPF inválido"
                txtCpfStatus.setTextColor(resources.getColor(
                    if (cpfValido) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                ))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 📜 EXIBIR TERMOS
        btnVerTermos.setOnClickListener {
            // Criar o AlertDialog para mostrar os termos
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Termos e Condições")
            builder.setMessage(
                "TERMOS DE USO – CUIDAR PRÓXIMO\n\n" +
                        "1. OBJETIVO DA PLATAFORMA\n" +
                        "A plataforma Cuidar Próximo tem como objetivo conectar cuidadores a pessoas idosas que necessitam de assistência, facilitando a organização de informações e o acompanhamento de cuidados.\n\n" +
                        "2. RESPONSABILIDADE DAS INFORMAÇÕES\n" +
                        "O usuário declara que todas as informações fornecidas são verdadeiras, completas e atualizadas. A plataforma não se responsabiliza por dados incorretos inseridos.\n\n" +
                        "3. NATUREZA DO SERVIÇO\n" +
                        "A plataforma atua apenas como intermediadora, não sendo responsável pela execução direta dos serviços prestados pelos cuidadores.\n\n" +
                        "4. CONDIÇÕES DE SAÚDE\n" +
                        "As informações sobre saúde possuem caráter informativo e não substituem avaliação médica profissional.\n\n" +
                        "5. PRIVACIDADE E DADOS\n" +
                        "Os dados fornecidos serão armazenados com segurança e utilizados apenas para funcionamento da plataforma, conforme a Lei Geral de Proteção de Dados (LGPD).\n\n" +
                        "6. SEGURANÇA DA CONTA\n" +
                        "O usuário é responsável por manter a confidencialidade de suas credenciais de acesso.\n\n" +
                        "7. LIMITAÇÃO DE RESPONSABILIDADE\n" +
                        "A plataforma não se responsabiliza por danos decorrentes do uso dos serviços.\n\n" +
                        "8. USO ADEQUADO\n" +
                        "É proibido utilizar a plataforma para fins ilegais ou fraudulentos.\n\n" +
                        "9. CANCELAMENTO\n" +
                        "O usuário pode solicitar o encerramento da conta a qualquer momento.\n\n" +
                        "10. ALTERAÇÕES\n" +
                        "Os termos podem ser atualizados a qualquer momento, sendo responsabilidade do usuário revisá-los.\n\n" +
                        "Ao utilizar a plataforma, você concorda com todos os termos acima."
            )

            builder.setPositiveButton("Fechar") { dialog, _ -> dialog.dismiss() }
            builder.setCancelable(true)
            builder.create().show()
        }

        // 🚀 FINALIZAR CADASTRO
        btnFinalizar.setOnClickListener {
            // Atualiza dados no ViewModel
            viewModel.cpfCuidador = cpfCuidador.text.toString()

            if (validarCampos()) {
                btnFinalizar.isEnabled = false
                viewModel.salvarCadastro()
            } else {
                showToast("Preencha todos os campos!")
            }
        }

        // Observa status do cadastro
        viewModel.cadastroStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                "sucesso" -> {
                    Toast.makeText(requireContext(), "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }

                "erro" -> {
                    Toast.makeText(requireContext(), "Erro ao realizar o cadastro. Tente novamente.", Toast.LENGTH_SHORT).show()
                    btnFinalizar.isEnabled = true
                }
            }
        }

        return view
    }

    private fun validarCampos(): Boolean {
        return when {
            cpfCuidador.text.isEmpty() -> { showToast("Informe o CPF!"); false }
            !isCPFValido(cpfCuidador.text.toString()) -> { showToast("CPF inválido!"); false }
            !checkBoxTerms.isChecked -> { showToast("Aceite os termos!"); false }
            else -> true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isCPFValido(cpf: String): Boolean {
        val cleanCpf = cpf.replace("[^\\d]".toRegex(), "")
        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) return false
        return try {
            val numbers = cleanCpf.map { it.toString().toInt() }
            val sum1 = (0..8).sumOf { (10 - it) * numbers[it] }
            val digit1 = ((sum1 * 10) % 11).let { if (it == 10) 0 else it }
            val sum2 = (0..9).sumOf { (11 - it) * numbers[it] }
            val digit2 = ((sum2 * 10) % 11).let { if (it == 10) 0 else it }
            digit1 == numbers[9] && digit2 == numbers[10]
        } catch (e: Exception) { false }
    }

    class MascaraCPF(private val editText: EditText) : TextWatcher {
        private var isUpdating = false
        override fun afterTextChanged(s: Editable?) {
            if (isUpdating) return
            isUpdating = true
            val str = s.toString().replace("[^\\d]".toRegex(), "")
            val formatted = StringBuilder()
            for (i in str.indices) {
                formatted.append(str[i])
                if (i == 2 || i == 5) formatted.append(".")
                if (i == 8) formatted.append("-")
            }
            editText.setText(formatted.toString())
            editText.setSelection(formatted.length)
            isUpdating = false
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}