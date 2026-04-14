package com.stardazz.smeeting.core.startup

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ModelInitPipelineEvent {
    data class Finished(val success: Boolean, val error: String?) : ModelInitPipelineEvent
    data object SkippedAwaitingPermission : ModelInitPipelineEvent
}

@Singleton
class ModelInitNotifier @Inject constructor() {
    private val _events =
        MutableSharedFlow<ModelInitPipelineEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ModelInitPipelineEvent> = _events.asSharedFlow()

    fun emitFinished(success: Boolean, error: String?) {
        _events.tryEmit(ModelInitPipelineEvent.Finished(success, error))
    }

    fun emitSkippedAwaitingPermission() {
        _events.tryEmit(ModelInitPipelineEvent.SkippedAwaitingPermission)
    }
}
