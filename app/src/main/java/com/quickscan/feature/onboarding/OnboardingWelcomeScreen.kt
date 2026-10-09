package com.quickscan.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickscan.R
import com.quickscan.core.ui.component.LucideArrowRight
import com.quickscan.core.ui.component.LucideShieldCheck
import com.quickscan.core.ui.component.LucideWifiOff
import com.quickscan.core.ui.component.LucideZap
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QrCornerBrackets
import com.quickscan.core.ui.component.QrPlaceholderView
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.component.ValuePropRow
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

private const val HERO_QR_SEED = 7_315_811L

@Composable
fun OnboardingWelcomeScreen(onGetStarted: () -> Unit) {
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        StatusBarSpacer()

        Column(
            modifier = Modifier.padding(
                start = Space.section,
                end = Space.section,
                top = Space.section,
                bottom = Space.x6xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3xl),
        ) {
            WelcomeHero()

            Text(
                text = stringResource(R.string.onboarding_title),
                style = text.display32,
                color = palette.ink,
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = text.body15,
                color = palette.inkMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                ValuePropRow(
                    icon = LucideZap,
                    title = stringResource(R.string.value_instant_title),
                    subtitle = stringResource(R.string.value_instant_subtitle),
                )
                ValuePropRow(
                    icon = LucideWifiOff,
                    title = stringResource(R.string.value_offline_title),
                    subtitle = stringResource(R.string.value_offline_subtitle),
                )
                ValuePropRow(
                    icon = LucideShieldCheck,
                    title = stringResource(R.string.value_private_title),
                    subtitle = stringResource(R.string.value_private_subtitle),
                )
            }

            QSPrimaryButton(
                label = stringResource(R.string.get_started),
                onClick = onGetStarted,
                icon = LucideArrowRight,
                container = palette.ink,
                content = palette.bg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The 262dp hero: accent glow, a bracketed sample code and a sweep line. */
@Composable
private fun WelcomeHero() {
    val palette = QsTheme.palette

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(262.dp)
            .clip(Radius.hero)
            .background(palette.surface),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(226.dp)
                .height(174.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent, Color.Transparent),
                        radius = 113f,
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(Radius.xl)
                .background(palette.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            QrPlaceholderView(
                seed = HERO_QR_SEED,
                foreground = palette.ink,
                modifier = Modifier.size(140.dp),
            )
        }

        QrCornerBrackets(
            color = palette.accent,
            armLength = 32.dp,
            strokeWidth = 4.dp,
            modifier = Modifier.size(200.dp),
        )
    }
}