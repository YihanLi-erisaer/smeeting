package com.stardazz.smeeting.core.startup

import android.content.Context
import android.util.Log
import com.stardazz.smeeting.core.llm.LlamaCppBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LlmModelState {
    data object NotDownloaded : LlmModelState
    data class Downloading(val progress: Float) : LlmModelState
    data object Downloaded : LlmModelState
    data object Loading : LlmModelState
    data object Ready : LlmModelState
    data class Error(val message: String) : LlmModelState
}

@Singleton
class LlmModelManager @Inject constructor(
    private val bridge: LlamaCppBridge,
) {
    private val mutex = Mutex()

    private val _state = MutableStateFlow<LlmModelState>(LlmModelState.NotDownloaded)
    val state: StateFlow<LlmModelState> = _state.asStateFlow()

    fun getModelFile(context: Context): File =
        File(context.filesDir, MODEL_FILENAME)

    fun isModelDownloaded(context: Context): Boolean =
        getModelFile(context).let { it.exists() && it.length() > MIN_MODEL_SIZE }

    suspend fun downloadModel(context: Context) {
        mutex.withLock {
            if (_state.value is LlmModelState.Downloading) return
            downloadInternal(context)
        }
    }

    private suspend fun downloadInternal(context: Context) {
        val file = getModelFile(context)
        if (file.exists() && file.length() > MIN_MODEL_SIZE) {
            _state.value = LlmModelState.Downloaded
            return
        }

        _state.value = LlmModelState.Downloading(0f)

        try {
            withContext(Dispatchers.IO) {
                val tmpFile = File(file.parentFile, "${MODEL_FILENAME}.tmp")
                val url = URL(MODEL_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout = 30_000

                if (tmpFile.exists() && tmpFile.length() > 0) {
                    conn.setRequestProperty("Range", "bytes=${tmpFile.length()}-")
                }

                conn.connect()
                val responseCode = conn.responseCode

                val totalSize: Long
                val append: Boolean
                if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    val contentRange = conn.getHeaderField("Content-Range")
                    totalSize = contentRange?.substringAfter("/")?.toLongOrNull()
                        ?: (tmpFile.length() + conn.contentLengthLong)
                    append = true
                } else {
                    totalSize = conn.contentLengthLong
                    append = false
                    if (tmpFile.exists()) tmpFile.delete()
                }

                conn.inputStream.use { input ->
                    FileOutputStream(tmpFile, append).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var downloaded = if (append) tmpFile.length() else 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalSize > 0) {
                                _state.value = LlmModelState.Downloading(
                                    (downloaded.toFloat() / totalSize).coerceIn(0f, 1f)
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
                tmpFile.renameTo(file)
            }
            _state.value = LlmModelState.Downloaded
            Log.i(TAG, "Model downloaded: ${file.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _state.value = LlmModelState.Error("Download failed: ${e.message}")
        }
    }

    suspend fun loadModel(context: Context, nThreads: Int = 4) {
        mutex.withLock {
            loadInternal(context, nThreads)
        }
    }

    private suspend fun loadInternal(context: Context, nThreads: Int) {
        val file = getModelFile(context)
        if (!file.exists() || file.length() < MIN_MODEL_SIZE) {
            _state.value = LlmModelState.NotDownloaded
            return
        }

        _state.value = LlmModelState.Loading
        try {
            val success = bridge.loadModel(file.absolutePath, nThreads)
            _state.value = if (success) LlmModelState.Ready
            else LlmModelState.Error("Failed to load model")
        } catch (e: Exception) {
            Log.e(TAG, "Load failed", e)
            _state.value = LlmModelState.Error("Load failed: ${e.message}")
        }
    }

    /**
     * Aborts any ongoing generation, releases native weights, and keeps the GGUF on disk if present.
     * State becomes [LlmModelState.Downloaded] or [LlmModelState.NotDownloaded] accordingly.
     */
    suspend fun unloadModel(context: Context) {
        mutex.withLock {
            bridge.abort()
            bridge.releaseModel()
            _state.value = if (isModelDownloaded(context)) LlmModelState.Downloaded
            else LlmModelState.NotDownloaded
        }
    }

    /**
     * Unloads the model and deletes the downloaded GGUF (and incomplete `.tmp`) from app storage.
     */
    suspend fun deleteDownloadedModel(context: Context) {
        mutex.withLock {
            bridge.abort()
            bridge.releaseModel()
            val file = getModelFile(context)
            val tmpFile = File(file.parentFile, "${MODEL_FILENAME}.tmp")
            try {
                if (tmpFile.exists()) tmpFile.delete()
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Delete model files failed", e)
            }
            _state.value = LlmModelState.NotDownloaded
        }
    }

    companion object {
        private const val TAG = "LlmModelManager"
        private const val MODEL_FILENAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        private const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        private const val MIN_MODEL_SIZE = 100_000_000L // 100MB sanity check (reject incomplete downloads)
    }
}
