package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: String, // "Note" or "Task"
    val category: String, // "Work", "Personal", "Urgent", "General"
    val isTaskComplete: Boolean = false,
    val alarmTime: Long? = null, // timestamp in ms for when the alarm should fire
    val isAlarmEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
