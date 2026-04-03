package com.example.kotlin_asr_with_ncnn.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    darkTheme: Boolean,
    useBeamSearch: Boolean,
    appVersion: String,
    onDarkThemeChanged: (Boolean) -> Unit,
    onUseBeamSearchChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.example.kotlin_asr_with_ncnn.feature.history.R.string.history_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← ${stringResource(com.example.kotlin_asr_with_ncnn.feature.history.R.string.back)}")
                    }
                }
            )
        }
    ){ padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(com.example.kotlin_asr_with_ncnn.feature.history.R.string.app_version),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
