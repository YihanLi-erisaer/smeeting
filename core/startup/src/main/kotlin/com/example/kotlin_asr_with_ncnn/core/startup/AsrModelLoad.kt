package com.example.kotlin_asr_with_ncnn.core.startup

import android.util.Log
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
        fun config(useVulkan: Boolean) = ModelConfig(
            encoderParam = "encoder.param",
            encoderBin = "encoder.bin",
            decoderParam = "decoder.param",
            decoderBin = "decoder.bin",
            joinerParam = "joiner.param",
            joinerBin = "joiner.bin",
            tokens = "tokens.txt",
            numThreads = 4,
            useVulkanCompute = useVulkan,
            useBeamSearch = useBeamSearch,
        )

        if (bridge.initModel(assets, config(useVulkan = true))) {
            return@withContext true
        }

        Log.i("AsrModelLoad", "Vulkan init failed or unavailable; retrying with CPU only")
        bridge.releaseModel()
        bridge.initModel(assets, config(useVulkan = false))
    }
}