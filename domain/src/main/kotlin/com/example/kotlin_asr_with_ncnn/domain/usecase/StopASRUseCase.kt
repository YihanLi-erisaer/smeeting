package com.example.kotlin_asr_with_ncnn.domain.usecase

import com.example.kotlin_asr_with_ncnn.domain.repository.ASRRepository
import javax.inject.Inject

class StopASRUseCase @Inject constructor(private val repository: ASRRepository) {
    suspend operator fun invoke() {
        repository.stopListening()
    }
}