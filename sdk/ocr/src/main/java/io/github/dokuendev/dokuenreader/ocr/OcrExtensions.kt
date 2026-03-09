package io.github.dokuendev.dokuenreader.ocr

import android.graphics.RectF

/**
 * Extension functions and convenience constructors for OCR API types.
 * These provide List-based APIs for better Kotlin ergonomics.
 * 
 * See the corresponding .aidl files for detailed field documentation.
 */

fun OcrBlock.copy(
    text: String = this.text,
    symbolBounds: List<RectF> = this.symbolBounds.toList(),
    isVertical: Boolean = this.isVertical
): OcrBlock {
    return OcrBlock(text, symbolBounds, isVertical)
}

fun OcrResult.copy(
    ocrBlocks: List<OcrBlock> = this.ocrBlocks.toList()
): OcrResult {
    return OcrResult(ocrBlocks)
}

fun OcrBlock(text: String, symbolBounds: List<RectF>, isVertical: Boolean): OcrBlock {
    return OcrBlock().apply {
        this.text = text
        this.symbolBounds = symbolBounds.toTypedArray()
        this.isVertical = isVertical
    }
}

fun OcrResult(ocrBlocks: List<OcrBlock>): OcrResult {
    return OcrResult().apply {
        this.ocrBlocks = ocrBlocks.toTypedArray()
    }
}

