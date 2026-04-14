package com.example.kotlin_asr_with_ncnn.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kotlin_asr_with_ncnn.domain.model.TranscriptionHistoryEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val entries by viewModel.entries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.history_copied)
    var revealedDeleteId by remember { mutableStateOf<String?>(null) }
    var selectedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailMenuExpanded by remember { mutableStateOf(false) }
    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId }
    val isShowingDetail = selectedEntry != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title)
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            detailMenuExpanded = false
                            if (isShowingDetail) {
                                selectedEntryId = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Text("← ${stringResource(R.string.back)}")
                    }
                },
                actions = {
                    if (selectedEntry != null) {
                        Box {
                            IconButton(onClick = { detailMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.history_title),
                                )
                            }
                            DropdownMenu(
                                expanded = detailMenuExpanded,
                                onDismissRequest = { detailMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.history_copy)) },
                                    onClick = {
                                        detailMenuExpanded = false
                                        copyToClipboard(context, selectedEntry.text)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(copiedMessage)
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.history_delete),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        detailMenuExpanded = false
                                        viewModel.deleteEntry(selectedEntry.id)
                                        revealedDeleteId = null
                                        selectedEntryId = null
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (selectedEntry != null) {
            HistoryEntryDetail(
                item = selectedEntry,
                contentPadding = padding,
            )
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = entries,
                    key = { it.id },
                ) { item ->
                    HistoryEntryRow(
                        item = item,
                        showDelete = revealedDeleteId == item.id,
                        onOpenDetail = {
                            revealedDeleteId = null
                            selectedEntryId = item.id
                        },
                        onRevealDelete = { revealedDeleteId = item.id },
                        onCollapseDelete = { revealedDeleteId = null },
                        onCopy = {
                            copyToClipboard(context, item.text)
                            scope.launch {
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        },
                        onDelete = {
                            viewModel.deleteEntry(item.id)
                            revealedDeleteId = null
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryEntryRow(
    item: TranscriptionHistoryEntry,
    showDelete: Boolean,
    onOpenDetail: () -> Unit,
    onRevealDelete: () -> Unit,
    onCollapseDelete: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                    .combinedClickable(
                        onClick = onOpenDetail,
                        onLongClick = {
                            if (showDelete) onCollapseDelete() else onRevealDelete()
                        },
                    ),
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatHistoryTime(item.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.history_copy))
            }
            AnimatedVisibility(
                visible = showDelete,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.history_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryDetail(
    item: TranscriptionHistoryEntry,
    contentPadding: PaddingValues,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = formatHistoryTime(item.createdAtMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private fun formatHistoryTime(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("transcription", text))
}
