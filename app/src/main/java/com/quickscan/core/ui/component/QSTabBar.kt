package com.quickscan.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.quickscan.R
import com.quickscan.core.ui.theme.Chrome
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius

enum class TabDestination(
    val labelRes: Int,
    val icon: ImageVector,
) {
    Scan(R.string.tab_scan, LucideScanLine),
    History(R.string.tab_history, LucideHistory),
    Create(R.string.tab_create, LucideQrCode),
    Settings(R.string.tab_settings, LucideSettings),
}

/**
 * The floating capsule tab bar: a translucent pill with a hairline inner stroke
 * and one tinted capsule marking the active destination. This is the iOS-native
 * tab pattern from the mockups, in place of Material's NavigationBar.
 */
@Composable
fun QSTabBar(
    selected: TabDestination,
    onSelect: (TabDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            ),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = Radius.capsule,
                    ambientColor = palette.shadow,
                    spotColor = palette.shadow,
                )
                .width(Chrome.tabBarWidth)
                .height(Chrome.tabBar)
                .clip(Radius.capsule)
                .background(palette.frosted)
                .border(1.dp, palette.frostedOutline, Radius.capsule)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabDestination.entries.forEach { destination ->
                TabItem(
                    destination = destination,
                    isSelected = destination == selected,
                    onClick = { onSelect(destination) },
                    label = stringResource(destination.labelRes),
                    activeInk = palette.accentTintInk,
                    idleInk = palette.inkFaint,
                    activeFill = palette.accentTint,
                    shape = Radius.capsule,
                    textStyle = text.tabLabel10,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    destination: TabDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    activeInk: Color,
    idleInk: Color,
    activeFill: Color,
    shape: Shape,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val ink = if (isSelected) activeInk else idleInk

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(Chrome.tabBar - 12.dp)
            .clip(shape)
            .background(if (isSelected) activeFill else Color.Transparent)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics {
                contentDescription = label
                stateDescription = if (isSelected) "Selected" else "Not selected"
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = textStyle, color = ink, maxLines = 1)
    }
}