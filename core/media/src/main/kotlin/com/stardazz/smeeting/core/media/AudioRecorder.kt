package com.stardazz.smeeting.core.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor() {
    private val TAG = "AudioRecorder"
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null

    interface AudioDataListener {
        fun onAudioData(data: ShortArray)
    }

    private var listener: AudioDataListener? = null

    fun setListener(l: AudioDataListener) {
        this.listener = l
    }

    @SuppressLint("MissingPermission")
    fun start(listener: AudioDataListener) {
        if (isRecording.get()) return
        this.listener = listener

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            return
        }

        audioRecord?.startRecording()
        isRecording.set(true)

        recordingThread = Thread({
            val buffer = ShortArray(BUFFER_SIZE)
            while (isRecording.get()) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    val data = buffer.copyOf(readSize)
                    this.listener?.onAudioData(data)
                }
            }
        }, "AudioRecordingThread")
        recordingThread?.start()
    }

    fun stop() {
        isRecording.set(false)
        try {
            recordingThread?.join()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording thread", e)
        }
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }
}
