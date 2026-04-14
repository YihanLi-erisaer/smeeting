package com.stardazz.smeeting.domain.usecase

import com.stardazz.smeeting.domain.model.TranscriptionHistoryEntry
import com.stardazz.smeeting.domain.repository.TranscriptionHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTranscriptionHistoryUseCase @Inject constructor(
    private val repository: TranscriptionHistoryRepository,
) {
    operator fun invoke(): Flow<List<TranscriptionHistoryEntry>> = repository.entries
}
