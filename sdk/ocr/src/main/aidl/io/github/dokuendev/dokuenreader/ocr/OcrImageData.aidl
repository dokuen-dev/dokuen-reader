package io.github.dokuendev.dokuenreader.ocr;

import android.os.SharedMemory;

/**
 * Container for image data passed to OCR plugins.
 * 
 * Contains raw pixel data in SharedMemory for CPU-accessible processing.
 * 
 * FORMAT: The image is always in RGBA_8888 format (4 bytes per pixel).
 * SIZE: The SharedMemory size is always width * height * 4 bytes.
 * 
 * LIFECYCLE:
 * The plugin MUST call sharedMemory.close() when finished processing to release resources.
 * The plugin MAY retain the SharedMemory beyond the process() call for asynchronous
 * processing if needed.
 * 
 * CANCELLATION:
 * If the user cancels, the memory remains valid until the plugin calls close().
 * The plugin should close the SharedMemory when done, whether processing completed
 * successfully or was cancelled.
 */
parcelable OcrImageData {
    /**
     * Raw pixel data in shared memory.
     * 
     * The SharedMemory contains raw pixel data in RGBA_8888 format.
     * Size is always width * height * 4 bytes.
     * Use SharedMemory.mapReadOnly() to access the data.
     * 
     * IMPORTANT: The plugin MUST call close() when finished to release resources.
     */
    SharedMemory sharedMemory;
    
    /**
     * Image width in pixels.
     */
    int width;
    
    /**
     * Image height in pixels.
     */
    int height;
}
