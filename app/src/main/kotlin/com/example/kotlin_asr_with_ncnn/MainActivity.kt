package com.example.kotlin_asr_with_ncnn

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.kotlin_asr_with_ncnn.core.media.ModelConfig
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.feature.home.ASRScreen
import com.example.kotlin_asr_with_ncnn.feature.home.ASRViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var nativeBridge: NcnnNativeBridge

    private val viewModel: ASRViewModel by viewModels()

    // Audio recording permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initASRModel()
        } else {
            Log.e("MainActivity", "Audio recording permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request audio recording permission
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ASRScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun initASRModel() {
        try {
            // Note: These paths must be relative to the assets/ folder root
            val modelConfig = ModelConfig(
                encoderParam = "encoder.param",
                encoderBin = "encoder.bin",
                decoderParam = "decoder.param",
                decoderBin = "decoder.bin",
                joinerParam = "joiner.param",
                joinerBin = "joiner.bin",
                tokens = "tokens.txt",
                numThreads = 4,
                useVulkanCompute = false 
            )
            
            Log.d("MainActivity", "Attempting to initialize native ASR model...")
            val success = nativeBridge.initModel(assets, modelConfig)
            
            if (success) {
                Log.i("MainActivity", "ASR Model initialized successfully")
                // Start inference if needed or wait for user action
            } else {
                Log.e("MainActivity", "Failed to initialize ASR Model. Check logcat (tag: NcnnASR-Native) for detailed error messages.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception during ASR initialization", e)
        }
    }
}
