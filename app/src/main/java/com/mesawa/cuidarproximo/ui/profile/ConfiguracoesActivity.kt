package com.mesawa.cuidarproximo.ui.profile

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.ScrollView
import android.widget.Switch
import com.mesawa.cuidarproximo.BaseActivity

class ConfiguracoesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = screenRoot(this)
        root.addView(title("Configuracoes"))
        root.addView(subtitle("Controle notificacoes, privacidade e preferencias do aplicativo."))

        val card = sectionCard(this)
        val content = cardContent(this)
        content.addView(switchRow("Notificacoes de agenda", true))
        content.addView(switchRow("Alertas de pagamento", true))
        content.addView(switchRow("Compartilhar localizacao no acompanhamento", true))
        content.addView(switchRow("Receber novidades do app", false))
        card.addView(content)
        root.addView(card)

        val seguranca = sectionCard(this, "#F9FAFB")
        val secContent = cardContent(this)
        secContent.addView(value("Seguranca", "#111827", 18f))
        secContent.addView(value("Permissoes sensiveis devem ser usadas apenas para agenda, pagamentos e acompanhamento autorizado.", "#4B5563", 14f))
        seguranca.addView(secContent)
        root.addView(seguranca)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun switchRow(text: String, checked: Boolean): CompoundButton =
        Switch(this).apply {
            this.text = text
            textSize = 16f
            isChecked = checked
            setPadding(0, dp(8), 0, dp(8))
        }
}
