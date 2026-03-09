package io.github.dokuendev.dokuenreader.ocr;

import io.github.dokuendev.dokuenreader.ocr.OcrResult;

/**
 * Callback interface for asynchronous OCR processing results.
 * 
 * This callback is provided to plugins during the process() call and must be invoked
 * exactly once to report the OCR result back to the host app.
 * 
 * Usage Pattern:
 * 1. Plugin receives process(imageData, textDirection, callback)
 * 2. Plugin performs OCR processing (potentially on a background thread)
 * 3. Plugin calls EITHER callback.onSuccess() OR callback.onFailure()
 * 4. Plugin must NOT call both methods
 * 5. Plugin must NOT call the callback more than once per process() invocation
 * 
 * Threading:
 * - The callback can be invoked from any thread
 * - The host app handles thread safety internally
 * - Plugins using oneway process() receive the call on a Binder background thread
 *   and can invoke the callback synchronously from that same thread
 * 
 * Cancellation:
 * - If the user cancels the operation, the host calls cancel() and ignores subsequent callbacks
 * - Plugins MAY skip calling the callback after cancellation (host will ignore it anyway)
 * - Plugins SHOULD check cancellation flags to avoid wasted work
 * 
 * Example (Synchronous on Binder thread):
 * ```
 * override fun process(imageData: OcrImageData?, textDirection: String?, callback: IOcrCallback?) {
 *     try {
 *         val sharedMemory = imageData?.sharedMemory!!
 *         val buffer = sharedMemory.mapReadOnly()
 *         val blocks = ocrEngine.recognize(buffer, imageData.width, imageData.height)
 *         SharedMemory.unmap(buffer)
 *         
 *         val result = OcrResult(ocrBlocks = blocks)
 *         callback?.onSuccess(result)
 *     } catch (e: Exception) {
 *         callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, e.message ?: "Unknown error")
 *     }
 * }
 * ```
 * 
 * Example (Async with custom threading):
 * ```
 * override fun process(imageData: OcrImageData?, textDirection: String?, callback: IOcrCallback?) {
 *     executor.execute {
 *         try {
 *             val blocks = performOcr(imageData)
 *             callback?.onSuccess(OcrResult(ocrBlocks = blocks))
 *         } catch (e: Exception) {
 *             callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, e.message)
 *         }
 *     }
 * }
 * ```
 */
interface IOcrCallback {
    /**
     * Report successful OCR processing.
     * 
     * Call this method when the plugin has successfully:
     * - Processed the image data
     * - Extracted text and bounding boxes
     * - Constructed the OcrResult with all detected text blocks
     * 
     * The OcrResult should contain:
     * - ocrBlocks: Array of OcrBlock, each containing:
     *   - text: The recognized text string
     *   - symbolBounds: Array of RectF for each character's bounding box
     *   - isVertical: Whether the text is vertical (true) or horizontal (false)
     * 
     * Performance Considerations:
     * - The host app will validate the result size against security limits
     * - Extremely large results (>1000 blocks or >1MB text) may be rejected
     * - Consider merging small adjacent text regions to reduce block count
     * 
     * @param result OcrResult containing all detected text blocks with their positions.
     *               Must not be null. Can contain empty ocrBlocks array if no text detected.
     */
    void onSuccess(in OcrResult result);
    
    /**
     * Report OCR processing failure.
     * 
     * Call this method when processing fails for any reason, including:
     * - Invalid or corrupted image data
     * - Network errors (for cloud-based OCR)
     * - Authentication failures (invalid API key)
     * - OCR engine errors (model loading failed, processing timeout)
     * - Resource exhaustion (out of memory)
     * 
     * See [OcrErrorCode] for error codes.
     * 
     * Error Messages:
     * The error message should be:
     * - User-friendly and actionable (e.g., "Network unavailable" not "SocketException")
     * - Localized if possible based on the USER_LOCALE from initialize() config
     * - Specific enough for users to fix the problem
     *
     * The host app will display this message to the user in the OCR UI. If the error message is
     * empty or null, a generic message based on the error code will be used.
     * 
     * After calling onFailure(), the plugin should remain in a ready state.
     * The host app may call process() again with a new image.
     * 
     * @param errorCode Error code from [OcrErrorCode] indicating the failure type.
     *                  Use the most specific code available to help users diagnose issues.
     * @param errorMessage Human-readable error message describing what went wrong.
     *                     Should be localized based on USER_LOCALE if possible.
     */
    void onFailure(int errorCode, String errorMessage);
}

