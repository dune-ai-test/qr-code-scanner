package com.quickscan.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickscan.R
import com.quickscan.core.ui.component.LucideArrowRight
import com.quickscan.core.ui.component.LucideCheck
import com.quickscan.core.ui.component.LucideCircleX
import com.quickscan.core.ui.component.LucideLock
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.theme.Metrics
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Radius
import com.quickscan.core.ui.theme.Space

@Composable
fun OnboardingNameScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = QsTheme.palette
    val text = QsTheme.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_set_up),
            onBack = onBack,
            actionIcon = LucideCheck,
            actionDescription = stringResource(R.string.continue_label),
            onAction = {
                viewModel.complete()
                onContinue()
            },
        )

        Column(
            modifier = Modifier.padding(
                start = Space.section,
                end = Space.section,
                top = Space.hero,
                bottom = Space.x6xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.section),
        ) {
            Text(
                text = stringResource(R.string.name_title),
                style = text.display30,
                color = palette.ink,
            )
            Text(
                text = stringResource(R.string.name_subtitle),
                style = text.body15,
                color = palette.inkMuted,
            )

            // The preview tracks the field as it is typed into.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Metrics.heroAvatar + 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(Metrics.heroAvatar)
                        .shadow(8.dp, CircleShape, spotColor = palette.accent)
                        .clip(CircleShape)
                        .background(palette.accentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.initial,
                        style = QsTheme.text.display32,
                        color = palette.accentTintInk,
                    )
                }
            }

            QSTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = stringResource(R.string.your_name_label),
                placeholder = stringResource(R.string.name_hint),
                height = 56.dp,
                radius = Radius.lg,
                trailing = if (state.name.isNotEmpty()) {
                    {
                        Icon(
                            imageVector = LucideCircleX,
                            contentDescription = stringResource(R.string.name_clear),
                            tint = palette.inkFaint,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.onNameChanged("") },
                        )
                    }
                } else {
                    null
                },
            )

            PrivacyHint()

            Spacer(Modifier.height(Space.xl))

            QSPrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = {
                    viewModel.complete()
                    onContinue()
                },
                icon = LucideArrowRight,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.skip_for_now),
                style = QsTheme.text.body14.copy(fontWeight = FontWeight.Medium),
                color = palette.inkFaint,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.complete("")
                        onContinue()
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrivacyHint() {
    val palette = QsTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.xs)
            .background(palette.successSurface)
            .padding(horizontal = 14.dp, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = LucideLock,
            contentDescription = null,
            tint = palette.successIcon,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.stored_locally),
            style = QsTheme.text.chip13.copy(fontWeight = FontWeight.Medium),
            color = palette.successInk,
        )
    }
}