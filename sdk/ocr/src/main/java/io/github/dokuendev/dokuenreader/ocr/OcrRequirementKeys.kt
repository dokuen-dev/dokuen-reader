package io.github.dokuendev.dokuenreader.ocr

/**
 * Keys for OCR plugins to use in the `InitResult.requirements` Bundle returned
 * by `IOcrService.initialize()`.
 *
 * These tell Dokuen what preprocessing to apply to the image before calling
 * `IOcrService.process()`.
 */
object OcrRequirementKeys {
    /**
     * Maximum supported bitmap width in pixels (Int).
     * If the captured bitmap is larger, Dokuen will downsample it while
     * preserving aspect ratio.
     */
    const val MAX_WIDTH = "max_width"

    /**
     * Maximum supported bitmap height in pixels (Int).
     * If the captured bitmap is larger, Dokuen will downsample it while
     * preserving aspect ratio.
     */
    const val MAX_HEIGHT = "max_height"

    /**
     * Requests Dokuen to resize the image to half resolution before sending
     * it to the plugin (Boolean).
     *
     * If true, downsampling happens before any additional resizing done to
     * accommodate `MAX_WIDTH` and `MAX_HEIGHT`.
     *
     * This can often improve both processing time and accuracy if you know in
     * advance that the text is relatively large on screen.
     */
    const val DOWNSAMPLE = "downsample"

    /**
     * Whether Dokuen should convert the bitmap to grayscale before sending
     * it to the plugin (Boolean).
     *
     * This is useful for reducing data size for cloud OCR APIs, but is often
     * not worth it for on-device models.
     */
    const val CONVERT_TO_GRAYSCALE = "convert_to_grayscale"
}
