package com.mesawa.cuidarproximo.ui.profile

import android.os.Bundle
import android.widget.ScrollView
import com.mesawa.cuidarproximo.BaseActivity

class PremiumActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = screenRoot(this)
        root.addView(title("Premium"))
        root.addView(subtitle("Novos recursos estao sendo preparados para familias e cuidadores."))

        val card = sectionCard(this, "#EEF2FF")
        val content = cardContent(this)
        content.addView(value("Em breve", "#312E81", 24f))
        content.addView(value("Teremos IA de cuidado, alertas inteligentes, relatorios de acompanhamento, historico expandido e prioridade no suporte.", "#374151", 16f))
        card.addView(content)
        root.addView(card)

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
