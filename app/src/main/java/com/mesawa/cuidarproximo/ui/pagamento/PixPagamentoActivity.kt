package com.mesawa.cuidarproximo.ui.pagamento


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.functions.FirebaseFunctions
import com.mesawa.cuidarproximo.databinding.ActivityPagamentoPixBinding
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity

class PagamentoPixActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagamentoPixBinding
    private lateinit var functions: FirebaseFunctions

    private var contratacaoId: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var verificandoPagamento = false
    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!verificandoPagamento) return

            verificarStatusPagamento()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPagamentoPixBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = FirebaseFunctions.getInstance("us-central1")
        supportActionBar?.hide()

        contratacaoId = intent.getStringExtra("contratacaoId") ?: ""

        val qrBase64 = intent.getStringExtra("qr_base64")
        val qrString = intent.getStringExtra("qr_string")

        val cuidadorNome =
            intent.getStringExtra("cuidadorNome") ?: "Cuidador"

        val valor =
            intent.getDoubleExtra("valor", 0.0)

        binding.txtCuidadorPix.text =
            "Cuidador: $cuidadorNome"

        binding.txtValorPix.text =
            "Total: R$ %.2f".format(valor)

        qrBase64?.let {

            val imageBytes =
                Base64.decode(it, Base64.DEFAULT)

            val bitmap =
                BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size
                )

            binding.imgQrCode.setImageBitmap(bitmap)
        }

        binding.txtCodigoPix.text =
            qrString ?: ""

        binding.btnCopiarCodigo.setOnClickListener {

            val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager

            val clip =
                ClipData.newPlainText(
                    "Pix",
                    qrString
                )

            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                "Código PIX copiado",
                Toast.LENGTH_SHORT
            ).show()
        }

        iniciarVerificacaoPagamento()
    }

    private fun iniciarVerificacaoPagamento() {
        if (contratacaoId.isBlank()) {
            Toast.makeText(this, "Contratação inválida", Toast.LENGTH_LONG).show()
            return
        }

        verificandoPagamento = true
        handler.post(pollingRunnable)
    }

    private fun verificarStatusPagamento() {

        val data =
            hashMapOf(
                "contratacaoId" to contratacaoId
            )

        functions
            .getHttpsCallable("verificarStatusPagamento")
            .call(data)

            .addOnSuccessListener { result ->

                val map =
                    result.data as? Map<*, *>

                val status =
                    map?.get("status")?.toString()

                Log.d(
                    "PIX_STATUS",
                    "Status: $status"
                )

                if (status == "approved") {

                    pararVerificacaoPagamento()

                    confirmarPagamento()
                }
            }

            .addOnFailureListener {

                Log.e(
                    "PIX_STATUS",
                    it.message ?: "Erro"
                )
            }
    }

    private fun confirmarPagamento() {
        functions
            .getHttpsCallable("confirmarPagamentoContratacao")
            .call(
                mapOf(
                    "contratacaoId" to contratacaoId
                )
            )

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Pagamento aprovado!",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(
                    android.content.Intent(
                        this,
                        EmAndamentoActivity::class.java
                    ).putExtra("contratacaoId", contratacaoId)
                )

                finish()
            }

            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Pagamento aprovado, mas houve erro ao confirmar a contratação",
                    Toast.LENGTH_LONG
                ).show()

                Log.e(
                    "PIX_CONFIRMAR",
                    it.message ?: "Erro ao confirmar pagamento"
                )
            }
    }

    override fun onDestroy() {

        pararVerificacaoPagamento()

        super.onDestroy()
    }

    private fun pararVerificacaoPagamento() {
        verificandoPagamento = false
        handler.removeCallbacks(pollingRunnable)
    }
}
