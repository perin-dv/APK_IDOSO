package com.mesawa.cuidarproximo.ui.home

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.mesawa.cuidarproximo.BaseActivity
import com.mesawa.cuidarproximo.R

class SaudeConteudoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titulo = intent.getStringExtra("titulo") ?: "Conteudo de cuidado"
        val subtitulo = intent.getStringExtra("subtitulo") ?: "Orientacoes para o atendimento"
        val conteudo = intent.getStringExtra("conteudo") ?: "Informacao indisponivel no momento."
        val corEscura = intent.getStringExtra("corEscura") ?: "#0F766E"
        val corClara = intent.getStringExtra("corClara") ?: "#CCFBF1"

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F6F8FB"))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }

        val close = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            setOnClickListener { finish() }
        }

        val hero = CardView(this).apply {
            radius = dp(30).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.parseColor(corClara))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(24), dp(22), dp(26))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor(corClara), Color.WHITE)
            ).apply {
                cornerRadius = dp(30).toFloat()
            }
        }

        val image = ImageView(this).apply {
            setImageResource(R.drawable.ic_elder_address)
            layoutParams = LinearLayout.LayoutParams(dp(112), dp(112))
        }

        val title = TextView(this).apply {
            text = titulo
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }

        val subtitle = TextView(this).apply {
            text = subtitulo
            setTextColor(Color.parseColor(corEscura))
            textSize = 15.5f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        heroContent.addView(image)
        heroContent.addView(title)
        heroContent.addView(subtitle)
        hero.addView(heroContent)

        val body = CardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        val bodyContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(22))
        }

        val label = TextView(this).apply {
            text = "Orientacao para familia e cuidador"
            setTextColor(Color.parseColor(corEscura))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        val text = TextView(this).apply {
            this.text = conteudo
            setTextColor(Color.parseColor("#334155"))
            textSize = 16f
            setLineSpacing(6f, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val footer = TextView(this).apply {
            this.text = "Essas informacoes ajudam no acompanhamento, mas nao substituem avaliacao medica."
            setTextColor(Color.parseColor("#64748B"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }

        bodyContent.addView(label)
        bodyContent.addView(text)
        bodyContent.addView(footer)
        body.addView(bodyContent)

        root.addView(close)
        root.addView(hero)
        root.addView(body)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
