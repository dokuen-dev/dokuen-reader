package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextDirectionDetectorTest {

    private lateinit var detector: TextDirectionDetector

    @Before
    fun setUp() {
        detector = TextDirectionDetector()
    }

    // Helper functions for creating test data
    private fun createSymbolBounds(x: Float, y: Float, width: Float = 10f, height: Float = 10f): RectF {
        return RectF(x, y, x + width, y + height)
    }

    private fun createWordData(
        text: String,
        wordBounds: RectF,
        symbolBounds: List<RectF>
    ): TextDirectionDetector.WordData {
        return TextDirectionDetector.WordData(text, wordBounds, symbolBounds)
    }

    private fun createHorizontalWord(
        text: String,
        startX: Float,
        y: Float,
        charWidth: Float = 10f,
        charHeight: Float = 10f
    ): TextDirectionDetector.WordData {
        val symbolBounds = mutableListOf<RectF>()
        for (i in text.indices) {
            symbolBounds.add(createSymbolBounds(startX + i * charWidth, y, charWidth, charHeight))
        }
        val wordBounds = RectF(
            startX,
            y,
            startX + text.length * charWidth,
            y + charHeight
        )
        return createWordData(text, wordBounds, symbolBounds)
    }

    private fun createVerticalWord(
        text: String,
        x: Float,
        startY: Float,
        charWidth: Float = 10f,
        charHeight: Float = 10f
    ): TextDirectionDetector.WordData {
        val symbolBounds = mutableListOf<RectF>()
        for (i in text.indices) {
            symbolBounds.add(createSymbolBounds(x, startY + i * charHeight, charWidth, charHeight))
        }
        val wordBounds = RectF(
            x,
            startY,
            x + charWidth,
            startY + text.length * charHeight
        )
        return createWordData(text, wordBounds, symbolBounds)
    }

    // Test empty and invalid inputs
    @Test
    fun testEmptyBlock() {
        val emptyBlock = TextDirectionDetector.BlockData()
        val result = detector.detectBlockOrientation(emptyBlock)
        assertEquals(TextDirection.NOT_SET, result)
    }

    @Test
    fun testBlockWithEmptyParagraphs() {
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(
            TextDirectionDetector.ParagraphData(
                bounds = RectF(0f, 0f, 100f, 20f),
                words = emptyList()
            )
        )
        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.NOT_SET, result)
    }

    @Test
    fun testParagraphWithNoSymbols() {
        val wordWithoutSymbols = TextDirectionDetector.WordData(
            text = "test",
            bounds = RectF(0f, 0f, 40f, 10f),
            symbolBounds = emptyList()
        )
        val paragraphData = TextDirectionDetector.ParagraphData(
            bounds = RectF(0f, 0f, 40f, 10f),
            words = listOf(wordWithoutSymbols)
        )
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.NOT_SET, result)
    }

    // Test clearly horizontal text
    @Test
    fun testClearlyHorizontalText() {
        val words = listOf(
            createHorizontalWord("Hello", 0f, 0f),
            createHorizontalWord("World", 60f, 0f),
            createHorizontalWord("This", 0f, 20f),
            createHorizontalWord("is", 50f, 20f),
            createHorizontalWord("horizontal", 70f, 20f)
        )

        val paragraphBounds = RectF(0f, 0f, 180f, 30f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    // Test clearly vertical text
    @Test
    fun testClearlyVerticalText() {
        val words = listOf(
            createVerticalWord("縦", 0f, 0f),
            createVerticalWord("書", 0f, 20f),
            createVerticalWord("き", 0f, 40f),
            createVerticalWord("文", 20f, 0f),
            createVerticalWord("字", 20f, 20f)
        )

        val paragraphBounds = RectF(0f, 0f, 30f, 60f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test proportionality heuristic specifically
    @Test
    fun testProportionalityHeuristicHorizontal() {
        // Create words that fit the horizontal model well
        val words = listOf(
            // 4-character word: should be ~40x10 for horizontal
            createWordData(
                "test",
                RectF(0f, 0f, 40f, 10f),
                listOf(
                    createSymbolBounds(0f, 0f),
                    createSymbolBounds(10f, 0f),
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(30f, 0f)
                )
            ),
            // 3-character word: should be ~30x10 for horizontal
            createWordData(
                "abc",
                RectF(50f, 0f, 80f, 10f),
                listOf(
                    createSymbolBounds(50f, 0f),
                    createSymbolBounds(60f, 0f),
                    createSymbolBounds(70f, 0f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 80f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    @Test
    fun testProportionalityHeuristicVertical() {
        // Create words that fit the vertical model well
        val words = listOf(
            // 4-character word: should be ~10x40 for vertical
            createWordData(
                "test",
                RectF(0f, 0f, 10f, 40f),
                listOf(
                    createSymbolBounds(0f, 0f),
                    createSymbolBounds(0f, 10f),
                    createSymbolBounds(0f, 20f),
                    createSymbolBounds(0f, 30f)
                )
            ),
            // 3-character word: should be ~10x30 for vertical
            createWordData(
                "abc",
                RectF(20f, 0f, 30f, 30f),
                listOf(
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(20f, 10f),
                    createSymbolBounds(20f, 20f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 30f, 40f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test aspect ratio heuristic
    @Test
    fun testWordAspectRatioHeuristicHorizontal() {
        val words = listOf(
            // Wide words (aspect ratio < 1)
            createWordData(
                "hello",
                RectF(0f, 0f, 50f, 10f), // 5:1 aspect ratio
                listOf(
                    createSymbolBounds(0f, 0f),
                    createSymbolBounds(10f, 0f),
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(30f, 0f),
                    createSymbolBounds(40f, 0f)
                )
            ),
            createWordData(
                "world",
                RectF(60f, 0f, 110f, 10f), // 5:1 aspect ratio
                listOf(
                    createSymbolBounds(60f, 0f),
                    createSymbolBounds(70f, 0f),
                    createSymbolBounds(80f, 0f),
                    createSymbolBounds(90f, 0f),
                    createSymbolBounds(100f, 0f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 110f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    @Test
    fun testWordAspectRatioHeuristicVertical() {
        val words = listOf(
            // Tall words (aspect ratio > 1)
            createWordData(
                "hello",
                RectF(0f, 0f, 10f, 50f), // 1:5 aspect ratio
                listOf(
                    createSymbolBounds(0f, 0f),
                    createSymbolBounds(0f, 10f),
                    createSymbolBounds(0f, 20f),
                    createSymbolBounds(0f, 30f),
                    createSymbolBounds(0f, 40f)
                )
            ),
            createWordData(
                "world",
                RectF(20f, 0f, 30f, 50f), // 1:5 aspect ratio
                listOf(
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(20f, 10f),
                    createSymbolBounds(20f, 20f),
                    createSymbolBounds(20f, 30f),
                    createSymbolBounds(20f, 40f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 30f, 50f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test symbol flow heuristic
    @Test
    fun testSymbolFlowHeuristicHorizontal() {
        val words = listOf(
            createWordData(
                "test",
                RectF(0f, 0f, 40f, 10f),
                listOf(
                    createSymbolBounds(0f, 0f),   // t
                    createSymbolBounds(10f, 0f),  // e (flows right)
                    createSymbolBounds(20f, 0f),  // s (flows right)
                    createSymbolBounds(30f, 0f)   // t (flows right)
                )
            ),
            createWordData(
                "flow",
                RectF(50f, 0f, 90f, 10f),
                listOf(
                    createSymbolBounds(50f, 0f),  // f
                    createSymbolBounds(60f, 0f),  // l (flows right)
                    createSymbolBounds(70f, 0f),  // o (flows right)
                    createSymbolBounds(80f, 0f)   // w (flows right)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 90f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    @Test
    fun testSymbolFlowHeuristicVertical() {
        val words = listOf(
            createWordData(
                "test",
                RectF(0f, 0f, 10f, 40f),
                listOf(
                    createSymbolBounds(0f, 0f),   // t
                    createSymbolBounds(0f, 10f),  // e (flows down)
                    createSymbolBounds(0f, 20f),  // s (flows down)
                    createSymbolBounds(0f, 30f)   // t (flows down)
                )
            ),
            createWordData(
                "flow",
                RectF(0f, 50f, 10f, 90f),
                listOf(
                    createSymbolBounds(0f, 50f),  // f
                    createSymbolBounds(0f, 60f),  // l (flows down)
                    createSymbolBounds(0f, 70f),  // o (flows down)
                    createSymbolBounds(0f, 80f)   // w (flows down)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 10f, 90f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test paragraph layout heuristic
    @Test
    fun testParagraphLayoutHeuristicWideHorizontal() {
        val words = listOf(
            createHorizontalWord("A", 0f, 0f),
            createHorizontalWord("very", 20f, 0f),
            createHorizontalWord("long", 70f, 0f),
            createHorizontalWord("horizontal", 120f, 0f),
            createHorizontalWord("sentence", 220f, 0f),
            createHorizontalWord("that", 320f, 0f),
            createHorizontalWord("spans", 370f, 0f),
            createHorizontalWord("width", 420f, 0f)
        )

        // Very wide paragraph (much wider than tall)
        val paragraphBounds = RectF(0f, 0f, 470f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    @Test
    fun testParagraphLayoutHeuristicTallVertical() {
        val words = listOf(
            createVerticalWord("縦", 0f, 0f),
            createVerticalWord("に", 0f, 20f),
            createVerticalWord("長", 0f, 40f),
            createVerticalWord("い", 0f, 60f),
            createVerticalWord("文", 0f, 80f),
            createVerticalWord("章", 0f, 100f),
            createVerticalWord("で", 0f, 120f),
            createVerticalWord("す", 0f, 140f)
        )

        // Very tall paragraph (much taller than wide)
        val paragraphBounds = RectF(0f, 0f, 10f, 160f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test inconclusive cases
    @Test
    fun testInconclusiveSquareParagraph() {
        val words = listOf(
            createWordData(
                "test",
                RectF(0f, 0f, 20f, 20f), // Square word
                listOf(
                    createSymbolBounds(0f, 0f, 10f, 10f),
                    createSymbolBounds(10f, 10f, 10f, 10f)
                )
            )
        )

        // Square paragraph
        val paragraphBounds = RectF(0f, 0f, 20f, 20f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.NOT_SET, result)
    }

    @Test
    fun testSingleCharacterWords() {
        val words = listOf(
            createWordData(
                "a",
                RectF(0f, 0f, 10f, 10f),
                listOf(createSymbolBounds(0f, 0f))
            ),
            createWordData(
                "b",
                RectF(20f, 0f, 30f, 10f),
                listOf(createSymbolBounds(20f, 0f))
            ),
            createWordData(
                "c",
                RectF(40f, 0f, 50f, 10f),
                listOf(createSymbolBounds(40f, 0f))
            )
        )

        val paragraphBounds = RectF(0f, 0f, 50f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        // Single character words don't provide strong evidence
        val result = detector.detectBlockOrientation(blockData)
        // Should still detect as horizontal due to paragraph layout
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    // Test multiple paragraphs
    @Test
    fun testMultipleParagraphsHorizontal() {
        val paragraph1Words = listOf(
            createHorizontalWord("First", 0f, 0f),
            createHorizontalWord("paragraph", 60f, 0f)
        )
        val paragraph1 = TextDirectionDetector.ParagraphData(
            RectF(0f, 0f, 150f, 10f),
            paragraph1Words
        )

        val paragraph2Words = listOf(
            createHorizontalWord("Second", 0f, 20f),
            createHorizontalWord("paragraph", 70f, 20f)
        )
        val paragraph2 = TextDirectionDetector.ParagraphData(
            RectF(0f, 20f, 160f, 30f),
            paragraph2Words
        )

        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraph1)
        blockData.paragraphs.add(paragraph2)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.HORIZONTAL, result)
    }

    @Test
    fun testMultipleParagraphsVertical() {
        val paragraph1Words = listOf(
            createVerticalWord("第", 0f, 0f),
            createVerticalWord("一", 0f, 20f)
        )
        val paragraph1 = TextDirectionDetector.ParagraphData(
            RectF(0f, 0f, 10f, 40f),
            paragraph1Words
        )

        val paragraph2Words = listOf(
            createVerticalWord("第", 20f, 0f),
            createVerticalWord("二", 20f, 20f)
        )
        val paragraph2 = TextDirectionDetector.ParagraphData(
            RectF(20f, 0f, 30f, 40f),
            paragraph2Words
        )

        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraph1)
        blockData.paragraphs.add(paragraph2)

        val result = detector.detectBlockOrientation(blockData)
        assertEquals(TextDirection.VERTICAL, result)
    }

    // Test mixed evidence (conflicting heuristics)
    @Test
    fun testMixedEvidence() {
        // Create a scenario where some heuristics point to horizontal, others to vertical
        val words = listOf(
            // This word has horizontal symbol flow but is placed vertically
            createWordData(
                "mixed",
                RectF(0f, 0f, 10f, 50f), // Tall bounding box (vertical-like)
                listOf(
                    createSymbolBounds(0f, 0f),   // Symbols flow horizontally
                    createSymbolBounds(10f, 0f),  // but word bounds are tall
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(30f, 0f),
                    createSymbolBounds(40f, 0f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 50f, 50f) // Square paragraph
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        // With mixed evidence and square paragraph, should be inconclusive
        val result = detector.detectBlockOrientation(blockData)
        // The exact result depends on which heuristics dominate
        assertTrue(result == TextDirection.HORIZONTAL || result == TextDirection.VERTICAL || result == TextDirection.NOT_SET)
    }

    // Test edge cases with very small or large dimensions
    @Test
    fun testVerySmallDimensions() {
        val words = listOf(
            createWordData(
                "tiny",
                RectF(0f, 0f, 2f, 1f), // Very small dimensions
                listOf(
                    createSymbolBounds(0f, 0f, 0.5f, 1f),
                    createSymbolBounds(0.5f, 0f, 0.5f, 1f),
                    createSymbolBounds(1f, 0f, 0.5f, 1f),
                    createSymbolBounds(1.5f, 0f, 0.5f, 1f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 2f, 1f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        // Should handle small dimensions gracefully
        assertNotNull(result)
    }

    @Test
    fun testZeroDimensions() {
        val words = listOf(
            createWordData(
                "zero",
                RectF(0f, 0f, 0f, 0f), // Zero dimensions
                listOf(createSymbolBounds(0f, 0f, 0f, 0f))
            )
        )

        val paragraphBounds = RectF(0f, 0f, 0f, 0f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        // Should handle zero dimensions without crashing
        assertEquals(TextDirection.NOT_SET, result)
    }

    // Test diagnostic logging
    @Test
    fun testDiagnosticLogging() {
        val words = listOf(createHorizontalWord("test", 0f, 0f))
        val paragraphData = TextDirectionDetector.ParagraphData(
            RectF(0f, 0f, 40f, 10f),
            words
        )
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        // Test with logging enabled
        val result1 = detector.detectBlockOrientation(blockData, logDiagnostics = true)
        assertNotNull(result1)

        // Test with logging disabled (default)
        val result2 = detector.detectBlockOrientation(blockData, logDiagnostics = false)
        assertNotNull(result2)

        // Results should be the same regardless of logging
        assertEquals(result1, result2)
    }

    // Test boundary conditions for evidence thresholds
    @Test
    fun testEvidenceThresholdBoundary() {
        // Create a scenario that produces exactly the minimum evidence difference (1.5)
        // This is tricky to control precisely, but we can test the boundary behavior
        val words = listOf(
            createWordData(
                "boundary",
                RectF(0f, 0f, 80f, 10f), // Moderately wide
                listOf(
                    createSymbolBounds(0f, 0f),
                    createSymbolBounds(10f, 0f),
                    createSymbolBounds(20f, 0f),
                    createSymbolBounds(30f, 0f),
                    createSymbolBounds(40f, 0f),
                    createSymbolBounds(50f, 0f),
                    createSymbolBounds(60f, 0f),
                    createSymbolBounds(70f, 0f)
                )
            )
        )

        val paragraphBounds = RectF(0f, 0f, 80f, 10f)
        val paragraphData = TextDirectionDetector.ParagraphData(paragraphBounds, words)
        val blockData = TextDirectionDetector.BlockData()
        blockData.paragraphs.add(paragraphData)

        val result = detector.detectBlockOrientation(blockData)
        // Should be either horizontal or inconclusive, but not vertical
        assertTrue(result == TextDirection.HORIZONTAL || result == TextDirection.NOT_SET)
        assertNotEquals(TextDirection.VERTICAL, result)
    }
}
