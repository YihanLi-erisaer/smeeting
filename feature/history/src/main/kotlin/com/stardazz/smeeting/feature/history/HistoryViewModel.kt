package com.stardazz.smeeting.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stardazz.smeeting.core.startup.LlmModelManager
import com.stardazz.smeeting.core.startup.LlmModelState
import com.stardazz.smeeting.domain.model.TranscriptionHistoryEntry
import com.stardazz.smeeting.domain.usecase.DeleteTranscriptionHistoryEntryUseCase
import com.stardazz.smeeting.domain.usecase.ObserveTranscriptionHistoryUseCase
import com.stardazz.smeeting.domain.usecase.SummarizeTranscriptionUseCase
import com.stardazz.smeeting.domain.usecase.UpdateHistorySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeTranscriptionHistoryUseCase: ObserveTranscriptionHistoryUseCase,
    private val deleteTranscriptionHistoryEntryUseCase: DeleteTranscriptionHistoryEntryUseCase,
    private val summarizeTranscriptionUseCase: SummarizeTranscriptionUseCase,
    private val updateHistorySummaryUseCase: UpdateHistorySummaryUseCase,
    private val llmModelManager: LlmModelManager,
) : ViewModel() {

    val entries: StateFlow<List<TranscriptionHistoryEntry>> =
        observeTranscriptionHistoryUseCase().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val llmState: StateFlow<LlmModelState> = llmModelManager.state

    private val _summarizingEntryId = MutableStateFlow<String?>(null)
    val summarizingEntryId: StateFlow<String?> = _summarizingEntryId.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private var summarizeJob: Job? = null

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            deleteTranscriptionHistoryEntryUseCase(id)
        }
    }

    fun summarize(entry: TranscriptionHistoryEntry) {
        if (summarizeJob?.isActive == true) return
        _summarizingEntryId.value = entry.id
        _streamingText.value = ""

        summarizeJob = viewModelScope.launch {
            try {
                summarizeTranscriptionUseCase(entry.text).collect { accumulated ->
                    _streamingText.value = accumulated
                }
                val finalText = _streamingText.value
                if (finalText.isNotEmpty()) {
                    updateHistorySummaryUseCase(entry.id, finalText)
                }
            } finally {
                _summarizingEntryId.value = null
            }
        }
    }

    fun cancelSummarize() {
        summarizeJob?.cancel()
        _summarizingEntryId.value = null
        _streamingText.value = ""
    }

    fun downloadLlmModel(context: android.content.Context) {
        viewModelScope.launch {
            llmModelManager.downloadModel(context)
            if (llmModelManager.state.value is LlmModelState.Downloaded) {
                llmModelManager.loadModel(context, nThreads = 4)
            }
        }
    }

    fun deleteLlmModelFiles(context: android.content.Context) {
        viewModelScope.launch {
            cancelSummarize()
            llmModelManager.deleteDownloadedModel(context)
        }
    }
}
