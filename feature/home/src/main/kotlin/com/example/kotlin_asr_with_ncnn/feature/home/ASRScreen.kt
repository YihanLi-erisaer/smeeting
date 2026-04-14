package com.example.kotlin_asr_with_ncnn.feature.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ASRScreen(
    viewModel: ASRViewModel,
    isModelLoading: Boolean = false,
    modelErrorMessage: String? = null,
    onSettingsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val outputScrollState = rememberScrollState()
    val view = LocalView.current
    val resultText = when {
        isModelLoading -> stringResource(R.string.loading_model_please_wait)
        modelErrorMessage != null -> modelErrorMessage
        uiState.isListening && uiState.resultText.isBlank() -> stringResource(R.string.recording)
        else -> uiState.resultText
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ASRContract.Effect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                }
                is ASRContract.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(resultText) {
        // Wait for the new text to be laid out before following the latest streamed output.
        withFrameNanos { }
        outputScrollState.scrollTo(outputScrollState.maxValue)
    }

    DisposableEffect(view, uiState.isListening) {
        val previousKeepScreenOn = view.keepScreenOn
        if (uiState.isListening) {
            view.keepScreenOn = true
        }

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isModelLoading -> stringResource(R.string.loading_model)
                    modelErrorMessage != null -> stringResource(R.string.model_error_title)
                    uiState.isListening -> stringResource(R.string.result_title)
                    else -> stringResource(R.string.press_start_hint)
                },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = resultText,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(outputScrollState),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.onIntent(ASRContract.Intent.ToggleListening) },
                    enabled = !isModelLoading && modelErrorMessage == null
                ) {
                    Text(
                        text = if (uiState.isListening) {
                            stringResource(R.string.stop)
                        } else {
                            stringResource(R.string.start)
                        }
                    )
                }

                Button(
                    onClick = { viewModel.onIntent(ASRContract.Intent.CopyResultClicked) },
                    enabled = uiState.canCopy
                ) {
                    Text(text = stringResource(R.string.copy))
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(text = stringResource(R.string.ai_mistakes_warning), style = MaterialTheme.typography.labelSmall)
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(48.dp)
        ) {
            Text(text = "⚙", style = MaterialTheme.typography.titleLarge)
        }

        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.history_open),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
