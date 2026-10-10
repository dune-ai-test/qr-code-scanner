import io


def edit(path, pairs):
    s = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old not in s:
            raise SystemExit('not found in %s: %r' % (path, old[:100]))
        s = s.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(s)


base = 'app/src/main/java/com/quickscan/'

# --- HistoryScreen: the toast block sat outside the composable, and three
#     imports the patch meant to add never landed.
edit(base + 'feature/history/HistoryScreen.kt', [
    ("        if (deletedCount > 0) {\n"
     "        LaunchedEffect(deletedCount) {\n"
     "            Toast.makeText(\n"
     "                context,\n"
     "                context.getString(R.string.deleted_selected, deletedCount),\n"
     "                Toast.LENGTH_SHORT,\n"
     "            ).show()\n"
     "            deletedCount = 0\n"
     "        }\n"
     "    }\n"
     "}", None),
])

s = io.open(base + 'feature/history/HistoryScreen.kt', encoding='utf-8').read()
tail = """        if (deletedCount > 0) {
        LaunchedEffect(deletedCount) {
            Toast.makeText(
                context,
                context.getString(R.string.deleted_selected, deletedCount),
                Toast.LENGTH_SHORT,
            ).show()
            deletedCount = 0
        }
    }
}
"""
assert tail in s, 'tail not found'
s = s.replace(tail, "}\n", 1)

# Put the toast inside the Box, before it closes.
anchor = "            containerColor = palette.surface,\n            )\n        }\n    }\n    }\n"
if anchor not in s:
    anchor = "                containerColor = palette.surface,\n            )\n        }\n    }\n    }\n"
toast = """                containerColor = palette.surface,
            )
        }

        if (deletedCount > 0) {
            LaunchedEffect(deletedCount) {
                Toast.makeText(
                    context,
                    context.getString(R.string.deleted_selected, deletedCount),
                    Toast.LENGTH_SHORT,
                ).show()
                deletedCount = 0
            }
        }
    }
}
"""
assert anchor in s, 'anchor not found'
s = s.replace(anchor, toast, 1)

for imp, after in [
    ("import androidx.compose.ui.platform.LocalContext\n",
     "import androidx.compose.ui.platform.LocalContext\n"),
    ("import androidx.compose.runtime.setValue\n",
     "import androidx.compose.runtime.rememberCoroutineScope\n"),
    ("import kotlinx.coroutines.launch\n",
     "import com.quickscan.R\n"),
]:
    if imp in s:
        continue
    if after in s:
        s = s.replace(after, after + imp, 1)
    else:
        raise SystemExit('anchor missing for ' + imp)
io.open(base + 'feature/history/HistoryScreen.kt', 'w', encoding='utf-8', newline='\n').write(s)

# --- MainActivity: a mutable property does not smart cast inside a `when`.
edit(base + 'MainActivity.kt', [
    ("                onboarded?.let { complete ->\n"
     "                    QuickScanNavHost(\n"
     "                        startDestination = when {\n"
     "                            !complete -> Routes.WELCOME\n"
     "                            requestedRoute != null -> requestedRoute\n"
     "                            else -> Routes.SCAN\n"
     "                        },\n"
     "                    )\n"
     "                }",
     "                onboarded?.let { complete ->\n"
     "                    // requestedRoute is a mutable field, so it needs a\n"
     "                    // local before it can be used as a String.\n"
     "                    val shortcut = requestedRoute\n"
     "                    QuickScanNavHost(\n"
     "                        startDestination = when {\n"
     "                            !complete -> Routes.WELCOME\n"
     "                            shortcut != null -> shortcut\n"
     "                            else -> Routes.SCAN\n"
     "                        },\n"
     "                    )\n"
     "                }"),
])

# --- QrStyle: the constant lives on the QrStyle companion.
edit(base + 'core/qr/QrStyle.kt', [
    ("        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {\n"
     "            color = DEFAULT_FOREGROUND\n"
     "            style = Paint.Style.FILL\n"
     "        }",
     "        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {\n"
     "            color = QrStyle.DEFAULT_FOREGROUND\n"
     "            style = Paint.Style.FILL\n"
     "        }"),
    ("        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {\n"
     "            color = DEFAULT_FOREGROUND\n"
     "            style = Paint.Style.STROKE",
     "        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {\n"
     "            color = QrStyle.DEFAULT_FOREGROUND\n"
     "            style = Paint.Style.STROKE"),
])

# --- QrViews: the Compose Canvas lambda hands over a DrawScope.
edit(base + 'core/ui/component/QrViews.kt', [
    ("    Canvas(modifier) {\n"
     "        val m = matrix ?: return@Canvas\n"
     "        QrRenderer.draw(this, m, style, size.width.toInt())\n"
     "    }",
     "    Canvas(modifier) {\n"
     "        val m = matrix ?: return@Canvas\n"
     "        drawIntoCanvas { canvas ->\n"
     "            QrRenderer.draw(canvas, m, style, size.width.toInt())\n"
     "        }\n"
     "    }"),
    ("import androidx.compose.foundation.Canvas\n",
     "import androidx.compose.foundation.Canvas\nimport androidx.compose.ui.graphics.drawscope.drawIntoCanvas\n"),
])

print('ok')