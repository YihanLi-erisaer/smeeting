package com.stardazz.smeeting.data.repository

import android.util.Log
import com.stardazz.smeeting.core.common.InferenceCoordinator
import com.stardazz.smeeting.core.llm.NcnnLlmBridge
import com.stardazz.smeeting.domain.repository.LLMRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMRepositoryImpl @Inject constructor(
    private val bridge: NcnnLlmBridge,
    private val coordinator: InferenceCoordinator,
) : LLMRepository {

    /**
     * Serializes summarize runs so a second call cannot start [bridge.generate] until the previous
     * run has finished [coordinator.release]. Otherwise [InferenceCoordinator.acquireLlm] allows
     * re-entrant LLM while the slot is still held, and the second [NcnnLlmBridge.generate] hits
     * "Generation already in progress" and returns "" (Re-summarize looks stuck then exits).
     */
    private val summarizeMutex = Mutex()

    override val isModelReady: StateFlow<Boolean> = bridge.isLoaded

    override fun summarize(text: String): Flow<String> = channelFlow {
        summarizeMutex.withLock {
            if (!coordinator.acquireLlm()) {
                Log.w(TAG, "Cannot start LLM: ASR is currently active")
                return@withLock
            }

            try {
                val prompt = buildPrompt(text)
                var accumulated = ""

                val collector = launch {
                    bridge.tokenFlow().collect { token ->
                        accumulated += token
                        send(accumulated)
                    }
                }

                bridge.generate(prompt, maxTokens = MAX_TOKENS, nThreads = N_THREADS)

                collector.cancel()
                if (accumulated.isNotEmpty()) {
                    send(accumulated)
                }
            } finally {
                coordinator.release()
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun abortGeneration() {
        bridge.abort()
    }

    override suspend fun awaitGenerationIdle() {
        bridge.isGenerating.first { !it }
    }

    private fun buildPrompt(transcriptionText: String): String {
        val trimmed = if (transcriptionText.length > MAX_INPUT_CHARS) {
            transcriptionText.take(MAX_INPUT_CHARS)
        } else {
            transcriptionText
        }
        return "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n<|im_start|>user\n$trimmed<|im_end|>\n<|im_start|>assistant\n"
    }

    companion object {
        private const val TAG = "LLMRepositoryImpl"
        private const val MAX_TOKENS = 512
        private const val N_THREADS = 4
        private const val MAX_INPUT_CHARS = 4096

        private const val SYSTEM_PROMPT =
            "You are a meeting transcription assistant. " +
            "Given a transcription, output a concise summary in the SAME LANGUAGE as the transcription. " +
            "Format:\n" +
            "Summary:\n" +
            "2-3 SENTENCES ONLY!\n\n" +
            "Key Points:\n" +
            "- bullet points ONLY ONE SENTENCE PER POINT! \n\n" +
            "Action Items:\n" +
            "- if any write 2-3 SENTENCES ONLY, otherwise write \"None\""
    }
}
