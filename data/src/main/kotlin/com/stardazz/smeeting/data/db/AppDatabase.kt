package com.stardazz.smeeting.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptionHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptionHistoryDao(): TranscriptionHistoryDao
}
