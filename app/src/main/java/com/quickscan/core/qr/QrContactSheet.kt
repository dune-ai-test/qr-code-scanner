package com.quickscan.core.qr

/**
 * Where each code goes on a sheet of many.
 *
 * One image holding every code is the useful shape for a batch: it can be
 * shared in a single action, printed on one page, and put on a wall. Ten
 * separate files would be easier to generate and impossible to hand to
 * anyone.
 *
 * This is arithmetic only — no pixels, no ZXing, no Android — so the grid can
 * be checked without rendering anything.
 */
object QrContactSheet {

    /**
     * A poster past about this size is not printable on a phone and stops
     * being a sheet anyone wants. Refusing the rest is better than producing a
     * 30MB bitmap that will not open.
     */
    const val MAX_ITEMS = 60

    const val MAX_COLUMNS = 4

    /** Roughly 52mm at 300dpi — printable on a home printer at any size. */
    private const val CELL_WIDTH = 520

    private const val PADDING = 48

    /** Room for a two-line caption under each code. */
    private const val LABEL_HEIGHT = 104

    data class Options(
        val count: Int,
        val columns: Int,
        val rows: Int,
        val cellWidth: Int,
        val cellHeight: Int,
        val codeSize: Int,
        val padding: Int,
        val width: Int,
        val height: Int,
    ) {
        /** The final cell may be short; a full page of background otherwise. */
        val isFullGrid: Boolean get() = count == columns * rows
    }

    data class Placement(
        val index: Int,
        val codeLeft: Int,
        val codeTop: Int,
        val codeSize: Int,
        val labelLeft: Int,
        val labelTop: Int,
        val labelWidth: Int,
    )

    fun optionsFor(count: Int): Options {
        val safe = count.coerceIn(0, MAX_ITEMS)
        // Wider than tall reads better on a wall and prints better on a
        // portrait page; a single code stays square.
        val columns = when {
            safe <= 1 -> 1
            safe <= 4 -> 2
            safe <= 9 -> 3
            else -> MAX_COLUMNS
        }
        val rows = if (safe == 0) 0 else (safe + columns - 1) / columns
        val codeSize = CELL_WIDTH - PADDING
        return Options(
            count = safe,
            columns = columns,
            rows = rows,
            cellWidth = CELL_WIDTH,
            cellHeight = codeSize + LABEL_HEIGHT,
            codeSize = codeSize,
            padding = PADDING,
            width = PADDING * 2 + columns * CELL_WIDTH,
            height = PADDING * 2 + rows * (codeSize + LABEL_HEIGHT),
        )
    }

    fun placements(options: Options): List<Placement> =
        (0 until options.count).map { index ->
            val row = index / options.columns
            val column = index % options.columns
            val cellLeft = options.padding + column * options.cellWidth
            val cellTop = options.padding + row * options.cellHeight
            val inset = options.padding / 2
            Placement(
                index = index,
                codeLeft = cellLeft + inset,
                codeTop = cellTop,
                codeSize = options.codeSize,
                labelLeft = cellLeft + inset,
                labelTop = cellTop + options.codeSize,
                labelWidth = options.codeSize,
            )
        }
}