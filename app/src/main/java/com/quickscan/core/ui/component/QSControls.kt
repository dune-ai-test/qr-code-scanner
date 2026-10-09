package com.quickscan.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickscan.core.ui.theme.Metrics
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

/** The 54dp primary call to action: accent fill, coloured lift, optional icon. */
@Composable
fun QSPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = QsTheme.palette.accent,
    content: Color = QsTheme.palette.accentOn,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Row(
        modifier = modifier
            .height(Metrics.buttonHeight)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = Radius.lgPlus,
                ambientColor = container,
                spotColor = container,
            )
            .clip(Radius.lgPlus)
            .background(if (enabled) container else palette.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.xxl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) content else palette.inkFaint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(9.dp))
        }
        Text(
            text = label,
            style = text.body15.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) content else palette.inkFaint,
            maxLines = 1,
        )
    }
}

/** A flat alternative to [QSPrimaryButton]: surface fill, no lift. */
@Composable
fun QSSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = QsTheme.palette.ink,
    enabled: Boolean = true,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Box(
        modifier = modifier
            .height(Metrics.buttonHeight)
            .clip(Radius.lgPlus)
            .background(palette.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = text.body15.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) ink else palette.inkFaint,
            maxLines = 1,
        )
    }
}

/** Square 54dp icon button that flanks a primary button, e.g. Copy or Share. */
@Composable
fun QSSquareAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    Box(
        modifier = modifier
            .size(Metrics.buttonHeight)
            .clip(Radius.lgPlus)
            .background(palette.surface)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** The 92px tile used for Scan image / Paste link on the scanner. */
@Composable
fun QSActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = modifier
            .height(92.dp)
            .clip(Radius.xxl)
            .background(palette.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(palette.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accentTintInk,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(text = label, style = text.chip13, color = palette.ink, maxLines = 1)
    }
}

/** Filter pill used on History. The active chip inverts to solid ink. */
@Composable
fun QSChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text
    val background by animateColorAsState(
        if (selected) palette.ink else palette.surface,
        tween(160),
        label = "chipBackground",
    )

    Box(
        modifier = modifier
            .height(Metrics.chipHeight)
            .clip(Radius.pill)
            .background(background)
            .clickable(onClick = onClick)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .padding(horizontal = Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = text.chip13.copy(
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
            ),
            color = if (selected) palette.bg else palette.inkMuted,
            maxLines = 1,
        )
    }
}

/**
 * iOS-style switch: a 51x31 track with a 27dp knob that slides rather than
 * scales. Off-state uses the hairline token so it reads on a surface card.
 */
@Composable
fun QSSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = QsTheme.palette
    val trackColor by animateColorAsState(
        if (checked) palette.accent else palette.hairline,
        tween(180),
        label = "switchTrack",
    )
    val knobOffset by animateDpAsState(
        if (checked) 22.dp else 2.dp,
        tween(180),
        label = "switchKnob",
    )

    Box(
        modifier = modifier
            .width(Metrics.switchTrack)
            .height(Metrics.switchTrackHeight)
            .clip(Radius.pill)
            .background(trackColor)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .semantics { stateDescription = if (checked) "On" else "Off" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = knobOffset)
                .size(Metrics.switchKnob)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * Labelled field matching the create and onboarding forms: an 11pt caps label
 * above a 48dp pill that grows a tinted ring on focus.
 */
@Composable
fun QSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    height: Dp = 48.dp,
    radius: Shape = Radius.md,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val palette = QsTheme.palette
    val text = QsTheme.text
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (focused) palette.accentTintInk.copy(alpha = 0.4f) else Color.Transparent,
        tween(160),
        label = "fieldRing",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(text = label, style = text.chip13, color = palette.inkFaint, maxLines = 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(radius)
                .background(palette.surface)
                .border(1.5.dp, borderColor, radius)
                .padding(horizontal = Space.xl),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = text.body14.copy(color = palette.ink),
                    singleLine = singleLine,
                    cursorBrush = SolidColor(palette.accentTintInk),
                    interactionSource = interaction,
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = text.body14,
                        color = palette.inkFaint,
                        maxLines = 1,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

