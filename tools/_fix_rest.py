import io


def edit(path, pairs):
    s = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old not in s:
            raise SystemExit('not found in %s: %r' % (path, old[:100]))
        s = s.replace(old, new)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(s)


base = 'app/src/main/java/com/quickscan/'

# --- QrStyle: DEFAULT_FOREGROUND lives on the QrStyle companion, and the
#     drawing helpers are top-level functions that cannot see it unqualified.
p = base + 'core/qr/QrStyle.kt'
s = io.open(p, encoding='utf-8').read()
s = s.replace("            color = DEFAULT_FOREGROUND\n",
              "            color = QrStyle.DEFAULT_FOREGROUND\n")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)

# --- QrViews: inside a Compose Canvas lambda, `this` is a DrawScope.
edit(base + 'core/ui/component/QrViews.kt', [
    ("        QrRenderer.draw(this, m, style, size.width.toInt())",
     "        drawIntoCanvas { canvas ->\n"
     "            QrRenderer.draw(canvas, m, style, size.width.toInt())\n"
     "        }"),
    ("import androidx.compose.foundation.Canvas\n",
     "import androidx.compose.foundation.Canvas\n"
     "import androidx.compose.ui.graphics.drawscope.drawIntoCanvas\n"),
])

# --- MainActivity: a mutable field does not smart cast inside a `when`.
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

# --- CreateScreen: the style swatch needs CircleShape.
p = base + 'feature/create/CreateScreen.kt'
s = io.open(p, encoding='utf-8').read()
if 'import androidx.compose.foundation.shape.CircleShape\n' not in s:
    s = s.replace('import androidx.compose.foundation.layout.Arrangement\n',
                  'import androidx.compose.foundation.layout.Arrangement\n'
                  'import androidx.compose.foundation.shape.CircleShape\n', 1)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)

print('ok')