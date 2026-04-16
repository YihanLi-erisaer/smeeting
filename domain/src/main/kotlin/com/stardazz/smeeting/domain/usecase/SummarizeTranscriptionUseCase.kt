package com.stardazz.smeeting.domain.usecase

import com.stardazz.smeeting.domain.repository.LLMRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SummarizeTranscriptionUseCase @Inject constructor(
    private val llmRepository: LLMRepository,
) {
    operator fun invoke(transcriptionText: String): Flow<String> =
        llmRepository.summarize(transcriptionText)
}
