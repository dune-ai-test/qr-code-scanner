import io


def edit(path, pairs):
    s = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old not in s:
            raise SystemExit('not found in %s: %r' % (path, old[:80]))
        s = s.replace(old, new)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(s)


# --- Strings.
edit('app/src/main/res/values/strings.xml', [
    ('    <string name="tab_scan">Scan</string>',
     '    <string name="pin_scan">Pin this scan</string>\n'
     '    <string name="unpin_scan">Remove from favourites</string>\n'
     '    <string name="pinned_section">FAVOURITES</string>\n'
     '    <string name="tab_scan">Scan</string>'),
])

# --- Repository: flip the pin.
edit('app/src/main/java/com/quickscan/data/repository/ScanRepository.kt', [
    ("    suspend fun delete(id: Long) = dao.deleteById(id)",
     "    suspend fun delete(id: Long) = dao.deleteById(id)\n\n"
     "    /** Pins or unpins a scan, whichever it currently is not. */\n"
     "    suspend fun togglePinned(id: Long): Boolean {\n"
     "        val row = dao.findById(id) ?: return false\n"
     "        val next = !row.isPinned\n"
     "        dao.update(row.copy(isPinned = next))\n"
     "        return next\n"
     "    }"),
])

# --- ResultViewModel: expose the toggle.
edit('app/src/main/java/com/quickscan/feature/result/ResultViewModel.kt', [
    ("    fun togglePasswordVisibility() {\n"
     "        _revealedPassword.value = !_revealedPassword.value\n"
     "    }",
     "    fun togglePasswordVisibility() {\n"
     "        _revealedPassword.value = !_revealedPassword.value\n"
     "    }\n\n"
     "    fun togglePinned() {\n"
     "        viewModelScope.launch { scanRepository.togglePinned(scanId) }\n"
     "    }"),
])

# --- ResultScreen: the nav slot becomes the pin; share moves to the action row.
edit('app/src/main/java/com/quickscan/feature/result/ResultScreen.kt', [
    ("            actionIcon = LucideEllipsis,\n"
     "            actionDescription = stringResource(R.string.more_actions),\n"
     "            onAction = { payload?.let { context.sharePayload(it) } },",
     "            actionIcon = if (state.entity?.isPinned == true) {\n"
     "                LucideBookmarkCheck\n"
     "            } else {\n"
     "                LucideBookmark\n"
     "            },\n"
     "            actionDescription = stringResource(\n"
     "                if (state.entity?.isPinned == true) {\n"
     "                    R.string.unpin_scan\n"
     "                } else {\n"
     "                    R.string.pin_scan\n"
     "                },\n"
     "            ),\n"
     "            onAction = viewModel::togglePinned,"),
    ("            ActionRow(payload = payload)",
     "            ActionRow(\n"
     "                payload = payload,\n"
     "                onShare = { context.sharePayload(payload) },\n"
     "            )"),
    ("private fun ActionRow(payload: ScannedPayload) {\n"
     "    val context = LocalContext.current\n",
     "private fun ActionRow(payload: ScannedPayload, onShare: () -> Unit) {\n"),
    ("import com.quickscan.core.ui.component.LucideBookmark\n",
     "import com.quickscan.core.ui.component.LucideBookmark\n"
     "import com.quickscan.core.ui.component.LucideBookmarkCheck\n"),
])

print('ok')
