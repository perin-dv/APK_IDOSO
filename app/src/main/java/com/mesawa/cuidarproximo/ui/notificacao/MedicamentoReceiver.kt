package com.mesawa.cuidarproximo.ui.notificacao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicamentoReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val titulo =
            intent.getStringExtra("titulo") ?: ""

        NotificationHelper(context)
            .mostrarNotificacao(
                "💊 Hora do medicamento",
                titulo
            )
    }
}