package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.graphics.RectF
import android.util.Log
import kotlin.math.abs

/**
 * Infers whether a text block is horizontal or vertical from the geometry the GCP Vision API
 * reported for it.
 *
 * This only works to the extent Vision's own word/paragraph segmentation already grouped
 * symbols according to their true reading direction: a word's bounding box is tall and narrow
 * because Vision decided a vertical run of glyphs belongs together as one word, not because of
 * some orientation-neutral measurement. Heuristics 1, 2, and 4 read off the shape of that
 * grouping and don't care what order the grouped symbols/words are listed in. Heuristic 3
 * additionally assumes that listed order reflects true reading direction (it measures the
 * position delta from each symbol/word to the next).
 *
 * An OCR engine whose segmentation is itself orientation-blind (e.g. one that only ever groups
 * characters left-to-right) would give none of this useful signal: vertically arranged glyphs
 * would come back as isolated single-character "words" instead of multi-character ones, and
 * this detector would have nothing to measure. Vision's document text detection does segment by
 * true orientation, which is what makes this approach viable here.
 */
class TextDirectionDetector {

    var logDiagnostics: Boolean = false

    data class WordData(
        val text: String,
        val bounds: RectF,
        val symbolBounds: List<RectF>
    )

    data class ParagraphData(
        val bounds: RectF,
        val words: List<WordData>
    )

    data class BlockData(
        val paragraphs: MutableList<ParagraphData> = mutableListOf()
    )

    private data class Evidence(
        var horizontal: Double = 0.0,
        var vertical: Double = 0.0
    ) {
        fun add(other: Evidence) {
            this.horizontal += other.horizontal
            this.vertical += other.vertical
        }
    }

    /**
     * Detects the dominant text orientation (Vertical or Horizontal) for a text block.
     */
    fun detectBlockOrientation(blockData: BlockData, logDiagnostics: Boolean = false): TextDirection {
        if (blockData.paragraphs.isEmpty()) {
            return TextDirection.NOT_SET
        }

        this.logDiagnostics = logDiagnostics

        logInitialBlockInfo(blockData.paragraphs)

        val evidence = Evidence()

        // Process each paragraph, accumulating evidence for horizontal vs. vertical layout.
        for ((paragraphIndex, paragraphInfo) in blockData.paragraphs.withIndex()) {
            if (paragraphInfo.words.isEmpty()) continue

            log("\n--- Analyzing Paragraph $paragraphIndex ---")

            // Establish a reference character size for this paragraph.
            val (refCharWidth, refCharHeight, symbolCount) = calculateReferenceCharSize(paragraphInfo, paragraphIndex)
            if (symbolCount == 0) {
                log("  Skipping paragraph $paragraphIndex (No symbols)")
                continue
            }

            // Run heuristics and accumulate evidence.
            evidence.add(
                analyzeProportionality(paragraphInfo.words, refCharWidth, refCharHeight)
            )
            evidence.add(
                analyzeWordAspectRatios(paragraphInfo.words, refCharWidth, refCharHeight)
            )
            evidence.add(
                analyzeSymbolFlow(paragraphInfo.words, refCharWidth, refCharHeight)
            )
            evidence.add(
                analyzeParagraphLayout(
                    paragraphInfo,
                    symbolCount,
                    refCharWidth,
                    refCharHeight,
                    paragraphIndex
                )
            )
        }

        if (evidence.horizontal == 0.0 && evidence.vertical == 0.0) {
            log("Result: INCONCLUSIVE (No valid paragraphs processed)")
            return TextDirection.NOT_SET
        }

        return makeFinalDecision(evidence)
    }

    /**
     * Logs the full text content of the block to be analyzed.
     */
    private fun logInitialBlockInfo(paragraphData: List<ParagraphData>) {
        if (!logDiagnostics) return

        val fullBlockText = paragraphData.joinToString("\n") { paragraph ->
            paragraph.words.joinToString(" ") { it.text }
        }
        if (fullBlockText.isNotEmpty()) {
            log("--- Starting Orientation Detection ---")
            log("Block Text (${fullBlockText.length} chars, ${paragraphData.size} paragraphs):")
            log(fullBlockText.prependIndent("  > "))
            log("--- End of Block ---")
        } else {
            log("--- Starting Orientation Detection: Block is EMPTY ---")
        }
    }

    /**
     * Calculates the average character width and height for a given paragraph.
     */
    private fun calculateReferenceCharSize(
        paragraphInfo: ParagraphData,
        paragraphIndex: Int
    ): Triple<Double, Double, Int> {
        val allSymbolBounds = paragraphInfo.words.flatMap { it.symbolBounds }
        if (allSymbolBounds.isEmpty()) {
            return Triple(0.0, 0.0, 0)
        }

        val refCharWidth = allSymbolBounds.sumOf { it.width().toDouble() } / allSymbolBounds.size
        val refCharHeight = allSymbolBounds.sumOf { it.height().toDouble() } / allSymbolBounds.size

        log(
            "  Paragraph $paragraphIndex reference character size (w×h): " +
                    "${refCharWidth.format(3)} × ${refCharHeight.format(3)}"
        )
        return Triple(refCharWidth, refCharHeight, allSymbolBounds.size)
    }

    /**
     * HEURISTIC 1: Compares a word's bounding box to expected dimensions based on symbol count.
     * A horizontal word should be N times the reference width, while a vertical one should be N times the reference height.
     */
    private fun analyzeProportionality(
        words: List<WordData>,
        refCharWidth: Double,
        refCharHeight: Double
    ): Evidence {
        val multiSymbolWords = words.filter { it.symbolBounds.size > 1 }

        if (multiSymbolWords.isEmpty()) return Evidence()

        var totalHorizontalError = 0.0
        var totalVerticalError = 0.0

        for (word in multiSymbolWords) {
            val wordWidth = word.bounds.width().toDouble()
            val wordHeight = word.bounds.height().toDouble()
            val symbolCount = word.symbolBounds.size

            // Horizontal model error
            val expectedWidthH = symbolCount * refCharWidth
            val expectedHeightH = refCharHeight
            totalHorizontalError += abs(wordWidth - expectedWidthH) / refCharWidth + abs(wordHeight - expectedHeightH) / refCharHeight

            // Vertical model error
            val expectedHeightV = symbolCount * refCharHeight
            val expectedWidthV = refCharWidth
            totalVerticalError += abs(wordHeight - expectedHeightV) / refCharHeight + abs(wordWidth - expectedWidthV) / refCharWidth
        }

        val avgHorizontalError = totalHorizontalError / multiSymbolWords.size
        val avgVerticalError = totalVerticalError / multiSymbolWords.size
        log(
            "  [Heuristic 1: Proportionality] Avg Error in char units (H/V): " +
                    "${avgHorizontalError.format(3)} / ${avgVerticalError.format(3)}"
        )

        val evidence = Evidence()

        val errorRatio = if (avgVerticalError > 0) avgHorizontalError / avgVerticalError else 1.0
        when {
            errorRatio < 0.5 -> {
                evidence.horizontal += 3.0
                log("    +3.0 H-evidence (horizontal model fits much better)")
            }

            errorRatio < 0.7 -> {
                evidence.horizontal += 1.5
                log("    +1.5 H-evidence (horizontal model fits better)")
            }

            errorRatio > 2.0 -> {
                evidence.vertical += 3.0
                log("    +3.0 V-evidence (vertical model fits much better)")
            }

            errorRatio > 1.43 -> {
                evidence.vertical += 1.5
                log("    +1.5 V-evidence (vertical model fits better)")
            }
        }
        return evidence
    }

    /**
     * HEURISTIC 2: For square-like characters (e.g., CJK), analyzes word aspect ratios.
     * Vertical words should be tall and thin, horizontal words should be wide and short.
     */
    private fun analyzeWordAspectRatios(
        words: List<WordData>,
        refCharWidth: Double,
        refCharHeight: Double
    ): Evidence {
        var verticalOrientedWords = 0
        var horizontalOrientedWords = 0

        // A word must have at least two characters for its aspect ratio to be a meaningful indicator of orientation.
        val eligibleWords =
            words.filter { it.symbolBounds.size >= 2 && it.bounds.width() > 0 && it.bounds.height() > 0 }

        for (word in eligibleWords) {
            val wordWidthInCharUnits = word.bounds.width() / refCharWidth
            val wordHeightInCharUnits = word.bounds.height() / refCharHeight
            val wordAspectRatio = wordHeightInCharUnits / wordWidthInCharUnits

            val numChars = word.symbolBounds.size

            // In our corrected space, a single character is effectively 1x1.
            // A horizontal word of N chars is ~Nx1 -> aspect ratio = 1/N.
            // A vertical word of N chars is ~1xN -> aspect ratio = N.
            val expectedHorizontalAspect = 1.0 / numChars
            val expectedVerticalAspect = numChars.toDouble()

            val horizontalModelRatio = wordAspectRatio / expectedHorizontalAspect
            val verticalModelRatio = wordAspectRatio / expectedVerticalAspect

            // The "error" is how far the ratio is from 1.0. We use max(ratio, 1/ratio) for a symmetrical distance.
            // For example, if ratio is 2, error is 2. If ratio is 0.5, error is 1/0.5 = 2.
            // A perfect match results in an error of 1.0.
            val horizontalError = maxOf(horizontalModelRatio, 1.0 / horizontalModelRatio)
            val verticalError = maxOf(verticalModelRatio, 1.0 / verticalModelRatio)

            // A tolerance is used to determine which orientation is a better fit.
            // One model must be significantly better (e.g., have an error that is at least 30% smaller).
            if (verticalError < horizontalError * 0.7) {
                verticalOrientedWords++
            } else if (horizontalError < verticalError * 0.7) {
                horizontalOrientedWords++
            }
        }

        if (eligibleWords.isNotEmpty()) {
            val verticalRatio = verticalOrientedWords.toDouble() / eligibleWords.size
            val horizontalRatio = horizontalOrientedWords.toDouble() / eligibleWords.size
            log("    [Heuristic 2: Word Aspect] V-oriented: $verticalOrientedWords, H-oriented: $horizontalOrientedWords (of ${eligibleWords.size})")

            when {
                verticalRatio >= 0.6 && verticalRatio > horizontalRatio * 1.5 -> {
                    log("      +2.0 V-evidence (strong vertical aspect pattern)")
                    return Evidence(0.0, 2.0)
                }

                horizontalRatio >= 0.6 && horizontalRatio > verticalRatio * 1.5 -> {
                    log("      +2.0 H-evidence (strong horizontal aspect pattern)")
                    return Evidence(2.0, 0.0)
                }
            }
        }
        return Evidence()
    }

    /**
     * HEURISTIC 3: Analyzes the direction of flow from one symbol/word to the next.
     *
     * Unlike the other heuristics, this one assumes the engine listed symbols/words in true
     * reading order; it does not just read off bounding-box shape.
     */
    private fun analyzeSymbolFlow(
        words: List<WordData>,
        refCharWidth: Double,
        refCharHeight: Double
    ): Evidence {
        log("    [Heuristic 3: Symbol Flow Analysis]")
        var horizontalFlows = 0
        var verticalFlows = 0
        var totalFlows = 0

        // Intra-word symbol flow
        for (word in words.filter { it.symbolBounds.size >= 2 }) {
            for (i in 0 until word.symbolBounds.size - 1) {
                val current = word.symbolBounds[i]
                val next = word.symbolBounds[i + 1]
                val (dx, dy) = Pair((next.left - current.left) / refCharWidth, (next.top - current.top) / refCharHeight)

                totalFlows++
                when {
                    abs(dx) > 0.7 && abs(dy) < 0.7 -> {
                        horizontalFlows++
                        log("      Symbol $i -> ${i + 1}: Horizontal flow (${dx.format(1)} chars)")
                    }

                    abs(dy) > 0.7 && abs(dx) < 0.7 -> {
                        verticalFlows++
                        log("      Symbol $i -> ${i + 1}: Vertical flow (${dy.format(1)} chars)")
                    }
                }
            }
        }

        // Inter-word flow (word to word)
        for (i in 0 until words.size - 1) {
            val current = words[i]
            val next = words[i + 1]
            val (dx, dy) = Pair(
                (next.bounds.left - current.bounds.left) / refCharWidth,
                (next.bounds.top - current.bounds.top) / refCharHeight
            )

            totalFlows++
            when {
                dy > 0.8 && dx < -1.0 -> { // Horizontal line break (down and left)
                    horizontalFlows++
                    log("      '${current.text}' -> '${next.text}': Horizontal line break")
                }

                dx < -0.8 && dy < -1.0 -> { // Vertical line break (left and up)
                    verticalFlows++
                    log("      '${current.text}' -> '${next.text}': Vertical line break")
                }

                dx > 0.8 && abs(dy) < 0.5 -> { // Normal horizontal flow
                    horizontalFlows++
                    log("      '${current.text}' -> '${next.text}': Horizontal word flow")
                }

                dy > 0.8 && abs(dx) < 0.5 -> { // Normal vertical flow
                    verticalFlows++
                    log("      '${current.text}' -> '${next.text}': Vertical word flow")
                }
            }
        }

        if (totalFlows > 0) {
            val hRatio = horizontalFlows.toDouble() / totalFlows
            val vRatio = verticalFlows.toDouble() / totalFlows
            log("      Flow analysis: H=$horizontalFlows, V=$verticalFlows (of $totalFlows)")

            return when {
                hRatio >= 0.7 -> Evidence(4.0, 0.0).also { log("        +4.0 H-evidence (strong horizontal flow)") }
                hRatio >= 0.5 -> Evidence(2.0, 0.0).also { log("        +2.0 H-evidence (moderate horizontal flow)") }
                vRatio >= 0.7 -> Evidence(0.0, 4.0).also { log("        +4.0 V-evidence (strong vertical flow)") }
                vRatio >= 0.5 -> Evidence(0.0, 2.0).also { log("        +2.0 V-evidence (moderate vertical flow)") }
                else -> Evidence()
            }
        }
        return Evidence()
    }

    /**
     * HEURISTIC 4: Analyzes the overall aspect ratio of the paragraph's bounding box.
     * Long, wide paragraphs are strongly horizontal; tall, thin ones are strongly vertical.
     */
    private fun analyzeParagraphLayout(
        pData: ParagraphData,
        symbolCount: Int,
        refCharWidth: Double,
        refCharHeight: Double,
        pIndex: Int
    ): Evidence {
        if (symbolCount <= 3) return Evidence()

        val widthInChars = pData.bounds.width() / refCharWidth
        val heightInChars = pData.bounds.height() / refCharHeight
        log(
            "    [Heuristic 4: Paragraph Layout] Para $pIndex: $symbolCount symbols in " +
                    "${widthInChars.format(1)}×${heightInChars.format(1)} chars"
        )

        return when {
            widthInChars > heightInChars * 4.0 -> Evidence(
                5.0,
                0.0
            ).also { log("      +5.0 H-evidence (UNAMBIGUOUS: paragraph much wider than tall)") }

            heightInChars > widthInChars * 4.0 -> Evidence(
                0.0,
                5.0
            ).also { log("      +5.0 V-evidence (UNAMBIGUOUS: paragraph much taller than wide)") }

            widthInChars > heightInChars * 2.0 -> Evidence(
                2.5,
                0.0
            ).also { log("      +2.5 H-evidence (strong: paragraph wider than tall)") }

            heightInChars > widthInChars * 2.0 -> Evidence(
                0.0,
                2.5
            ).also { log("      +2.5 V-evidence (strong: paragraph taller than wide)") }

            widthInChars > heightInChars * 1.5 -> Evidence(
                1.0,
                0.0
            ).also { log("      +1.0 H-evidence (moderate: paragraph somewhat wider)") }

            heightInChars > widthInChars * 1.5 -> Evidence(
                0.0,
                1.0
            ).also { log("      +1.0 V-evidence (moderate: paragraph somewhat taller)") }

            else -> Evidence()
        }
    }

    /**
     * Makes the final orientation decision based on the accumulated evidence scores.
     */
    private fun makeFinalDecision(evidence: Evidence): TextDirection {
        val evidenceDifference = abs(evidence.vertical - evidence.horizontal)
        val minEvidenceThreshold = 1.5

        log("\n--- Final Analysis for block ---")
        log(
            "Final Evidence (V/H): ${evidence.vertical.format(1)} / ${evidence.horizontal.format(1)} " +
                    "(Difference: ${evidenceDifference.format(1)})"
        )

        return when {
            evidence.vertical > evidence.horizontal && evidenceDifference >= minEvidenceThreshold -> {
                log("Result: VERTICAL (Threshold: $minEvidenceThreshold)")
                TextDirection.VERTICAL
            }

            evidence.horizontal > evidence.vertical && evidenceDifference >= minEvidenceThreshold -> {
                log("Result: HORIZONTAL (Threshold: $minEvidenceThreshold)")
                TextDirection.HORIZONTAL
            }

            else -> {
                log("Result: INCONCLUSIVE (Difference below threshold $minEvidenceThreshold)")
                TextDirection.NOT_SET
            }
        }
    }

    private fun log(message: String) {
        if (logDiagnostics) {
            Log.d(TAG, message)
        }
    }

    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

    companion object {
        private const val TAG = "TextDirectionDetector"
    }
}
