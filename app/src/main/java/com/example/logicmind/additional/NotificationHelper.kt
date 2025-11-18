package com.example.logicmind.additional

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.logicmind.R
import com.example.logicmind.activities.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "streak_reminder_channel"
    private const val CHANNEL_NAME = "Codzienne przypomnienie"
    private const val NOTIFICATION_ID = 1

    /** Tworzy kanał powiadomień */
    fun createNotificationChannel(context: Context) {
        val importance =
            NotificationManager.IMPORTANCE_DEFAULT //Dźwięk, wibracje, pojawia się w pasku stanu
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Kanał dla codziennych przypomnień o streaku."
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /** Wyświetla powiadomienia push */
    fun showNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context) //kanał istnieje

        //uruchamia aplikacje po kliknięciu
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())


    }
}