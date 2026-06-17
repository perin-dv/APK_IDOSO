package com.mesawa.cuidarproximo.ui.pagamento

import android.content.*
import android.graphics.BitmapFactory
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximo.databinding.ActivityPagamentoPixBinding
import kotlinx.coroutines.*
import android.util.Base64
import android.util.Log
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity

class PagamentoPixActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagamentoPixBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var functions: FirebaseFunctions
    private var pollingJob: Job? = null
    private var paymentId: String? = null
    private var contratacaoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagamentoPixBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbarPix)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarPix.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Firebase Functions
        functions = FirebaseFunctions.getInstance("us-central1")

        // Recebe dados
        paymentId = intent.getStringExtra("paymentId")
        contratacaoId = intent.getStringExtra("contratacaoId") ?: ""
        val qrBase64 = intent.getStringExtra("qr_base64")
        val qrString = intent.getStringExtra("qr_string")

        // Exibe QR
        qrBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgQrCode.setImageBitmap(bitmap)
        }
        binding.txtCodigoPix.text = qrString ?: ""

        // Copiar código Pix
        binding.btnCopiarCodigo.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Pix", qrString))
            Toast.makeText(this, "Código Pix copiado!", Toast.LENGTH_SHORT).show()
        }

        // Começa polling para verificar pagamento
        iniciarVerificacaoPagamento()
    }

    private fun iniciarVerificacaoPagamento() {
        paymentId?.let {
            pollingJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    verificarStatusPagamento(it)
                    delay(5000)
                }
            }
        }
    }

    private fun verificarStatusPagamento(paymentId: String) {
        val data = hashMapOf("paymentId" to paymentId)

        functions.getHttpsCallable("verificarStatusPagamento")
            .call(data)
            .addOnSuccessListener { result ->
                val map = result.data as? Map<*, *>
                val status = map?.get("status")?.toString()
                if (status == "approved") {
                    atualizarFirebaseEAbrirTela()
                    pollingJob?.cancel()
                } else {
                    Log.d("PixStatus", "Aguardando pagamento. Status atual: $status")
                }
            }
            .addOnFailureListener {
                Log.e("PixStatus", "Erro ao verificar pagamento: ${it.message}")
            }
    }

    private fun atualizarFirebaseEAbrirTela() {
        firestore.collection("contratacoes")
            .document(contratacaoId)
            .update(
                mapOf(
                    "status" to "aguardando_cuidador",
                    "pagamentoStatus" to "aprovado",
                    "metodoPagamento" to "PIX"
                )
            )
            .addOnSuccessListener {
                startActivity(Intent(this, EmAndamentoActivity::class.java))
                finish()
            }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        super.onDestroy()
    }
}