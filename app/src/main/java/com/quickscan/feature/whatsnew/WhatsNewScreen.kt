package com.quickscan.feature.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.release.ReleaseNotes
import com.quickscan.core.ui.component.LucideCheck
import com.quickscan.core.ui.component.LucideSparkles
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/**
 * The release history, newest first. Marking it read is what clears the badge
 * on the Settings row, so opening this screen is the acknowledgement.
 */
@Composable
fun WhatsNewScreen(
    onBack: () -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val text = QsTheme.text

    LaunchedEffect(Unit) { viewModel.markRead() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.whats_new_title),
            onBack = onBack,
            actionIcon = LucideCheck,
            actionDescription = stringResource(R.string.whats_new_mark_read),
            onAction = viewModel::markRead,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.x2xl),
            verticalArrangement = Arrangement.spacedBy(Space.x2xl),
        ) {
            state.entries.forEach { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius.xlPlus)
                        .background(palette.surface)
                        .padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.xl),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentTint),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = LucideSparkles,
                                    contentDescription = null,
                                    tint = palette.accentTintInk,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = entry.version,
                                    style = text.nav16,
                                    color = palette.ink,
                                )
                                Text(
                                    text = entry.date,
                                    style = text.rowSub12,
                                    color = palette.inkFaint,
                                )
                            }
                        }
                        if (entry.version == ReleaseNotes.current.version) {
                            Badge(label = stringResource(R.string.whats_new_new))
                        }
                    }

                    entry.highlights.forEach { highlight ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = highlight.title,
                                style = text.row15,
                                color = palette.ink,
                            )
                            Text(
                                text = highlight.detail,
                                style = text.body14,
                                color = palette.inkMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.x2xl))
        }
    }
}

@Composable
private fun Badge(label: String) {
    val palette = QsTheme.palette
    Text(
        text = label,
        style = QsTheme.text.label11,
        color = palette.accentTintInk,
        modifier = Modifier
            .clip(Radius.pill)
            .background(palette.accentTint)
            .padding(horizontal = Space.md, vertical = 5.dp),
    )
}
