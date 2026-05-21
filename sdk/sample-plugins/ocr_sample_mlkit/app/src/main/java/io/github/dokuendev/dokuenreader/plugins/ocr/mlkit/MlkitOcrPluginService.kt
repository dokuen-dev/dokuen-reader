package io.github.dokuendev.dokuenreader.plugins.ocr.mlkit

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import io.github.dokuendev.dokuenreader.ocr.OcrBlock
import io.github.dokuendev.dokuenreader.ocr.OcrErrorCode
import io.github.dokuendev.dokuenreader.ocr.OcrException
import io.github.dokuendev.dokuenreader.ocr.OcrPluginService
import io.github.dokuendev.dokuenreader.ocr.OcrRequirementKeys
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.ExecutionException

class MlkitOcrPluginService : OcrPluginService() {

    private var textRecognizer: TextRecognizer? = null

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
        putBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION, false)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
        putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja"))
    }

    override val configSchema = listOf(
        ConfigField(
            key = ConfigKey.HALF_RESOLUTION,
            displayName = "Half resolution",
            description = "Improve processing speed (and often accuracy too) if your text is large on screen. Leave off for reading things like web pages.",
            type = ConfigFieldType.BOOLEAN,
            defaultValue = "false",
            isRequired = false
        )
    )

    override suspend fun onInitialize(config: Bundle?): InitResult {
        try {
            textRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

            // Read user config for half resolution setting
            val useHalfResolution = config?.getString(ConfigKey.HALF_RESOLUTION) == "true"

            val requirements = Bundle().apply {
                putBoolean(OcrRequirementKeys.CONVERT_TO_GRAYSCALE, false)
                putBoolean(OcrRequirementKeys.DOWNSAMPLE, useHalfResolution)
            }
            return InitResultFactory.success(requirements)
        } catch (e: Exception) {
            throw OcrException(OcrErrorCode.INTERNAL_ERROR, "Failed to initialize ML Kit: ${e.message}")
        }
    }

    override suspend fun onProcessImage(
        bitmap: Bitmap,
        textDirection: String?
    ): List<OcrBlock> {
        val recognizer = textRecognizer
            ?: throw OcrException(OcrErrorCode.INTERNAL_ERROR, "TextRecognizer not initialized")

        try {
            currentCoroutineContext().ensureActive()

            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = Tasks.await(recognizer.process(image))

            currentCoroutineContext().ensureActive()

            val normX = 1.0f / bitmap.width
            val normY = 1.0f / bitmap.height

            val textBlocks = visionText?.textBlocks ?: emptyList()
            val ocrBlocks = textBlocks.map { block ->
                val allSymbols = block.lines.flatMap { line ->
                    line.elements.flatMap { element -> element.symbols }
                }

                val cleanedText = block.text.replace("\n", "")
                val alignedBounds = mutableListOf<RectF>()
                var symbolIndex = 0
                var currentSymbolText = ""
                var currentSymbolCharIndex = 0

                for (char in cleanedText) {
                    if (char.isWhitespace()) {
                        alignedBounds.add(RectF())
                        continue
                    }

                    while (currentSymbolCharIndex >= currentSymbolText.length && symbolIndex < allSymbols.size) {
                        currentSymbolText = allSymbols[symbolIndex].text
                        currentSymbolCharIndex = 0
                        if (currentSymbolText.isEmpty()) {
                            symbolIndex++
                        }
                    }

                    if (currentSymbolCharIndex < currentSymbolText.length) {
                        if (char == currentSymbolText[currentSymbolCharIndex]) {
                            val symbol = allSymbols[symbolIndex]
                            val rect = symbol.boundingBox
                            if (rect != null) {
                                alignedBounds.add(
                                    RectF(
                                        rect.left * normX,
                                        rect.top * normY,
                                        rect.right * normX,
                                        rect.bottom * normY
                                    )
                                )
                            } else {
                                alignedBounds.add(RectF())
                            }
                            currentSymbolCharIndex++
                            if (currentSymbolCharIndex >= currentSymbolText.length) {
                                symbolIndex++
                            }
                        } else {
                            alignedBounds.add(RectF())
                        }
                    } else {
                        alignedBounds.add(RectF())
                    }
                }
                OcrBlock(cleanedText, alignedBounds, isVertical = false)
            }

            return if (textDirection == "vertical") {
                convertToVerticalReading(ocrBlocks)
            } else {
                ocrBlocks
            }

        } catch (e: Exception) {
            Log.e(TAG, "ML Kit OCR failed", e)
            val unwrapped = if (e is ExecutionException) e.cause else e

            if (unwrapped is MlKitException) {
                val apiCode = when (unwrapped.errorCode) {
                    MlKitException.PERMISSION_DENIED -> OcrErrorCode.PERMISSION_DENIED
                    MlKitException.UNAVAILABLE -> OcrErrorCode.MODEL_UNAVAILABLE
                    MlKitException.INVALID_ARGUMENT -> OcrErrorCode.INVALID_ARGUMENT
                    MlKitException.INTERNAL -> OcrErrorCode.INTERNAL_ERROR
                    MlKitException.UNSUPPORTED -> OcrErrorCode.UNSUPPORTED
                    else -> OcrErrorCode.UNKNOWN_ERROR
                }
                throw OcrException(apiCode, unwrapped.message ?: "ML Kit error")
            } else {
                throw OcrException(OcrErrorCode.UNKNOWN_ERROR, e.message ?: "Unknown error")
            }
        }
    }

    override fun onShutdown() {
        textRecognizer?.close()
        textRecognizer = null
    }

    // -------------------------------------------------------------------------
    // Vertical text conversion
    // -------------------------------------------------------------------------

    internal data class ColumnInfo(
        val symbols: MutableList<Pair<Char, RectF>>,
        var leftBound: Float,
        var rightBound: Float
    )

    internal data class BlockInfo(
        val symbols: MutableList<Pair<Char, RectF>>,
        val bounds: RectF
    )

    internal fun convertToVerticalReading(ocrBlocks: List<OcrBlock>): List<OcrBlock> {
        val allSymbols = extractSymbolsFromBlocks(ocrBlocks)
        val columns = groupSymbolsIntoColumns(allSymbols)
        val sortedSymbols = getSortedSymbolsForVerticalReading(columns)
        val blocks = groupSymbolsIntoBlocks(sortedSymbols)
        return convertToOcrBlocks(blocks)
    }

    internal fun extractSymbolsFromBlocks(ocrBlocks: List<OcrBlock>): List<Pair<Char, RectF>> {
        val allSymbols = mutableListOf<Pair<Char, RectF>>()

        for (block in ocrBlocks) {
            val text = block.text
            val bounds = block.symbolBounds

            for (i in text.indices) {
                if (i < bounds.size) {
                    val char = text[i]
                    val bound = bounds[i]
                    if (!char.isWhitespace() && !bound.isEmpty) {
                        allSymbols.add(Pair(char, bound))
                    }
                }
            }
        }

        return allSymbols
    }

    internal fun groupSymbolsIntoColumns(allSymbols: List<Pair<Char, RectF>>): MutableList<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()

        if (allSymbols.isEmpty()) {
            return columns
        }

        val sortedSymbols = allSymbols.sortedBy { it.second.left }

        for (symbol in sortedSymbols) {
            val matchedColumnIndex = findMatchingColumn(columns, symbol)

            if (matchedColumnIndex != -1) {
                addSymbolToExistingColumn(columns, matchedColumnIndex, symbol)
            } else {
                createNewColumn(columns, symbol)
            }
        }

        return columns
    }

    internal fun findMatchingColumn(columns: List<ColumnInfo>, symbol: Pair<Char, RectF>): Int {
        val bounds = symbol.second
        val symbolLeft = bounds.left
        val symbolRight = bounds.right
        val symbolWidth = symbolRight - symbolLeft
        val threshold = symbolWidth * 0.5f
        val searchThreshold = symbolLeft - threshold

        for ((index, column) in columns.withIndex()) {
            if (column.leftBound > symbolRight + threshold) {
                break
            }

            if (column.rightBound >= searchThreshold && column.leftBound <= symbolRight + threshold) {
                val overlapStart = maxOf(symbolLeft, column.leftBound)
                val overlapEnd = minOf(symbolRight, column.rightBound)
                val overlapWidth = maxOf(0f, overlapEnd - overlapStart)

                val minWidth = minOf(symbolWidth, column.rightBound - column.leftBound)
                val overlapThreshold = minWidth * 0.5f

                if (overlapWidth > overlapThreshold) {
                    return index
                }
            }
        }

        return -1
    }

    internal fun addSymbolToExistingColumn(
        columns: MutableList<ColumnInfo>,
        columnIndex: Int,
        symbol: Pair<Char, RectF>
    ) {
        val column = columns[columnIndex]
        val bounds = symbol.second

        column.symbols.add(symbol)
        column.leftBound = minOf(column.leftBound, bounds.left)
        column.rightBound = maxOf(column.rightBound, bounds.right)

        if (columnIndex > 0 && columns[columnIndex].leftBound < columns[columnIndex - 1].leftBound) {
            val columnToMove = columns.removeAt(columnIndex)
            var insertPos = columnIndex - 1
            while (insertPos > 0 && columns[insertPos - 1].leftBound > columnToMove.leftBound) {
                insertPos--
            }
            columns.add(insertPos, columnToMove)
        }
    }

    internal fun createNewColumn(columns: MutableList<ColumnInfo>, symbol: Pair<Char, RectF>) {
        val bounds = symbol.second
        val newColumn = ColumnInfo(
            symbols = mutableListOf(symbol),
            leftBound = bounds.left,
            rightBound = bounds.right
        )

        var insertPos = columns.binarySearch { it.leftBound.compareTo(bounds.left) }
        if (insertPos < 0) {
            insertPos = -(insertPos + 1)
        }
        columns.add(insertPos, newColumn)
    }

    internal fun getSortedSymbolsForVerticalReading(columns: MutableList<ColumnInfo>): List<Pair<Char, RectF>> {
        for (column in columns) {
            column.symbols.sortBy { it.second.top }
        }
        val sortedColumns = columns.sortedByDescending { it.rightBound }
        return sortedColumns.flatMap { it.symbols }
    }

    internal fun groupSymbolsIntoBlocks(sortedSymbols: List<Pair<Char, RectF>>): List<BlockInfo> {
        val blocks = mutableListOf<BlockInfo>()
        val paddingFactor = 1.0f

        for (symbol in sortedSymbols) {
            val symbolBounds = symbol.second

            val symbolWidth = symbolBounds.width()
            val symbolHeight = symbolBounds.height()
            val inflatedBounds = RectF(
                symbolBounds.left - symbolWidth * paddingFactor,
                symbolBounds.top - symbolHeight * paddingFactor,
                symbolBounds.right + symbolWidth * paddingFactor,
                symbolBounds.bottom + symbolHeight * paddingFactor
            )

            var existingBlockIndex = -1
            for ((index, block) in blocks.withIndex()) {
                if (RectF.intersects(inflatedBounds, block.bounds)) {
                    existingBlockIndex = index
                    break
                }
            }

            if (existingBlockIndex != -1) {
                val block = blocks[existingBlockIndex]
                block.symbols.add(symbol)
                block.bounds.union(symbolBounds)
            } else {
                blocks.add(BlockInfo(mutableListOf(symbol), RectF(symbolBounds)))
            }
        }

        return blocks
    }

    internal fun convertToOcrBlocks(blocks: List<BlockInfo>): List<OcrBlock> {
        return blocks.map { block ->
            val blockText = block.symbols.map { it.first }.joinToString("")
            val blockBounds = block.symbols.map { it.second }
            OcrBlock(blockText, blockBounds, isVertical = true)
        }
    }

    /**
     * User-facing configuration keys defined in [configSchema].
     */
    private object ConfigKey {
        const val HALF_RESOLUTION = "half_resolution"
    }

    companion object {
        private const val TAG = "MlkitOcrPlugin"
    }
}
