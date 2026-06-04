package com.mesawa.cuidarproximo.ui.notificacao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mesawa.cuidarproximo.R

class NotificationHelper(
    private val context: Context
) {

    private val channelId = "agenda_channel"

    init {
        criarCanal()
    }

    private fun criarCanal() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Lembretes Agenda",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description =
                "Canal de lembretes do app"

            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    fun mostrarNotificacao(
        titulo: String,
        descricao: String
    ) {

        val builder = NotificationCompat.Builder(
            context,
            channelId
        )

            .setSmallIcon(R.drawable.ic_notification)

            .setContentTitle(titulo)

            .setContentText(descricao)

            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )

            .setAutoCancel(true)

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}