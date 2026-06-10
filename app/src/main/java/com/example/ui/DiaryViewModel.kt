package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DiaryDatabase
import com.example.data.DiaryEntry
import com.example.data.DiaryRepository
import com.example.scheduler.DiaryAlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DiaryRepository
    
    // Core database flows
    val allEntries: StateFlow<List<DiaryEntry>>
    
    // UI state flows
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val database = DiaryDatabase.getDatabase(application)
        repository = DiaryRepository(database.diaryDao())
        
        allEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Reactive combinations
    val filteredEntries: StateFlow<List<DiaryEntry>> = combine(
        allEntries, _selectedCategory, _searchQuery
    ) { entries, category, query ->
        entries.filter { entry ->
            val matchesCategory = category == "All" || entry.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isEmpty() || 
                    entry.title.contains(query, ignoreCase = true) || 
                    entry.content.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks: StateFlow<List<DiaryEntry>> = allEntries.map { entries ->
        entries.filter { 
            it.type == "Task" && !it.isTaskComplete && (it.alarmTime ?: 0L) > System.currentTimeMillis()
        }.sortedBy { it.alarmTime ?: Long.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<DiaryEntry>> = allEntries.map { entries ->
        entries.filter { it.type == "Note" }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveEntry(
        id: Int = 0,
        title: String,
        content: String,
        type: String,
        category: String,
        alarmTime: Long?,
        isAlarmEnabled: Boolean
    ) {
        viewModelScope.launch {
            val entry = DiaryEntry(
                id = id,
                title = title,
                content = content,
                type = type,
                category = category,
                alarmTime = alarmTime,
                isAlarmEnabled = isAlarmEnabled,
                createdAt = if (id == 0) System.currentTimeMillis() else {
                    repository.getEntryById(id)?.createdAt ?: System.currentTimeMillis()
                }
            )

            val newId = repository.insertEntry(entry).toInt()
            val finalId = if (id == 0) newId else id

            // Sync with alarm scheduler
            val context = getApplication<Application>().applicationContext
            if (isAlarmEnabled && alarmTime != null && alarmTime > System.currentTimeMillis()) {
                DiaryAlarmScheduler.scheduleAlarm(
                    context = context,
                    entryId = finalId,
                    title = title,
                    content = content, // Notification message body
                    type = type,
                    timeMs = alarmTime
                )
            } else {
                DiaryAlarmScheduler.cancelAlarm(context, finalId)
            }
        }
    }

    fun toggleTaskCompletion(entry: DiaryEntry) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(
                isTaskComplete = !entry.isTaskComplete,
                // If completed, disable active alarms to avoid annoying notifications for completed tasks!
                isAlarmEnabled = if (!entry.isTaskComplete) false else entry.isAlarmEnabled
            )
            repository.insertEntry(updatedEntry)

            val context = getApplication<Application>().applicationContext
            if (updatedEntry.isTaskComplete) {
                DiaryAlarmScheduler.cancelAlarm(context, updatedEntry.id)
            } else if (updatedEntry.isAlarmEnabled && updatedEntry.alarmTime != null && updatedEntry.alarmTime > System.currentTimeMillis()) {
                DiaryAlarmScheduler.scheduleAlarm(
                    context = context,
                    entryId = updatedEntry.id,
                    title = updatedEntry.title,
                    content = updatedEntry.content,
                    type = updatedEntry.type,
                    timeMs = updatedEntry.alarmTime
                )
            }
        }
    }

    fun deleteEntry(entryId: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            DiaryAlarmScheduler.cancelAlarm(context, entryId)
            repository.deleteEntryById(entryId)
        }
    }
}
