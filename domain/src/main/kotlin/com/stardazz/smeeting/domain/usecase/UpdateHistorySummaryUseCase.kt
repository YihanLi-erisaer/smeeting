package com.stardazz.smeeting.domain.usecase

import com.stardazz.smeeting.domain.repository.TranscriptionHistoryRepository
import javax.inject.Inject

class UpdateHistorySummaryUseCase @Inject constructor(
    private val repository: TranscriptionHistoryRepository,
) {
    suspend operator fun invoke(id: String, summary: String) {
        repository.updateSummary(id, summary)
    }
}
