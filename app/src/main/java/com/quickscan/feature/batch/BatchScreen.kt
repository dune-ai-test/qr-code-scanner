package com.quickscan.feature.batch

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.quickscan.R
import com.quickscan.core.qr.BatchPayloads
import com.quickscan.core.qr.QrBatchRenderer
import com.quickscan.core.qr.QrExporter
import com.quickscan.core.qr.QrStyle
import com.quickscan.core.ui.component.LucideLayoutGrid
import com.quickscan.core.ui.component.LucideShare2
import com.quickscan.core.ui.component.QSNavBar
import com.quickscan.core.ui.component.QSPrimaryButton
import com.quickscan.core.ui.component.QSSecondaryButton
import com.quickscan.core.ui.component.QSTextField
import com.quickscan.core.ui.component.StatusBarSpacer
import com.quickscan.core.ui.theme.QsTheme
import com.quickscan.core.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything this screen shows is derived from one text field, so it is held
 * here rather than in a ViewModel: `rememberSaveable` already survives the
 * rotation that a ViewModel would exist for, and there is no repository to
 * inject.
 */
@Composable
fun BatchScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val style = QrStyle.DEFAULT
    val state = remember(input) { BatchUiState.of(input) }
    val palette = QsTheme.palette
    val text = QsTheme.text
    val context = LocalContext.current

    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var building by remember { mutableStateOf(false) }
    // Rendering sixty codes is real work, so it runs on a button press rather
    // than on every keystroke, which is what a live preview would cost.
    var buildRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(buildRequest) {
        if (buildRequest == 0) return@LaunchedEffect
        val items = state.items
        if (items.isEmpty()) return@LaunchedEffect
        building = true
        try {
            val bitmap = withContext(Dispatchers.Default) {
                QrBatchRenderer.render(items, style)
            }
            if (bitmap != null) sheet = Sheet(items, bitmap)
        } finally {
            building = false
        }
    }

    // Valid only while the list is the one that was built, so editing after a
    // build hides the stale sheet instead of showing codes that are no longer
    // what is pasted.
    val built = sheet?.takeIf { it.items == state.items }?.bitmap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.x2xl),
    ) {
        StatusBarSpacer()

        QSNavBar(
            title = stringResource(R.string.nav_batch),
            onBack = onBack,
            actionIcon = LucideLayoutGrid,
            actionDescription = stringResource(R.string.batch_clear),
            onAction = { input = ""; sheet = null },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Text(
                text = stringResource(R.string.batch_headline),
                style = text.title21,
                color = palette.ink,
            )
            Text(
                text = stringResource(R.string.batch_body),
                style = text.body14,
                color = palette.inkMuted,
            )

            QSTextField(
                value = input,
                onValueChange = { input = it },
                label = stringResource(R.string.batch_input_label),
                placeholder = stringResource(R.string.batch_input_placeholder),
                singleLine = false,
                height = 190.dp,
                imeAction = ImeAction.Default,
            )

            when {
                state.pastedCount == 0 -> Unit

                state.overCap -> Text(
                    text = stringResource(
                        R.string.batch_capped,
                        BatchPayloads.MAX_ITEMS,
                        state.pastedCount,
                    ),
                    style = text.rowSub12,
                    color = palette.warnIcon,
                )

                state.droppedCount > 0 -> Text(
                    text = stringResource(R.string.batch_dropped, state.droppedCount),
                    style = text.rowSub12,
                    color = palette.inkFaint,
                )

                else -> Text(
                    text = pluralStringResource(
                        R.plurals.batch_ready,
                        state.items.size,
                        state.items.size,
                    ),
                    style = text.rowSub12,
                    color = palette.inkFaint,
                )
            }

            QSPrimaryButton(
                label = stringResource(
                    if (building) R.string.batch_building else R.string.batch_build,
                ),
                onClick = { buildRequest++ },
                enabled = state.canBuild && !building,
                icon = LucideLayoutGrid,
                modifier = Modifier.fillMaxWidth(),
            )

            if (built != null) {
                Image(
                    bitmap = built.asImageBitmap(),
                    contentDescription = stringResource(R.string.batch_sheet_desc),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(top = Space.sm),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.batch_sheet_size,
                        state.items.size,
                        state.items.size,
                        built.width,
                        built.height,
                    ),
                    style = text.rowSub12,
                    color = palette.inkFaint,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    QSPrimaryButton(
                        label = stringResource(R.string.batch_share),
                        onClick = {
                            context.startActivity(
                                QrExporter.shareIntent(
                                    context,
                                    built,
                                    context.getString(R.string.batch_share),
                                ),
                            )
                        },
                        icon = LucideShare2,
                        modifier = Modifier.weight(1f),
                    )
                    QSSecondaryButton(
                        label = stringResource(R.string.batch_save),
                        onClick = {
                            QrExporter.saveToGallery(context, built, BATCH_FILE_NAME)
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        R.string.batch_save_failed,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(Space.hero))
        }
    }
}

private data class BatchUiState(
    val items: List<BatchPayloads.Item>,
    /** Non-blank lines the user actually pasted. */
    val pastedCount: Int,
) {
    /**
     * Lines that became no code — duplicates, and anything past the cap.
     * Surfaced rather than swallowed, because a sheet with fewer codes than
     * lines pasted looks like a bug and is one unless it says so.
     */
    val droppedCount: Int get() = (pastedCount - items.size).coerceAtLeast(0)

    val overCap: Boolean get() = pastedCount > BatchPayloads.MAX_ITEMS

    val canBuild: Boolean get() = items.isNotEmpty()

    companion object {
        /** Parsed once per edit; recomposition reads it many times. */
        fun of(input: String): BatchUiState = BatchUiState(
            items = BatchPayloads.parse(input),
            pastedCount = input.lineSequence().count { line -> line.isNotBlank() },
        )
    }
}

/** What was built, and from what, so a stale sheet can be recognised. */
private class Sheet(
    val items: List<BatchPayloads.Item>,
    val bitmap: Bitmap,
)

private const val BATCH_FILE_NAME = "quickscan-batch.png"