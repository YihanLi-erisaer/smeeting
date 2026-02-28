package com.example.kotlin_asr_with_ncnn.presentation.asr

import com.example.kotlin_asr_with_ncnn.domain.model.Transcription
import com.example.kotlin_asr_with_ncnn.domain.repository.EngineStatus

class ASRContract {

    /**
     * UI State: defines all data shown on the screen
     */
    data class State(
        val status: EngineStatus = EngineStatus.IDLE,
        val currentTranscription: Transcription? = null,
        val isError: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * UI Event/Intent: defines actions the user can perform
     */
    sealed class Event {
        object ToggleListening : Event()
        object StartListening : Event()
        object StopListening : Event()
        data class ErrorOccurred(val message: String) : Event()
    }

    /**
     * UI Side Effect: defines one-time actions (like toasts, navigation)
     */
    sealed class Effect {
        data class ShowToast(val message: String) : Effect()
    }
}