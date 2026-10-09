package com.quickscan.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickscan.R
import com.quickscan.core.ui.theme.Chrome
import com.quickscan.core.ui.theme.QsTheme

/**
 * Reserves the system status bar. The mockups draw an iOS-style status bar
 * with time and signal glyphs; QuickScan lets the real Android one show through
 * instead, so this only holds the space the design expects to be there.
 */
@Composable
fun StatusBarSpacer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.height(
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        ),
    )
}

/**
 * 56dp nav bar: a circular back affordance, a centred title, and a circular
 * action on the trailing edge. The title stays optically centred because both
 * end slots are the same 36dp size.
 */
@Composable
fun QSNavBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Chrome.navBar)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = LucideChevronLeft,
                contentDescription = stringResource(R.string.action_back),
                background = palette.surface,
                tint = palette.ink,
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.size(Chrome.navButton))
        }

        Text(
            text = title,
            style = text.nav16,
            color = palette.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        if (actionIcon != null && onAction != null) {
            CircleIconButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                background = palette.surface,
                tint = palette.ink,
                onClick = onAction,
            )
        } else {
            Spacer(Modifier.size(Chrome.navButton))
        }
    }
}

/** 36dp circular surface button used by the nav bar and result actions. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Chrome.navButton,
    iconSize: Dp = 18.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape)
            .clickable(onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}