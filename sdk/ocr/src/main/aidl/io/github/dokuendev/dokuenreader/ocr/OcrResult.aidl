package io.github.dokuendev.dokuenreader.ocr;

import io.github.dokuendev.dokuenreader.ocr.OcrBlock;

/**
 * The complete result of OCR processing.
 */
parcelable OcrResult {
    /**
     * The list of OCR blocks, each containing recognized text and metadata.
     */
    OcrBlock[] ocrBlocks;
}
