package com.stardazz.smeeting.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stardazz.smeeting.domain.model.TranscriptionHistoryEntry

@Entity(tableName = "transcription_history")
data class TranscriptionHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
)

fun TranscriptionHistoryEntity.toDomain() = TranscriptionHistoryEntry(
    id = id,
    text = text,
    createdAtMillis = createdAtMillis,
)
