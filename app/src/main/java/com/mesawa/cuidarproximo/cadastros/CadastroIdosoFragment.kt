package com.mesawa.cuidarproximo.cadastros

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximo.R
import java.util.*

class CadastroIdosoFragment : Fragment() {

    private lateinit var nomeIdoso: EditText
    private lateinit var cpfIdoso: EditText
    private lateinit var dataNascimento: EditText
    private lateinit var cidade: EditText

    private lateinit var spinnerGenero: Spinner
    private lateinit var spinnerCondicao: Spinner
    private lateinit var spinnerDependencia: Spinner

    private lateinit var editTextOutro: EditText
    private lateinit var btnContinuar: Button

    private lateinit var viewModel: CadastroViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cadastro_idoso, container, false)

        viewModel = ViewModelProvider(requireActivity())[CadastroViewModel::class.java]

        nomeIdoso = view.findViewById(R.id.editTextNomeIdoso)
        cpfIdoso = view.findViewById(R.id.editTextCpfIdoso)
        dataNascimento = view.findViewById(R.id.editTextDataNascimento)
        cidade = view.findViewById(R.id.editTextCidade)

        spinnerGenero = view.findViewById(R.id.spinnerGenero)
        spinnerCondicao = view.findViewById(R.id.spinnerCondicao)
        spinnerDependencia = view.findViewById(R.id.spinnerDependencia)

        editTextOutro = view.findViewById(R.id.editTextCondicaoOutro)
        btnContinuar = view.findViewById(R.id.buttonContinuarIdoso)

        // 🔥 Aplicar a máscara de CPF
        cpfIdoso.addTextChangedListener(MascaraCPF(cpfIdoso))

        // 🔥 Bloqueia digitação manual (mais profissional)
        dataNascimento.isFocusable = false

        dataNascimento.setOnClickListener {
            mostrarDatePicker()
        }

        // Gênero
        spinnerGenero.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Masculino", "Feminino", "Outro")
        )

        // Condição
        val condicoes = arrayOf(
            "Saudável",
            "Hipertensão",
            "Diabetes",
            "Alzheimer",
            "Parkinson",
            "Mobilidade reduzida",
            "Acamado",
            "Outro"
        )

        spinnerCondicao.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            condicoes
        )

        spinnerCondicao.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                editTextOutro.visibility =
                    if (condicoes[position] == "Outro") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Dependência
        spinnerDependencia.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Independente", "Parcial", "Total")
        )

        // Botão
        btnContinuar.setOnClickListener {
            // Coleta os dados mas não salva no Firestore ainda
            viewModel.nomeIdoso = nomeIdoso.text.toString()
            viewModel.cpfIdoso = cpfIdoso.text.toString()
            viewModel.dataNascimento = dataNascimento.text.toString()
            viewModel.cidade = cidade.text.toString()
            viewModel.genero = spinnerGenero.selectedItem.toString()

            viewModel.condicao = if (spinnerCondicao.selectedItem.toString() == "Outro") {
                editTextOutro.text.toString()
            } else {
                spinnerCondicao.selectedItem.toString()
            }

            viewModel.dependencia = spinnerDependencia.selectedItem.toString()

            // Navega para o próximo fragmento sem salvar
            (activity as CadastroActivity).navegarPara(CadastroExtraFragment())
        }
        return view
    }

    // 📅 Date Picker (Profissional)
    @SuppressLint("DefaultLocale")
    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val dataFormatada = String.format("%02d/%02d/%04d", day, month + 1, year)
                dataNascimento.setText(dataFormatada)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    // 🔥 Validação CPF Segura
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
        } catch (e: Exception) {
            false
        }
    }

    // Validação dos campos
    private fun validarCampos(): Boolean {

        if (nomeIdoso.text.toString().isEmpty() ||
            cpfIdoso.text.toString().isEmpty() ||
            dataNascimento.text.toString().isEmpty() ||
            cidade.text.toString().isEmpty()
        ) {
            Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isCPFValido(cpfIdoso.text.toString())) {
            Toast.makeText(context, "CPF inválido!", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // Máscara CPF
    class MascaraCPF(private val editText: EditText) : TextWatcher {

        private var isUpdating = false

        override fun afterTextChanged(s: Editable?) {
            if (s == null) return
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