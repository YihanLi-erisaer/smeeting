package com.example.kotlin_asr_with_ncnn

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Tracks whether the ASR model has finished loading (off main thread). */
sealed class ModelInitState {
    data object Loading : ModelInitState()
    data object Ready : ModelInitState()
    data class Error(val message: String) : ModelInitState()
}

enum class MainScreen {
    Home,
    Settings,
    History,
}

@HiltViewModel
class MainUiViewModel @Inject constructor(
    
): ViewModel() {
    private val _currentScreen = MutableStateFlow(MainScreen.Home)
    val currentScreen: StateFlow<MainScreen> = _currentScreen.asStateFlow()

    private val _modelInitState = MutableStateFlow<ModelInitState>(ModelInitState.Loading)
    val modelInitState: StateFlow<ModelInitState> = _modelInitState.asStateFlow()

    fun setModelInitResult(success: Boolean, errorMessage: String? = null) {
        _modelInitState.value = if (success) ModelInitState.Ready else ModelInitState.Error(
            errorMessage ?: "Model failed to load"
        )
    }

    fun openSettings() {
        _currentScreen.value = MainScreen.Settings
    }

    fun closeSettings() {
        onBack()
    }

    fun openHistory() {
        _currentScreen.value = MainScreen.History
    }

    fun closeHistory() {
        onBack()
    }

    fun onBack() {
        _currentScreen.update { screen ->
            if (screen == MainScreen.Home) screen else MainScreen.Home
        }
    }
}
