package com.task.lumiform.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.task.lumiform.R
import com.task.lumiform.domain.model.SyncStatus
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * A strip at the top of the screen telling the user whether their answers are saved, waiting or sent.
 * [SyncStatus.Synced] is only shown for a moment right after a send finishes, then the strip hides.
 * When the strip is hidden it still pads for the status bar, so the content never slides under it.
 */
@Composable
fun SyncStatusComponent(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    // Turns true while there is something unsent, so reaching Synced afterwards shows "All answers sent" briefly.
    var showSynced by remember { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (status != SyncStatus.Synced) {
            showSynced = true
        } else if (showSynced) {
            delay(3.seconds)
            showSynced = false
        }
    }

    val visible = status != SyncStatus.Synced || showSynced
    val (containerColor, contentColor) = status.colors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(color = containerColor, contentColor = contentColor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (status == SyncStatus.Sending || status == SyncStatus.Retrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = contentColor,
                        )
                    }
                    Text(text = stringResource(status.messageRes()), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun SyncStatus.messageRes() = when (this) {
    SyncStatus.Synced -> R.string.sync_synced
    SyncStatus.SavedOnDevice -> R.string.sync_saved_on_device
    SyncStatus.WaitingForNetwork -> R.string.sync_waiting_for_network
    SyncStatus.Sending -> R.string.sync_sending
    SyncStatus.Retrying -> R.string.sync_retrying
}

@Composable
private fun SyncStatus.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        SyncStatus.Synced -> scheme.primaryContainer to scheme.onPrimaryContainer
        SyncStatus.WaitingForNetwork -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        SyncStatus.Retrying -> scheme.errorContainer to scheme.onErrorContainer
        SyncStatus.SavedOnDevice, SyncStatus.Sending -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}
