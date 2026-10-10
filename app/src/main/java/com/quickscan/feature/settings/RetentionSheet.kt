package com.quickscan.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickscan.R
import com.quickscan.core.ui.component.LucideCheck
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/**
 * The choices behind "Keep history for…".
 *
 * Zero means keep everything, which is why it is offered explicitly rather
 * than living at the end of a cycle the user has to guess.
 */
private val RETENTION_OPTIONS = listOf(
    7 to R.string.retention_week,
    30 to R.string.retention_month,
    90 to R.string.retention_three_months,
    365 to R.string.retention_year,
    0 to R.string.retention_always,
)

@Composable
fun RetentionSheet(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.hero)
            .background(palette.surfaceElevated)
            .padding(horizontal = Space.xl, vertical = Space.xxl)
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + Space.xxl,
            ),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            text = stringResource(R.string.setting_retention_title),
            style = text.nav16,
            color = palette.ink,
            modifier = Modifier.padding(bottom = Space.sm),
        )

        RETENTION_OPTIONS.forEach { (days, label) ->
            val selected = days == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.xl)
                    .background(if (selected) palette.accentTint else Color.Transparent)
                    .clickable {
                        onPick(days)
                        onDismiss()
                    }
                    .padding(horizontal = Space.xl, vertical = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(label),
                    style = text.body15.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) palette.accentTintInk else palette.ink,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = LucideCheck,
                        contentDescription = null,
                        tint = palette.accentTintInk,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}