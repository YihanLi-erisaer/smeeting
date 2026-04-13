package com.example.kotlin_asr_with_ncnn

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.lifecycle.lifecycleScope
import com.example.kotlin_asr_with_ncnn.core.common.ThemePreferences
import com.example.kotlin_asr_with_ncnn.core.media.InferenceBackend
import com.example.kotlin_asr_with_ncnn.core.media.NcnnNativeBridge
import com.example.kotlin_asr_with_ncnn.core.startup.AsrModelManager
import com.example.kotlin_asr_with_ncnn.core.ui.rememberThemeState
import com.example.kotlin_asr_with_ncnn.feature.home.ASRViewModel
import com.example.kotlin_asr_with_ncnn.feature.history.HistoryViewModel
import com.example.kotlin_asr_with_ncnn.core.startup.ModelInitNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.kotlin_asr_with_ncnn.feature.settings.R as SettingsR
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var nativeBridge: NcnnNativeBridge
    @Inject
    lateinit var asrModelManager: AsrModelManager
    @Inject
    lateinit var themePreferences: ThemePreferences
    @Inject
    lateinit var modelInitNotifier: ModelInitNotifier
    private val viewModel: ASRViewModel by viewModels()
    private val mainUiViewModel: MainUiViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val asrModelCoordinator by lazy {
        AsrModelCoordinator(
            activity = this,
            asrModelManager = asrModelManager,
            themePreferences = themePreferences,
            modelInitNotifier = modelInitNotifier,
            mainUiViewModel = mainUiViewModel,
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> asrModelCoordinator.onAudioPermissionResult(isGranted) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!asrModelCoordinator.hasAudioPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            LaunchedEffect(Unit) {
                launch {
                    asrModelCoordinator.observeModelInitEvents()
                }
                withFrameMillis { }
                asrModelCoordinator.runStartupPipeline(this)
            }
            val darkTheme by themePreferences.darkThemeFlow.collectAsState(initial = false)
            val useBeamSearch by themePreferences.useBeamSearchFlow.collectAsState(initial = false)
            val modelInitState by mainUiViewModel.modelInitState.collectAsState()
            val currentScreen by mainUiViewModel.currentScreen.collectAsState()
            val inferenceBackend by nativeBridge.inferenceBackend.collectAsState(initial = null)
            val inferenceBackendLabel = when (modelInitState) {
                is ModelInitState.Loading -> stringResource(SettingsR.string.inference_backend_loading)
                is ModelInitState.Error -> stringResource(SettingsR.string.inference_backend_unavailable)
                is ModelInitState.Ready -> when (inferenceBackend) {
                    InferenceBackend.Gpu -> stringResource(SettingsR.string.inference_backend_gpu)
                    InferenceBackend.Cpu -> stringResource(SettingsR.string.inference_backend_cpu)
                    null -> stringResource(SettingsR.string.inference_backend_unknown)
                }
            }
            LaunchedEffect(useBeamSearch, modelInitState) {
                asrModelCoordinator.syncDecoderMode(useBeamSearch, modelInitState)
            }
            val themeState = rememberThemeState(
                initialDarkTheme = darkTheme,
                onThemeChanged = { lifecycleScope.launch { themePreferences.setDarkTheme(it) } }
            )
            val appVersion = remember { getAppVersion() }

            MainAppScreen(
                viewModel = viewModel,
                historyViewModel = historyViewModel,
                mainUiViewModel = mainUiViewModel,
                themePreferences = themePreferences,
                themeState = themeState,
                currentScreen = currentScreen,
                useBeamSearch = useBeamSearch,
                appVersion = appVersion,
                inferenceBackendLabel = inferenceBackendLabel,
                modelInitState = modelInitState,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to read app version", e)
            getString(R.string.unknown_version)
        }
    }
}
