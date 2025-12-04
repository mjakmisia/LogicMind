package com.example.logicmind.additional

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showNotification(
            context, "Streak!",
            "Nie zapomnij o swoim streaku! Zagraj teraz aby nie stracić postępów!"
        )
    }
}