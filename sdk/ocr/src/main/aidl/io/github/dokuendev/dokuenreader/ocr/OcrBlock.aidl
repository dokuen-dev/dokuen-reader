package io.github.dokuendev.dokuenreader.ocr;

/**
 * Represents a block of recognized text from OCR processing.
 */
parcelable OcrBlock {
    /**
     * The recognized text content.
     */
    String text;
    
    /**
     * Bounding rectangles for each character/symbol in the text.
     * The array size should match the text length.
     */
    android.graphics.RectF[] symbolBounds;
    
    /**
     * Whether the text is oriented vertically (true) or horizontally (false).
     */
    boolean isVertical;
}
