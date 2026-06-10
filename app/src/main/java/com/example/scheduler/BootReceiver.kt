package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.DiaryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device booted. Rescheduling alarms...")
            
            // Hold the receiver alive during background database query
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DiaryDatabase.getDatabase(context)
                    val dao = db.diaryDao()
                    
                    // Retrieve all elements from Flow (first emission containing the currently saved database list)
                    val entries = dao.getAllEntries().first()
                    val currentTime = System.currentTimeMillis()
                    
                    var rescheduledCount = 0
                    for (entry in entries) {
                        if (entry.isAlarmEnabled && entry.alarmTime != null && entry.alarmTime > currentTime) {
                            DiaryAlarmScheduler.scheduleAlarm(
                                context = context,
                                entryId = entry.id,
                                title = entry.title,
                                content = entry.content,
                                type = entry.type,
                                timeMs = entry.alarmTime
                            )
                            rescheduledCount++
                        }
                    }
                    Log.d(TAG, "Successfully rescheduled $rescheduledCount alarms.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                } finally {
                    // Tell Android OS that this receiver has completed its work
                    pendingResult.finish()
                }
            }
        }
    }
}
