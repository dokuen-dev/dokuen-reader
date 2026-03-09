package io.github.dokuendev.dokuenreader.plugins.ocr.mlkit

import android.graphics.Bitmap
import android.graphics.RectF
import io.github.dokuendev.dokuenreader.ocr.OcrBlock
import io.github.dokuendev.dokuenreader.ocr.OcrErrorCode
import io.github.dokuendev.dokuenreader.ocr.OcrException
import io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MlkitOcrPluginService business logic.
 * 
 * These tests verify:
 * - Configuration schema
 * - Capabilities reporting
 * - Initialization logic (mocked, as ML Kit requires real Android environment)
 * - Shutdown behavior
 * - Error handling
 * - Vertical text conversion logic
 * 
 * Note: Tests that require actual ML Kit initialization (onInitialize, onProcessImage)
 * are tested in the integration tests. Unit tests focus on the vertical text conversion
 * logic which can be tested independently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MlkitOcrPluginServiceTest {

    private lateinit var service: MlkitOcrPluginService

    @Before
    fun setup() {
        service = MlkitOcrPluginService()
    }

    // -------------------------------------------------------------------------
    // Configuration Tests
    // -------------------------------------------------------------------------

    @Test
    fun configSchema_hasHalfResolutionField() {
        val schema = service.configSchema

        assertEquals("Should have 1 config field", 1, schema.size)

        val halfResField = schema.find { it.key == "half_resolution" }
        assertNotNull("Should have half_resolution field", halfResField)
        assertEquals("Half resolution", halfResField!!.displayName)
        assertEquals(ConfigFieldType.BOOLEAN, halfResField.type)
        assertFalse("Should be optional", halfResField.isRequired)
        assertEquals("false", halfResField.defaultValue)
        assertTrue(
            "Description should mention speed",
            halfResField.description.contains("speed", ignoreCase = true)
        )
    }

    // -------------------------------------------------------------------------
    // Capabilities Tests
    // -------------------------------------------------------------------------

    @Test
    fun capabilities_reportsCorrectFeatures() {
        val capabilities = service.capabilities

        assertTrue(
            "Should support vertical text",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT)
        )
        assertFalse(
            "Should not support auto text direction",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION)
        )
        assertFalse(
            "Should not require internet",
            capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )
        assertTrue(
            "Should have custom config",
            capabilities.getBoolean(PluginCapabilityKeys.HAS_CUSTOM_CONFIG)
        )

        val languages = capabilities.getStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES)
        assertNotNull("Should have supported languages", languages)
        assertEquals("Should support 1 language", 1, languages!!.size)
        assertTrue("Should support Japanese", languages.contains("ja"))
    }

    @Test
    fun capabilities_doesNotSupportAutoTextDirection() {
        val capabilities = service.capabilities

        assertFalse(
            "MLKit requires explicit horizontal/vertical direction",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION)
        )
    }

    @Test
    fun capabilities_isOfflineOnly() {
        val capabilities = service.capabilities

        assertFalse(
            "MLKit works offline without internet",
            capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )
    }

    // -------------------------------------------------------------------------
    // Initialization Tests
    // -------------------------------------------------------------------------

    // Note: Actual initialization tests are in integration tests since ML Kit
    // requires a real Android environment. Unit tests focus on testable logic.

    @Test
    fun capabilities_doesNotRequireInternet() {
        // MLKit works offline, unlike cloud-based OCR services
        assertFalse(
            "MLKit works offline without internet",
            service.capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )
    }

    // -------------------------------------------------------------------------
    // Processing Error Tests
    // -------------------------------------------------------------------------

    @Test
    fun processImage_failsWhenNotInitialized() = runTest {
        // Don't initialize
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        try {
            service.onProcessImage(bitmap, "horizontal")
            fail("Should throw exception when not initialized")
        } catch (e: OcrException) {
            assertEquals(
                "Should return INTERNAL_ERROR",
                OcrErrorCode.INTERNAL_ERROR,
                e.errorCode
            )
            assertTrue(
                "Error should mention initialization",
                e.message!!.contains("not initialized", ignoreCase = true)
            )
        } finally {
            bitmap.recycle()
        }
    }

    // -------------------------------------------------------------------------
    // Shutdown Tests
    // -------------------------------------------------------------------------

    @Test
    fun shutdown_canBeCalledMultipleTimes() {
        // Don't initialize - just test shutdown is safe
        service.onShutdown()
        service.onShutdown() // Should not throw

        assertTrue("Multiple shutdowns should be safe", true)
    }

    @Test
    fun shutdown_canBeCalledWithoutInitialization() {
        // Should not throw even if never initialized
        service.onShutdown()

        assertTrue("Shutdown without init should be safe", true)
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - Symbol Extraction Tests
    // -------------------------------------------------------------------------

    @Test
    fun extractSymbols_handlesEmptyBlockList() {
        val result = service.extractSymbolsFromBlocks(emptyList())

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun extractSymbols_filtersWhitespace() {
        val blocks = listOf(
            OcrBlock(
                text = "A B",
                symbolBounds = listOf(
                    RectF(0.1f, 0.1f, 0.2f, 0.2f),
                    RectF(), // whitespace
                    RectF(0.3f, 0.1f, 0.4f, 0.2f)
                ),
                isVertical = false
            )
        )

        val result = service.extractSymbolsFromBlocks(blocks)

        assertEquals("Should extract only non-whitespace", 2, result.size)
        assertEquals('A', result[0].first)
        assertEquals('B', result[1].first)
    }

    @Test
    fun extractSymbols_filtersEmptyBounds() {
        val blocks = listOf(
            OcrBlock(
                text = "AB",
                symbolBounds = listOf(
                    RectF(0.1f, 0.1f, 0.2f, 0.2f),
                    RectF() // empty bound
                ),
                isVertical = false
            )
        )

        val result = service.extractSymbolsFromBlocks(blocks)

        assertEquals("Should filter empty bounds", 1, result.size)
        assertEquals('A', result[0].first)
    }

    @Test
    fun extractSymbols_handlesMultipleBlocks() {
        val blocks = listOf(
            OcrBlock(
                text = "AB",
                symbolBounds = listOf(
                    RectF(0.1f, 0.1f, 0.2f, 0.2f),
                    RectF(0.3f, 0.1f, 0.4f, 0.2f)
                ),
                isVertical = false
            ),
            OcrBlock(
                text = "CD",
                symbolBounds = listOf(
                    RectF(0.5f, 0.1f, 0.6f, 0.2f),
                    RectF(0.7f, 0.1f, 0.8f, 0.2f)
                ),
                isVertical = false
            )
        )

        val result = service.extractSymbolsFromBlocks(blocks)

        assertEquals("Should extract from all blocks", 4, result.size)
        assertEquals('A', result[0].first)
        assertEquals('B', result[1].first)
        assertEquals('C', result[2].first)
        assertEquals('D', result[3].first)
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - Column Grouping Tests
    // -------------------------------------------------------------------------

    @Test
    fun groupColumns_handlesEmptySymbols() {
        val result = service.groupSymbolsIntoColumns(emptyList())

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun groupColumns_createsSingleColumnForVerticalSymbols() {
        val symbols = listOf(
            Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair('B', RectF(0.1f, 0.3f, 0.2f, 0.4f)),
            Pair('C', RectF(0.1f, 0.5f, 0.2f, 0.6f))
        )

        val result = service.groupSymbolsIntoColumns(symbols)

        assertEquals("Should create one column", 1, result.size)
        assertEquals("Column should have 3 symbols", 3, result[0].symbols.size)
    }

    @Test
    fun groupColumns_createsMultipleColumnsForSeparatedSymbols() {
        val symbols = listOf(
            Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair('B', RectF(0.5f, 0.1f, 0.6f, 0.2f)) // Far apart horizontally
        )

        val result = service.groupSymbolsIntoColumns(symbols)

        assertEquals("Should create two columns", 2, result.size)
        assertEquals("First column should have 1 symbol", 1, result[0].symbols.size)
        assertEquals("Second column should have 1 symbol", 1, result[1].symbols.size)
    }

    @Test
    fun groupColumns_groupsOverlappingSymbols() {
        // Symbols with >50% horizontal overlap should be in same column
        val symbols = listOf(
            Pair('A', RectF(0.1f, 0.1f, 0.3f, 0.2f)),
            Pair('B', RectF(0.15f, 0.3f, 0.35f, 0.4f)) // 75% overlap
        )

        val result = service.groupSymbolsIntoColumns(symbols)

        assertEquals("Should create one column for overlapping symbols", 1, result.size)
        assertEquals("Column should have 2 symbols", 2, result[0].symbols.size)
    }

    @Test
    fun groupColumns_sortsColumnsLeftToRight() {
        val symbols = listOf(
            Pair('B', RectF(0.5f, 0.1f, 0.6f, 0.2f)), // Right column
            Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f))  // Left column
        )

        val result = service.groupSymbolsIntoColumns(symbols)

        assertEquals("Should have 2 columns", 2, result.size)
        assertTrue("First column should be leftmost", result[0].leftBound < result[1].leftBound)
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - Column Sorting Tests
    // -------------------------------------------------------------------------

    @Test
    fun sortColumns_sortsRightToLeft() {
        val columns = mutableListOf(
            MlkitOcrPluginService.ColumnInfo(
                symbols = mutableListOf(Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f))),
                leftBound = 0.1f,
                rightBound = 0.2f
            ),
            MlkitOcrPluginService.ColumnInfo(
                symbols = mutableListOf(Pair('B', RectF(0.5f, 0.1f, 0.6f, 0.2f))),
                leftBound = 0.5f,
                rightBound = 0.6f
            )
        )

        val result = service.getSortedSymbolsForVerticalReading(columns)

        assertEquals("Should have 2 symbols", 2, result.size)
        assertEquals("First symbol should be from rightmost column", 'B', result[0].first)
        assertEquals("Second symbol should be from leftmost column", 'A', result[1].first)
    }

    @Test
    fun sortColumns_sortsSymbolsTopToBottomWithinColumn() {
        val columns = mutableListOf(
            MlkitOcrPluginService.ColumnInfo(
                symbols = mutableListOf(
                    Pair('B', RectF(0.1f, 0.5f, 0.2f, 0.6f)), // Bottom
                    Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f))  // Top
                ),
                leftBound = 0.1f,
                rightBound = 0.2f
            )
        )

        val result = service.getSortedSymbolsForVerticalReading(columns)

        assertEquals("Should have 2 symbols", 2, result.size)
        assertEquals("First symbol should be from top", 'A', result[0].first)
        assertEquals("Second symbol should be from bottom", 'B', result[1].first)
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - Block Grouping Tests
    // -------------------------------------------------------------------------

    @Test
    fun groupBlocks_handlesEmptySymbols() {
        val result = service.groupSymbolsIntoBlocks(emptyList())

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun groupBlocks_createsSingleBlockForNearbySymbols() {
        val symbols = listOf(
            Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair('B', RectF(0.1f, 0.25f, 0.2f, 0.35f)) // Close vertically
        )

        val result = service.groupSymbolsIntoBlocks(symbols)

        assertEquals("Should create one block", 1, result.size)
        assertEquals("Block should have 2 symbols", 2, result.first().symbols.size)
    }

    @Test
    fun groupBlocks_createsMultipleBlocksForDistantSymbols() {
        val symbols = listOf(
            Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f)),
            Pair('B', RectF(0.1f, 0.8f, 0.2f, 0.9f)) // Far apart vertically
        )

        val result = service.groupSymbolsIntoBlocks(symbols)

        assertEquals("Should create two blocks", 2, result.size)
        assertEquals("First block should have 1 symbol", 1, result[0].symbols.size)
        assertEquals("Second block should have 1 symbol", 1, result[1].symbols.size)
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - OcrBlock Conversion Tests
    // -------------------------------------------------------------------------

    @Test
    fun convertToOcrBlocks_setsVerticalFlag() {
        val blocks = listOf(
            MlkitOcrPluginService.BlockInfo(
                symbols = mutableListOf(Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f))),
                bounds = RectF(0.1f, 0.1f, 0.2f, 0.2f)
            )
        )

        val result = service.convertToOcrBlocks(blocks)

        assertEquals("Should have 1 block", 1, result.size)
        assertTrue("Block should be marked as vertical", result[0].isVertical)
    }

    @Test
    fun convertToOcrBlocks_concatenatesText() {
        val blocks = listOf(
            MlkitOcrPluginService.BlockInfo(
                symbols = mutableListOf(
                    Pair('A', RectF(0.1f, 0.1f, 0.2f, 0.2f)),
                    Pair('B', RectF(0.1f, 0.3f, 0.2f, 0.4f)),
                    Pair('C', RectF(0.1f, 0.5f, 0.2f, 0.6f))
                ),
                bounds = RectF(0.1f, 0.1f, 0.2f, 0.6f)
            )
        )

        val result = service.convertToOcrBlocks(blocks)

        assertEquals("Should have 1 block", 1, result.size)
        assertEquals("Text should be concatenated", "ABC", result[0].text)
        assertEquals("Should have 3 bounds", 3, result[0].symbolBounds.size)
    }

    @Test
    fun convertToOcrBlocks_preservesBounds() {
        val expectedBounds = listOf(
            RectF(0.1f, 0.1f, 0.2f, 0.2f),
            RectF(0.1f, 0.3f, 0.2f, 0.4f)
        )
        val blocks = listOf(
            MlkitOcrPluginService.BlockInfo(
                symbols = mutableListOf(
                    Pair('A', expectedBounds[0]),
                    Pair('B', expectedBounds[1])
                ),
                bounds = RectF(0.1f, 0.1f, 0.2f, 0.4f)
            )
        )

        val result = service.convertToOcrBlocks(blocks)

        assertEquals("Should have correct number of bounds", expectedBounds.size, result[0].symbolBounds.size)
        for (i in expectedBounds.indices) {
            assertEquals("Bound $i should match", expectedBounds[i], result[0].symbolBounds[i])
        }
    }

    // -------------------------------------------------------------------------
    // Vertical Text Conversion - End-to-End Tests
    // -------------------------------------------------------------------------

    @Test
    fun convertToVertical_handlesEmptyBlocks() {
        val result = service.convertToVerticalReading(emptyList())

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun convertToVertical_marksBlocksAsVertical() {
        val blocks = listOf(
            OcrBlock(
                text = "ABC",
                symbolBounds = listOf(
                    RectF(0.1f, 0.1f, 0.2f, 0.2f),
                    RectF(0.1f, 0.3f, 0.2f, 0.4f),
                    RectF(0.1f, 0.5f, 0.2f, 0.6f)
                ),
                isVertical = false
            )
        )

        val result = service.convertToVerticalReading(blocks)

        assertTrue("Should have at least one block", result.isNotEmpty())
        assertTrue("All blocks should be marked vertical", result.all { it.isVertical })
    }
}
