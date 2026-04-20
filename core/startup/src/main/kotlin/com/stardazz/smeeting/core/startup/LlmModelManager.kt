package com.stardazz.smeeting.core.startup

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.stardazz.smeeting.core.llm.LlmNcnnDevicePolicy
import com.stardazz.smeeting.core.llm.NcnnLlmBridge
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
import java.util.concurrent.atomic.AtomicBoolean
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
    private val bridge: NcnnLlmBridge,
) {
    private val mutex = Mutex()

    /** True while JNI is inside ncnn model load. Not held under [mutex] — see [loadInternal]. */
    private val loadNativeInProgress = AtomicBoolean(false)

    private val _state = MutableStateFlow<LlmModelState>(LlmModelState.NotDownloaded)
    val state: StateFlow<LlmModelState> = _state.asStateFlow()

    fun getModelDirectory(context: Context): File =
        File(context.filesDir, MODEL_SUBDIR)

    /** @deprecated Prefer [getModelDirectory]; kept for callers expecting a single path. */
    @Deprecated("LLM is a directory of ncnn assets", ReplaceWith("getModelDirectory(context)"))
    fun getModelFile(context: Context): File = getModelDirectory(context)

    fun isModelDownloaded(context: Context): Boolean {
        val dir = getModelDirectory(context)
        if (!dir.isDirectory) return false
        val decoder = File(dir, "qwen3_decoder.ncnn.fp16.bin")
        val embedBin = File(dir, "qwen3_embed_token.ncnn.fp16.bin")
        val modelJson = File(dir, "model.json")
        if (!modelJson.exists() || !decoder.exists() || !embedBin.exists()) return false
        if (decoder.length() < MIN_DECODER_BIN_BYTES) return false
        if (embedBin.length() < MIN_EMBED_BIN_BYTES) return false
        return true
    }

    suspend fun downloadModel(context: Context) {
        mutex.withLock {
            if (_state.value is LlmModelState.Downloading) return
            downloadInternal(context)
        }
    }

    private suspend fun downloadInternal(context: Context) {
        if (loadNativeInProgress.get()) {
            Log.w(TAG, "downloadModel skipped: LLM native load in progress")
            return
        }
        val dir = getModelDirectory(context)
        if (isModelDownloaded(context)) {
            _state.value = LlmModelState.Downloaded
            return
        }

        _state.value = LlmModelState.Downloading(0f)

        try {
            withContext(Dispatchers.IO) {
                if (!dir.exists()) dir.mkdirs()

                var totalExpected = 0L
                for (part in MODEL_PARTS) {
                    val len = probeContentLength(URL(MODEL_BASE + part.remoteName))
                    if (len > 0) totalExpected += len
                }
                if (totalExpected <= 0L) totalExpected = ESTIMATED_TOTAL_BYTES

                var doneBytes = 0L
                for (part in MODEL_PARTS) {
                    val dest = File(dir, part.localName)
                    if (dest.exists() && dest.length() > 0L) {
                        doneBytes += dest.length()
                        _state.value = LlmModelState.Downloading(
                            (doneBytes.toFloat() / totalExpected.toFloat()).coerceIn(0f, 1f),
                        )
                        continue
                    }
                    val tmp = File(dir, "${part.localName}.tmp")
                    if (tmp.exists()) tmp.delete()
                    val url = URL(MODEL_BASE + part.remoteName)
                    downloadOneFile(url, tmp, dest) { receivedInPart ->
                        val overall = doneBytes + receivedInPart
                        _state.value = LlmModelState.Downloading(
                            (overall.toFloat() / totalExpected.toFloat()).coerceIn(0f, 1f),
                        )
                    }
                    doneBytes += dest.length()
                }
            }
            _state.value = LlmModelState.Downloaded
            Log.i(TAG, "ncnn LLM assets downloaded under ${dir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _state.value = LlmModelState.Error("Download failed: ${e.message}")
        }
    }

    private fun probeContentLength(url: URL): Long {
        var conn: HttpURLConnection? = null
        return try {
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000
            conn.connect()
            val n = conn.contentLengthLong
            if (n > 0) n else 0L
        } catch (_: Exception) {
            0L
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadOneFile(
        url: URL,
        tmp: File,
        dest: File,
        onDelta: (Long) -> Unit,
    ) {
        if (dest.exists() && dest.length() > 0) {
            onDelta(dest.length())
            return
        }
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 60_000
        conn.readTimeout = 60_000
        conn.connect()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw IllegalStateException("HTTP ${conn.responseCode} for $url")
        }
        var received = 0L
        conn.inputStream.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    received += read
                    onDelta(received)
                }
            }
        }
        conn.disconnect()
        if (!tmp.renameTo(dest)) {
            throw IllegalStateException("rename ${tmp.name} -> ${dest.name} failed")
        }
    }

    suspend fun loadModel(context: Context, nThreads: Int = DEFAULT_LOAD_THREADS) {
        loadInternal(context, nThreads)
    }

    private suspend fun loadInternal(context: Context, nThreads: Int) {
        val dir = getModelDirectory(context)
        val useVulkan = LlmNcnnDevicePolicy.preferNcnnVulkan()
        mutex.withLock {
            when (_state.value) {
                LlmModelState.Ready -> {
                    Log.d(TAG, "loadModel: already Ready, skipping")
                    return
                }
                LlmModelState.Loading -> {
                    Log.d(TAG, "loadModel: another load in progress, skipping duplicate call")
                    return
                }
                else -> Unit
            }
            if (!isModelDownloaded(context)) {
                _state.value = LlmModelState.NotDownloaded
                return
            }
            _state.value = LlmModelState.Loading
            loadNativeInProgress.set(true)
        }
        try {
            val path = dir.absolutePath
            val t0 = SystemClock.elapsedRealtime()
            Log.i(
                TAG,
                "LLM ncnn load starting: $path threads=$nThreads vulkan=$useVulkan " +
                    "(first load can take several minutes on low-memory devices)",
            )
            val success = bridge.loadModel(path, useVulkan, nThreads, vulkanDeviceIndex = 0)
            val elapsed = SystemClock.elapsedRealtime() - t0
            Log.i(TAG, "LLM ncnn load finished in ${elapsed}ms, success=$success")
            mutex.withLock {
                _state.value = if (success) LlmModelState.Ready
                else LlmModelState.Error("Failed to load model")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load failed", e)
            mutex.withLock {
                _state.value = LlmModelState.Error("Load failed: ${e.message}")
            }
        } finally {
            loadNativeInProgress.set(false)
        }
    }

    suspend fun unloadModel(context: Context) {
        mutex.withLock {
            bridge.abort()
            bridge.releaseModel()
            _state.value = if (isModelDownloaded(context)) LlmModelState.Downloaded
            else LlmModelState.NotDownloaded
        }
    }

    suspend fun deleteDownloadedModel(context: Context) {
        mutex.withLock {
            if (loadNativeInProgress.get()) {
                Log.w(TAG, "deleteDownloadedModel skipped: LLM load in progress")
                return
            }
            bridge.abort()
            bridge.releaseModel()
            val dir = getModelDirectory(context)
            try {
                dir.listFiles()?.forEach { f ->
                    try {
                        f.delete()
                    } catch (_: Exception) {
                    }
                }
                dir.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Delete model directory failed", e)
            }
            _state.value = LlmModelState.NotDownloaded
        }
    }

    private data class ModelPart(val localName: String, val remoteName: String)

    companion object {
        private const val TAG = "LlmModelManager"

        private const val MODEL_SUBDIR = "qwen3_0.6b_ncnn"
        private const val MODEL_BASE = "https://mirrors.sdu.edu.cn/ncnn_modelzoo/qwen3_0.6b/"

        private val MODEL_PARTS = listOf(
            ModelPart("model.json", "model_fp16.json"),
            ModelPart("merges.txt", "merges.txt"),
            ModelPart("vocab.txt", "vocab.txt"),
            ModelPart("qwen3_decoder.ncnn.fp16.param", "qwen3_decoder.ncnn.fp16.param"),
            ModelPart("qwen3_decoder.ncnn.fp16.bin", "qwen3_decoder.ncnn.fp16.bin"),
            ModelPart("qwen3_embed_token.ncnn.fp16.param", "qwen3_embed_token.ncnn.fp16.param"),
            ModelPart("qwen3_embed_token.ncnn.fp16.bin", "qwen3_embed_token.ncnn.fp16.bin"),
            ModelPart("qwen3_proj_out.ncnn.fp16.param", "qwen3_proj_out.ncnn.fp16.param"),
        )

        /** ~1.5GB FP16 bundle; used when HEAD does not return lengths. */
        private const val ESTIMATED_TOTAL_BYTES = 1_600_000_000L

        private const val MIN_DECODER_BIN_BYTES = 500_000_000L
        private const val MIN_EMBED_BIN_BYTES = 50_000_000L

        const val DEFAULT_LOAD_THREADS = 2
    }
}
