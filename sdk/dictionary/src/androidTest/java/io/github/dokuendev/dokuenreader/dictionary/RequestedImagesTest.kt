package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.SharedMemory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks on a real device that the RGBA_8888 bytes the host sends become the right colors.
 * Robolectric cannot check this on every machine, since its native Bitmap does not always
 * store pixels in Android's byte order.
 */
@RunWith(AndroidJUnit4::class)
class RequestedImagesTest {

    @Test
    fun toBitmap_keepsChannelOrder() {
        val memory = SharedMemory.create("test", 2 * 1 * 4)
        val buffer = memory.mapReadWrite()
        buffer.put(byteArrayOf(0x11, 0x22, 0x33, 0xFF.toByte(), 0x44, 0x55, 0x66, 0xFF.toByte()))
        SharedMemory.unmap(buffer)
        val image = RequestedImage().apply { sharedMemory = memory; width = 2; height = 1 }

        val bitmap = RequestedImages.toBitmap(image)!!
        try {
            assertEquals(Color.argb(0xFF, 0x11, 0x22, 0x33), bitmap.getPixel(0, 0))
            assertEquals(Color.argb(0xFF, 0x44, 0x55, 0x66), bitmap.getPixel(1, 0))
        } finally {
            bitmap.recycle()
        }
    }
}
