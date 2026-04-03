package com.example.kotlin_asr_with_ncnn

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Tracks whether the ASR model has finished loading (off main thread). */
sealed class ModelInitState {
    data object Loading : ModelInitState()
    data object Ready : ModelInitState()
    data class Error(val message: String) : ModelInitState()
}

@HiltViewModel
class MainUiViewModel @Inject constructor(
    
): ViewModel() {
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()


    private val _modelInitState = MutableStateFlow<ModelInitState>(ModelInitState.Loading)
    val modelInitState: StateFlow<ModelInitState> = _modelInitState.asStateFlow()

    fun setModelInitResult(success: Boolean, errorMessage: String? = null) {
        _modelInitState.value = if (success) ModelInitState.Ready else ModelInitState.Error(
            errorMessage ?: "Model failed to load"
        )
    }

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun openHistory() {
        _showHistory.value = true
    }

    fun closeHistory() {
        _showHistory.value = false
    }
}
