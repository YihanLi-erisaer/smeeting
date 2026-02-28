package com.example.kotlin_asr_with_ncnn.data.repository

import android.util.Log
import com.example.kotlin_asr_with_ncnn.core.media.AudioRecorder
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.domain.model.Transcription
import com.example.kotlin_asr_with_ncnn.domain.repository.ASRRepository
import com.example.kotlin_asr_with_ncnn.domain.repository.EngineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ASRRepositoryImpl @Inject constructor(
    private val nativeBridge: NcnnNativeBridge,
    private val audioRecorder: AudioRecorder
) : ASRRepository {

    private val TAG = "ASRRepositoryImpl"
    
    private val audioChunks = Collections.synchronizedList(mutableListOf<ShortArray>())

    private val _transcriptionFlow = MutableSharedFlow<Transcription>(
        replay = 1,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var lastTranscription: Transcription? = null

    init {
        nativeBridge.setCallback { text, confidence, isFinal ->
            Log.d(TAG, "Native Callback: '$text' (isFinal: $isFinal)")
            
            // Only update if the new text is not a generic "Finished" message, 
            // OR if it's the first result we've ever gotten.
            val isStatusMessage = text.contains("ASR Session Finished") || text.contains("Transcribe Complete")
            
            if (!isStatusMessage || lastTranscription == null) {
                val newTranscription = Transcription(
                    id = System.currentTimeMillis().toString(),
                    text = text,
                    confidence = confidence,
                    timestamp = System.currentTimeMillis(),
                    isFinal = isFinal
                )
                lastTranscription = newTranscription
                _transcriptionFlow.tryEmit(newTranscription)
            } else {
                Log.d(TAG, "Skipping status message to preserve transcription text")
            }
        }
    }

    override fun startListening(): Flow<Transcription> {
        Log.d(TAG, "startListening: Buffering audio")
        audioChunks.clear()
        lastTranscription = null
        
        _transcriptionFlow.tryEmit(Transcription("0", "Recording...", 0f, 0L, false))

        audioRecorder.start(object : AudioRecorder.AudioDataListener {
            override fun onAudioData(data: ShortArray) {
                audioChunks.add(data)
            }
        })
        return _transcriptionFlow.asSharedFlow()
    }

    override suspend fun stopListening() {
        Log.d(TAG, "stopListening: Recorder stopped, starting model inference")
        audioRecorder.stop()

        val totalSize = synchronized(audioChunks) { audioChunks.sumOf { it.size } }
        
        if (totalSize > 0) {
            val fullAudio = ShortArray(totalSize)
            var offset = 0
            synchronized(audioChunks) {
                for (chunk in audioChunks) {
                    System.arraycopy(chunk, 0, fullAudio, offset, chunk.size)
                    offset += chunk.size
                }
            }

            withContext(Dispatchers.Default) {
                _transcriptionFlow.tryEmit(Transcription("proc", "Processing recording...", 0f, 0L, false))
                
                nativeBridge.stopInference()
                nativeBridge.startInference()
                nativeBridge.feedAudioData(fullAudio)
                nativeBridge.signalInputFinished()
                nativeBridge.stopInference()
                
                Log.d(TAG, "Inference completed for $totalSize samples")
            }
        } else {
            Log.w(TAG, "No audio data was captured")
            _transcriptionFlow.tryEmit(Transcription("err", "Error: No audio captured", 0f, 0L, true))
        }
    }

    override fun getEngineStatus(): EngineStatus {
        return when(nativeBridge.getStatus()) {
            0 -> EngineStatus.IDLE
            1 -> EngineStatus.INITIALIZING
            2 -> EngineStatus.LISTENING
            else -> EngineStatus.ERROR
        }
    }
}
