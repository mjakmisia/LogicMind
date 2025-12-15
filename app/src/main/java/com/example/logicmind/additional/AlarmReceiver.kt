package com.example.logicmind.additional

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.logicmind.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showNotification(
            context,
            context.getString(R.string.notification_title),
            context.getString(R.string.notification_message)
        )
    }
}