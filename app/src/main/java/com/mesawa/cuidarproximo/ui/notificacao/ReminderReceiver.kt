package com.mesawa.cuidarproximo.ui.notificacao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val titulo =
            intent.getStringExtra("titulo")
                ?: "Lembrete"

        val descricao =
            intent.getStringExtra("descricao")
                ?: "Hora da tarefa"

        NotificationHelper(context)
            .mostrarNotificacao(
                titulo,
                descricao
            )
    }
}