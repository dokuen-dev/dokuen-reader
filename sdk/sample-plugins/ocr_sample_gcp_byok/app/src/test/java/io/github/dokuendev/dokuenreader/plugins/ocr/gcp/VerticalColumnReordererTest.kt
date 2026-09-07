package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VerticalColumnReordererTest {

    /**
     * Builds one synthetic column's symbols: one character per position, reading top-to-bottom
     * starting at ([startX], [startY]), optionally drifting horizontally by [xDriftPerChar] per
     * step to simulate a photo where the text is at a slight angle rather than perfectly
     * vertical.
     */
    private fun buildColumn(
        text: String,
        startX: Float,
        startY: Float = 0f,
        charWidth: Float = 10f,
        charHeight: Float = 10f,
        xDriftPerChar: Float = 0f
    ): List<Pair<String, RectF>> {
        return text.mapIndexed { index, ch ->
            val x = startX + xDriftPerChar * index
            val y = startY + charHeight * index
            Pair(ch.toString(), RectF(x, y, x + charWidth, y + charHeight))
        }
    }

    private fun textOf(symbols: List<Pair<String, RectF>>): String = symbols.joinToString("") { it.first }

    @Test
    fun reversedColumns_getsReorderedRightToLeft() {
        val columnLeft = buildColumn("ABCD", startX = 0f)
        val columnMiddle = buildColumn("EFGH", startX = 20f)
        val columnRight = buildColumn("IJKL", startX = 40f)

        // GCP's broken order: left-to-right (the failure mode this class corrects).
        val input = columnLeft + columnMiddle + columnRight

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        // Correct vertical reading order: rightmost column first, each column's own internal
        // (already-correct) top-to-bottom order untouched.
        assertEquals("IJKLEFGHABCD", textOf(result))
    }

    @Test
    fun alreadyCorrectRightToLeftColumns_leftUnchanged() {
        val columnLeft = buildColumn("ABCD", startX = 0f)
        val columnMiddle = buildColumn("EFGH", startX = 20f)
        val columnRight = buildColumn("IJKL", startX = 40f)

        // Already in correct right-to-left order.
        val input = columnRight + columnMiddle + columnLeft

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        // Confidently-correct case should hit the cheap early-out and return the same instance,
        // not just equal content. Nothing should even attempt a reorder here.
        assertSame(input, result)
        assertEquals("IJKLEFGHABCD", textOf(result))
    }

    @Test
    fun singleColumn_leftUnchanged() {
        val singleColumn = buildColumn("ABCDEFGH", startX = 0f)

        val result = VerticalColumnReorderer.reorderIfNeeded(singleColumn)

        // First and last symbols land at nearly the same X. No directional evidence, so this
        // must be left alone rather than run through full segmentation on weak evidence.
        assertSame(singleColumn, result)
    }

    @Test
    fun tooFewTotalSymbols_leftUnchangedEvenIfPositionsLookReversed() {
        // Two 2-symbol "columns" in broken left-to-right order. Looks reversed geometrically,
        // but total symbol count can never support two well-fit (>=3 symbol) columns.
        val columnLeft = buildColumn("AB", startX = 0f)
        val columnRight = buildColumn("CD", startX = 40f)
        val input = columnLeft + columnRight

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        assertSame(input, result)
    }

    @Test
    fun oneUnderfilledColumnAmongOtherwiseValidOnes_bailsOnWholeBlock() {
        val columnLeft = buildColumn("ABCD", startX = 0f)
        // Only 2 symbols, below the minimum a column needs to be trusted.
        val shortColumn = buildColumn("EF", startX = 20f)
        val columnRight = buildColumn("GHIJ", startX = 40f)

        // Broken left-to-right order, otherwise a clear case for reordering.
        val input = columnLeft + shortColumn + columnRight

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        // A single low-confidence column disqualifies the whole block. Conservative by design.
        assertSame(input, result)
    }

    @Test
    fun columnsInNonMonotonicOrder_leftUnchanged() {
        val columnLeft = buildColumn("ABCD", startX = 0f)
        val columnMiddle = buildColumn("EFGH", startX = 20f)
        val columnRight = buildColumn("IJKL", startX = 40f)

        // Middle, then left, then right. Not a simple left-to-right reversal of a right-to-left
        // reading order, so this isn't the specific failure mode this class corrects.
        val input = columnMiddle + columnLeft + columnRight

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        assertSame(input, result)
    }

    @Test
    fun slightlyRotatedColumns_stillReorderedCorrectly() {
        // Each column drifts sideways by 0.3 char-widths per row, simulating a photo where the
        // text isn't perfectly axis-aligned. The per-column drift (0.3) stays within the
        // continuation tolerance, while the gap between columns (60 char-widths) stays far
        // outside it, so column boundaries should still be detected correctly.
        val columnLeft = buildColumn("ABCDE", startX = 0f, xDriftPerChar = 3f)
        val columnRight = buildColumn("FGHIJ", startX = 60f, xDriftPerChar = 3f)

        val input = columnLeft + columnRight

        val result = VerticalColumnReorderer.reorderIfNeeded(input)

        assertEquals("FGHIJABCDE", textOf(result))
    }
}
