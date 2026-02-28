package com.example.kotlin_asr_with_ncnn.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_asr_with_ncnn.domain.model.Transcription
import com.example.kotlin_asr_with_ncnn.domain.usecase.StartASRUseCase
import com.example.kotlin_asr_with_ncnn.domain.usecase.StopASRUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ASRViewModel @Inject constructor(
    private val startASRUseCase: StartASRUseCase,
    private val stopASRUseCase: StopASRUseCase
) : ViewModel() {

    private val _transcriptionState = MutableStateFlow<Transcription?>(null)
    val transcriptionState: StateFlow<Transcription?> = _transcriptionState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var collectionJob: Job? = null

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        collectionJob?.cancel()
        _isListening.value = true
        collectionJob = viewModelScope.launch {
            startASRUseCase().collect { transcription ->
                _transcriptionState.value = transcription
            }
        }
    }

    private fun stopListening() {
        viewModelScope.launch {
            stopASRUseCase()
            _isListening.value = false
            // Note: We don't cancel the job here so we can receive the final processing result
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectionJob?.cancel()
    }
}
