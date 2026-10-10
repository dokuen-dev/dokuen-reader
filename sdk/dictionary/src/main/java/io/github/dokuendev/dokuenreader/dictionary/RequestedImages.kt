package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Bitmap
import android.os.SharedMemory

internal object RequestedImages {

    /**
     * Copies [image]'s pixels into a new ARGB_8888 Bitmap and closes its SharedMemory. Returns
     * null (after closing) when the dimensions are not positive, the buffer is not exactly
     * width * height * 4 bytes, or the bitmap cannot be created (for example, out of memory).
     */
    fun toBitmap(image: RequestedImage): Bitmap? {
        val memory = image.sharedMemory ?: return null
        try {
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return null
            val expected = width.toLong() * height * 4
            if (memory.size.toLong() != expected) return null
            val buffer = memory.mapReadOnly()
            try {
                val bitmap = androidx.core.graphics.createBitmap(width, height)
                try {
                    bitmap.copyPixelsFromBuffer(buffer)
                } catch (t: Throwable) {
                    bitmap.recycle()
                    throw t
                }
                return bitmap
            } finally {
                SharedMemory.unmap(buffer)
            }
        } catch (_: Exception) {
            return null
        } catch (_: OutOfMemoryError) {
            return null
        } finally {
            memory.close()
        }
    }
}
