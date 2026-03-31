package com.example.kotlin_asr_with_ncnn.core.startup

import com.example.kotlin_asr_with_ncnn.core.media.ModelConfig
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AsrModelLoad {

    suspend fun load(
        bridge: NcnnNativeBridge,
        assets: android.content.res.AssetManager,
        useBeamSearch: Boolean,
    ): Boolean = withContext(Dispatchers.Default) {
        val modelConfig = ModelConfig(
            encoderParam = "encoder.param",
            encoderBin = "encoder.bin",
            decoderParam = "decoder.param",
            decoderBin = "decoder.bin",
            joinerParam = "joiner.param",
            joinerBin = "joiner.bin",
            tokens = "tokens.txt",
            numThreads = 4,
            useVulkanCompute = false,
            useBeamSearch = useBeamSearch,
        )
        bridge.initModel(assets, modelConfig)
    }
}