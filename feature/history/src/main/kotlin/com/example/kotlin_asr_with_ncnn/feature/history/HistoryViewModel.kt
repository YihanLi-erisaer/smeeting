package com.example.kotlin_asr_with_ncnn.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_asr_with_ncnn.domain.model.TranscriptionHistoryEntry
import com.example.kotlin_asr_with_ncnn.domain.usecase.DeleteTranscriptionHistoryEntryUseCase
import com.example.kotlin_asr_with_ncnn.domain.usecase.ObserveTranscriptionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeTranscriptionHistoryUseCase: ObserveTranscriptionHistoryUseCase,
    private val deleteTranscriptionHistoryEntryUseCase: DeleteTranscriptionHistoryEntryUseCase,
) : ViewModel() {

    val entries: StateFlow<List<TranscriptionHistoryEntry>> =
        observeTranscriptionHistoryUseCase().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            deleteTranscriptionHistoryEntryUseCase(id)
        }
    }
}
