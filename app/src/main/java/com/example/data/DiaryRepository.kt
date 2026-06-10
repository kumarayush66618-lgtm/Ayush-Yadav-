package com.example.data

import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {
    val allEntries: Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    suspend fun getEntryById(id: Int): DiaryEntry? {
        return diaryDao.getEntryById(id)
    }

    suspend fun insertEntry(entry: DiaryEntry): Long {
        return diaryDao.insertEntry(entry)
    }

    suspend fun deleteEntryById(id: Int) {
        diaryDao.deleteEntryById(id)
    }
}
