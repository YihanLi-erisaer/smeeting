package com.example.kotlin_asr_with_ncnn.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ASRScreen(
    viewModel: ASRViewModel
) {
    val transcription by viewModel.transcriptionState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isListening) "Result text" else "Press bottom to Start",
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
                    text = transcription?.text ?: "Result text",
                    style = MaterialTheme.typography.bodyLarge
                )
                /** Text(
                    text = "Confidence: ${transcription?.confidence ?: 0f}",
                    style = MaterialTheme.typography.bodySmall
                )**/
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.toggleListening() }
        ) {
            Text(text = if (isListening) "Stop" else "Start")
        }
    }
}