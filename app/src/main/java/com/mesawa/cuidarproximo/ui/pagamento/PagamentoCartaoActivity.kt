package com.mesawa.cuidarproximo.ui.pagamento

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import com.mesawa.cuidarproximo.BaseActivity
import com.google.firebase.functions.FirebaseFunctions
import com.mesawa.cuidarproximo.databinding.ActivityPagamentoCartaoBinding
import com.mesawa.cuidarproximo.ui.andamento.EmAndamentoActivity

class PagamentoCartaoActivity : BaseActivity() {

    private lateinit var binding: ActivityPagamentoCartaoBinding
    private lateinit var functions: FirebaseFunctions
    private var contratacaoId: String = ""
    private var contratacaoOwnerId: String = ""
    private var confirmandoPagamento = false
    private var initPoint: String = ""
    private var ultimoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagamentoCartaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarCartao.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbarCartao.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Firebase Functions
        functions = FirebaseFunctions.getInstance("us-central1")

        // Dados recebidos
        carregarDadosIntent(intent)

        Log.d(
            "PagamentoCartao",
            "onCreate contratacaoId=$contratacaoId initPointVazio=${initPoint.isBlank()}"
        )

        if (tratarUrlRetorno(intent.data?.toString())) {
            return
        }

        if (contratacaoId.isBlank() || initPoint.isBlank()) {
            mostrarStatus(
                "Checkout inválido. Volte e tente gerar o pagamento novamente.",
                carregando = false
            )
            return
        }

        WebView.setWebContentsDebuggingEnabled(true)
        mostrarStatus("Carregando Mercado Pago...", carregando = true)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webPagamento, true)

        binding.webPagamento.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString =
                settings.userAgentString + " CuidarProximoAndroidWebView"

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    val popupWebView = WebView(this@PagamentoCartaoActivity)
                    configurarPopupWebView(popupWebView)
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = popupWebView
                    resultMsg?.sendToTarget()
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString()
                    Log.d("PagamentoCartao", "Navegando: $url")
                    if (tratarUrlRetorno(url)) return true

                    if (!url.isNullOrBlank() && !url.startsWith("http")) {
                        return tratarUrlEspecial(url)
                    }

                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) ultimoUrl = url
                    Log.d("PagamentoCartao", "Checkout carregou: $url")
                    mostrarStatus("Mercado Pago aberto. Conclua o pagamento.", carregando = false)
                    tratarUrlRetorno(url)
                    CookieManager.getInstance().flush()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        Log.e(
                            "PagamentoCartao",
                            "Erro WebView ${error?.errorCode}: ${error?.description} url=${request.url}"
                        )
                        mostrarStatus(
                            "Falha ao carregar Mercado Pago. Tentando novamente...",
                            carregando = true
                        )
                        view?.postDelayed({ view.loadUrl(initPoint) }, 1200)
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.e(
                        "PagamentoCartao",
                        "WebView fechou o processo. didCrash=${detail?.didCrash()}"
                    )
                    mostrarStatus(
                        "A tela do Mercado Pago reiniciou. Carregando de novo...",
                        carregando = true
                    )
                    binding.webPagamento.postDelayed({
                        binding.webPagamento.loadUrl(ultimoUrl.ifBlank { initPoint })
                    }, 800)
                    return true
                }
            }
            loadUrl(initPoint)
        }
    }

    private fun configurarPopupWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
            request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString()
                Log.d("PagamentoCartao", "Popup navegando: $url")
                if (tratarUrlRetorno(url)) return true
                if (!url.isNullOrBlank()) {
                    binding.webPagamento.loadUrl(url)
                    return true
                }
                return false
            }
        }
    }

    private fun tratarUrlRetorno(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("cuidarproximo://pagamento")) {
            return false
        }

        Log.d("PagamentoCartao", "Retorno Mercado Pago: $url")

        return when {
            url.contains("resultado=sucesso") -> {
                val uri = Uri.parse(url)
                val paymentId =
                    uri.getQueryParameter("payment_id")
                        ?: uri.getQueryParameter("collection_id")

                confirmarPagamentoCartao(paymentId)
                true
            }
            url.contains("resultado=pendente") -> {
                mostrarStatus(
                    "Pagamento pendente. Conclua pelo Mercado Pago.",
                    carregando = false
                )
                true
            }
            url.contains("resultado=erro") -> {
                mostrarStatus(
                    "Pagamento não aprovado. Tente novamente.",
                    carregando = false
                )
                if (initPoint.isNotBlank()) {
                    binding.webPagamento.loadUrl(initPoint)
                }
                true
            }
            else -> false
        }
    }

    private fun tratarUrlEspecial(url: String): Boolean {
        if (url.startsWith("intent://")) {
            runCatching {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")

                if (!fallbackUrl.isNullOrBlank()) {
                    Log.d("PagamentoCartao", "Usando fallback web do intent: $fallbackUrl")
                    binding.webPagamento.loadUrl(fallbackUrl)
                } else {
                    mostrarStatus(
                        "Mercado Pago pediu outro app, mas vou manter o checkout aqui.",
                        carregando = false
                    )
                }
            }.onFailure {
                Log.e("PagamentoCartao", "Falha ao tratar intent url: $url", it)
            }
            return true
        }

        Log.d("PagamentoCartao", "URL especial bloqueada dentro do checkout: $url")
        mostrarStatus(
            "Mercado Pago tentou abrir outro app. Continue pela tela web.",
            carregando = false
        )
        return true
    }

    private fun confirmarPagamentoCartao(paymentId: String?) {
        if (confirmandoPagamento) return

        if (paymentId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Mercado Pago não retornou o ID do pagamento",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        confirmandoPagamento = true
        mostrarStatus("Confirmando pagamento...", carregando = true)

        functions.getHttpsCallable("confirmarPagamentoCartaoMercadoPago")
            .call(
                mapOf(
                    "contratacaoId" to contratacaoId,
                    "contratacaoOwnerId" to contratacaoOwnerId,
                    "paymentId" to paymentId
                )
            )
            .addOnSuccessListener {
                startActivity(
                    Intent(this, EmAndamentoActivity::class.java)
                        .putExtra("contratacaoId", contratacaoId)
                        .putExtra("contratacaoOwnerId", contratacaoOwnerId)
                )
                finish()
            }
            .addOnFailureListener {
                confirmandoPagamento = false
                mostrarStatus(
                    "Erro ao confirmar pagamento: ${it.message}",
                    carregando = false
                )
            }
    }

    private fun mostrarStatus(mensagem: String, carregando: Boolean) {
        binding.txtStatusCheckout.text = mensagem
        binding.progressoCheckout.visibility = if (carregando) View.VISIBLE else View.GONE
        Log.d("PagamentoCartao", mensagem)
    }

    private fun carregarDadosIntent(intent: Intent?) {
        contratacaoId =
            intent?.getStringExtra("contratacaoId")
                ?: intent?.data?.getQueryParameter("contratacaoId")
                ?: contratacaoId

        contratacaoOwnerId =
            intent?.getStringExtra("contratacaoOwnerId")
                ?: intent?.data?.getQueryParameter("contratacaoOwnerId")
                ?: contratacaoOwnerId

        initPoint =
            intent?.getStringExtra("init_point")
                ?: initPoint
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        carregarDadosIntent(intent)
        Log.d(
            "PagamentoCartao",
            "onNewIntent contratacaoId=$contratacaoId url=${intent?.data}"
        )
        tratarUrlRetorno(intent?.data?.toString())
    }

    override fun onResume() {
        super.onResume()
        Log.d("PagamentoCartao", "onResume")
    }

    override fun onPause() {
        Log.d("PagamentoCartao", "onPause")
        super.onPause()
    }

    override fun onDestroy() {
        Log.d("PagamentoCartao", "onDestroy isFinishing=$isFinishing")
        super.onDestroy()
    }
}
