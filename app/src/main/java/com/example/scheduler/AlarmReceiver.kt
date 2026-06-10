package com.example.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "diary_alarm_channel"
        const val CHANNEL_NAME = "Diary & Task Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getIntExtra("ENTRY_ID", 0)
        val title = intent.getStringExtra("ENTRY_TITLE") ?: "Task Reminder"
        val content = intent.getStringExtra("ENTRY_CONTENT") ?: "You have an upcoming task!"
        val type = intent.getStringExtra("ENTRY_TYPE") ?: "Task"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled diary tasks and reminders."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action to open MainActivity when notification is clicked
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            entryId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        notificationManager.notify(entryId, builder.build())
    }
}
