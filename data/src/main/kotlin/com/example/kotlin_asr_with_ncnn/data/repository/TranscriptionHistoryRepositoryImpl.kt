package com.example.kotlin_asr_with_ncnn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kotlin_asr_with_ncnn.domain.model.TranscriptionHistoryEntry
import com.example.kotlin_asr_with_ncnn.domain.repository.TranscriptionHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.transcriptionHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "transcription_history",
)

private val KEY_ENTRIES_JSON = stringPreferencesKey("entries_json")

private const val MAX_ENTRIES = 500

@Singleton
class TranscriptionHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TranscriptionHistoryRepository {

    override val entries: Flow<List<TranscriptionHistoryEntry>> =
        context.transcriptionHistoryDataStore.data.map { prefs ->
            parseEntriesJson(prefs[KEY_ENTRIES_JSON] ?: "[]")
        }

    override suspend fun append(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        context.transcriptionHistoryDataStore.edit { prefs ->
            val current = parseEntriesJson(prefs[KEY_ENTRIES_JSON] ?: "[]")
            val entry = TranscriptionHistoryEntry(
                id = UUID.randomUUID().toString(),
                text = trimmed,
                createdAtMillis = System.currentTimeMillis(),
            )
            val merged = listOf(entry) + current
            prefs[KEY_ENTRIES_JSON] = entriesToJson(merged.take(MAX_ENTRIES))
        }
    }

    override suspend fun remove(id: String) {
        context.transcriptionHistoryDataStore.edit { prefs ->
            val current = parseEntriesJson(prefs[KEY_ENTRIES_JSON] ?: "[]")
            prefs[KEY_ENTRIES_JSON] = entriesToJson(current.filter { it.id != id })
        }
    }
}

private fun parseEntriesJson(raw: String): List<TranscriptionHistoryEntry> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    TranscriptionHistoryEntry(
                        id = o.getString("id"),
                        text = o.optString("text", ""),
                        createdAtMillis = o.optLong("createdAtMillis", 0L),
                    ),
                )
            }
        }
    }.getOrElse { emptyList() }
}

private fun entriesToJson(list: List<TranscriptionHistoryEntry>): String {
    val arr = JSONArray()
    for (e in list) {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("text", e.text)
        o.put("createdAtMillis", e.createdAtMillis)
        arr.put(o)
    }
    return arr.toString()
}
