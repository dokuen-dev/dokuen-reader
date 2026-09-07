package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.graphics.RectF
import kotlin.math.abs

/**
 * Corrects GCP Vision's column order for vertical Japanese/Chinese text blocks.
 *
 * Vertical CJK text is read top-to-bottom and right-to-left. GCP Vision's symbol/word
 * segmentation usually emits the symbols in that correct order, but sometimes it lists the
 * columns backwards (left-to-right instead of right-to-left), especially if the image has
 * mixed horizontal and vertical text regions.
 *
 * This class detects that failure mode when columns are reversed, and reorders them. The symbol
 * order *within* each column, which is already correctly top-to-bottom, is never touched.
 * Detection and reordering both operate on raw symbol geometry rather than GCP's word/paragraph
 * groupings, because a "word" can straddle a column break, making its bounding box (which would
 * be the height of the full column, and two columns wide) unusable.
 *
 * Every stage below defaults to leaving the input untouched when the evidence is inconclusive.
 * This only ever fires on blocks that show strong, specific evidence of the reversed-columns
 * failure mode.
 */
internal object VerticalColumnReorderer {

    // A well-fit column needs enough points for its internal step pattern to be trustworthy
    // evidence (2 points always "fit" a line trivially). Two such columns are the minimum this
    // algorithm ever needs to justify reordering anything.
    private const val MIN_SYMBOLS_PER_COLUMN = 3
    private const val MIN_TOTAL_SYMBOLS = MIN_SYMBOLS_PER_COLUMN * 2

    // Endpoint-delta triage thresholds, in reference-char-width units. See [reorderIfNeeded].
    private const val SAFE_ALREADY_RTL_THRESHOLD_CHARS = 1.0f
    private const val SUSPICIOUS_LTR_THRESHOLD_CHARS = 1.0f

    // How far a step's perpendicular (column-relative) offset may drift from the column's own
    // recent local direction and still count as "the same column". Tolerant of a photo where
    // the text is at a slight angle, since it's measured against the column's own recent trend
    // rather than a fixed vertical axis.
    private const val MAX_PERPENDICULAR_DRIFT_CHAR_WIDTHS = 0.6f

    // A continuation step must move a meaningful distance along the column's own reading
    // direction; too small a step is noise (overlapping/adjacent symbol bounds), not evidence of
    // flowing within the same column.
    private const val MIN_STEP_CHAR_HEIGHTS = 0.3f

    // Number of trailing steps averaged to estimate a column's current local direction.
    private const val TREND_WINDOW = 4

    /**
     * Returns [symbols] with columns reordered right-to-left, or unchanged if there isn't enough
     * high-confidence evidence that reordering is needed and safe. [symbols] must be in GCP's
     * original order, one entry per GCP "symbol" (its text fragment, which is almost always one
     * character, paired with its bounding box).
     */
    fun reorderIfNeeded(symbols: List<Pair<String, RectF>>): List<Pair<String, RectF>> {
        if (symbols.size < MIN_TOTAL_SYMBOLS) return symbols

        val refCharWidth = symbols.map { it.second.width() }.average().toFloat()
        val refCharHeight = symbols.map { it.second.height() }.average().toFloat()
        if (refCharWidth <= 0f || refCharHeight <= 0f) return symbols

        // Cheap triage: compare the block's first and last symbols before doing any real work.
        val firstCenterX = symbols.first().second.centerX()
        val lastCenterX = symbols.last().second.centerX()
        val endpointDeltaInChars = (firstCenterX - lastCenterX) / refCharWidth

        // Last symbol comfortably to the LEFT of the first: reading already flowed right-to-left
        // overall. Confidently correct already.
        if (endpointDeltaInChars >= SAFE_ALREADY_RTL_THRESHOLD_CHARS) return symbols
        // No strong signal either way (e.g. a single column, where first/last land at nearly the
        // same X). Leave it alone rather than run the full check on weak evidence.
        if (endpointDeltaInChars > -SUSPICIOUS_LTR_THRESHOLD_CHARS) return symbols

        val columns = segmentIntoColumns(symbols, refCharWidth, refCharHeight)
        if (columns.size < 2) return symbols
        // Any column too short to trust its own internal consistency disqualifies the whole
        // block. Conservative by design; a partial/uncertain reorder is worse than none.
        if (columns.any { it.size < MIN_SYMBOLS_PER_COLUMN }) return symbols

        val columnCenters = columns.map { column -> column.map { it.second.centerX() }.average() }
        val isAscending = columnCenters.zipWithNext().all { (a, b) -> b > a }
        // Only correct the specific failure mode we're confident about: columns definitely
        // listed left-to-right. Any other order (already right-to-left, or something stranger)
        // is left untouched rather than guessed at.
        if (!isAscending) return symbols

        return columns.sortedByDescending { column -> column.map { it.second.centerX() }.average() }
            .flatten()
    }

    /**
     * Splits [symbols] into contiguous runs ("columns") by detecting large jumps between
     * consecutive symbols. A step continues the current column when it moves far enough along
     * the column's own recently established direction and stays within tolerance of that
     * direction perpendicular-wise; anything else starts a new column.
     */
    private fun segmentIntoColumns(
        symbols: List<Pair<String, RectF>>,
        refCharWidth: Float,
        refCharHeight: Float
    ): List<List<Pair<String, RectF>>> {
        val columns = mutableListOf<MutableList<Pair<String, RectF>>>()
        var current = mutableListOf(symbols[0])

        for (i in 1 until symbols.size) {
            val prevBounds = symbols[i - 1].second
            val currBounds = symbols[i].second
            val dxChars = (currBounds.centerX() - prevBounds.centerX()) / refCharWidth
            val dyChars = (currBounds.centerY() - prevBounds.centerY()) / refCharHeight

            val recentDx = recentAverageDx(current, refCharWidth)
            val isContinuation = dyChars > MIN_STEP_CHAR_HEIGHTS &&
                    abs(dxChars - recentDx) <= MAX_PERPENDICULAR_DRIFT_CHAR_WIDTHS

            if (isContinuation) {
                current.add(symbols[i])
            } else {
                columns.add(current)
                current = mutableListOf(symbols[i])
            }
        }
        columns.add(current)
        return columns
    }

    /** Average horizontal step (in char-width units) over the column's last few symbols. */
    private fun recentAverageDx(column: List<Pair<String, RectF>>, refCharWidth: Float): Float {
        if (column.size < 2) return 0f
        val window = column.takeLast(TREND_WINDOW + 1)
        var total = 0f
        for (i in 1 until window.size) {
            total += (window[i].second.centerX() - window[i - 1].second.centerX()) / refCharWidth
        }
        return total / (window.size - 1)
    }
}
