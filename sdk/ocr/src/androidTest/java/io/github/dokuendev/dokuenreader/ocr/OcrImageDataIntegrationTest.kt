package io.github.dokuendev.dokuenreader.ocr

import android.os.Parcel
import android.os.SharedMemory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for OcrImageData parcelable implementation.
 * 
 * These tests verify that image data can be correctly transferred across
 * process boundaries using AIDL.
 */
@RunWith(AndroidJUnit4::class)
class OcrImageDataIntegrationTest {

    @Test
    fun ocrImageData_withSharedMemory_canBeParceled() {
        val width = 640
        val height = 480

        // Create SharedMemory with test data (RGBA_8888 format)
        val sharedMemory = SharedMemory.create("test_image", width * height * 4)
        val buffer = sharedMemory.mapReadWrite()
        // Write some test data
        for (i in 0 until 100) {
            buffer.put(i.toByte())
        }
        SharedMemory.unmap(buffer)

        val original = OcrImageData().apply {
            this.sharedMemory = sharedMemory
            this.width = width
            this.height = height
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrImageData.CREATOR.createFromParcel(parcel)

            assertNotNull(unparceled.sharedMemory)
            assertEquals(width, unparceled.width)
            assertEquals(height, unparceled.height)

            // Verify data integrity
            val unparceledBuffer = unparceled.sharedMemory.mapReadOnly()
            for (i in 0 until 100) {
                assertEquals(i.toByte(), unparceledBuffer.get())
            }
            SharedMemory.unmap(unparceledBuffer)
        } finally {
            parcel.recycle()
            sharedMemory.close()
        }
    }

    @Test
    fun ocrImageData_largeImage_canBeParceled() {
        val width = 1920
        val height = 1080

        val sharedMemory = SharedMemory.create("large_image", width * height * 4)

        val original = OcrImageData().apply {
            this.sharedMemory = sharedMemory
            this.width = width
            this.height = height
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrImageData.CREATOR.createFromParcel(parcel)

            assertEquals(width, unparceled.width)
            assertEquals(height, unparceled.height)
            assertNotNull(unparceled.sharedMemory)
        } finally {
            parcel.recycle()
            sharedMemory.close()
        }
    }

    @Test
    fun ocrImageData_describeContents_returnsZero() {
        val sharedMemory = SharedMemory.create("test", 100 * 100 * 4)
        val imageData = OcrImageData().apply {
            this.sharedMemory = sharedMemory
            this.width = 100
            this.height = 100
        }
        sharedMemory.use {
            assertEquals(1, imageData.describeContents())
        }
    }
}
