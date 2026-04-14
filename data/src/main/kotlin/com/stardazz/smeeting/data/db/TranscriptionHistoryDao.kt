package com.stardazz.smeeting.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionHistoryDao {

    @Query("SELECT * FROM transcription_history ORDER BY created_at_millis DESC")
    fun observeAll(): Flow<List<TranscriptionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranscriptionHistoryEntity)

    @Query("DELETE FROM transcription_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM transcription_history")
    suspend fun count(): Int

    /** Remove the N oldest rows so the table stays within a size cap. */
    @Query(
        """
        DELETE FROM transcription_history
        WHERE id IN (
            SELECT id FROM transcription_history
            ORDER BY created_at_millis ASC
            LIMIT :count
        )
        """
    )
    suspend fun deleteOldest(count: Int)
}
