package com.example.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object DiaryAlarmScheduler {
    private const val TAG = "DiaryAlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(
        context: Context,
        entryId: Int,
        title: String,
        content: String,
        type: String,
        timeMs: Long
    ) {
        if (timeMs <= System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to schedule alarm in the past or now. Skipped.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ENTRY_ID", entryId)
            putExtra("ENTRY_TITLE", title)
            putExtra("ENTRY_CONTENT", content)
            putExtra("ENTRY_TYPE", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Allows alarm to fire even when device is in low-power idle doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm successfully for entry $entryId at $timeMs")
        } catch (e: SecurityException) {
            // Under Android 14+ exact alarms require explicit user settings or special policies,
            // fallback gracefully to setAndAllowWhileIdle which is highly reliable without permission
            Log.w(TAG, "Exact alarm permission missing. Falling back to inexact alarm.", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeMs,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(context: Context, entryId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm successfully for entry $entryId")
        }
    }
}
