package io.github.dokuendev.dokuenreader.ocr

import android.graphics.RectF
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for OcrBlock parcelable implementation.
 */
@RunWith(AndroidJUnit4::class)
class OcrBlockIntegrationTest {

    @Test
    fun ocrBlock_canBeParceledAndUnparceled() {
        val text = "テスト文字"
        val bounds = listOf(
            RectF(0f, 0f, 10f, 20f),
            RectF(10f, 0f, 20f, 20f),
            RectF(20f, 0f, 30f, 20f),
            RectF(30f, 0f, 40f, 20f),
            RectF(40f, 0f, 50f, 20f)
        )
        val isVertical = true

        val original = OcrBlock(text, bounds, isVertical)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrBlock.CREATOR.createFromParcel(parcel)

            assertEquals(original.text, unparceled.text)
            assertEquals(original.symbolBounds.size, unparceled.symbolBounds.size)
            for (i in original.symbolBounds.indices) {
                assertEquals(original.symbolBounds[i], unparceled.symbolBounds[i])
            }
            assertEquals(original.isVertical, unparceled.isVertical)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrBlock_horizontalText_canBeParceled() {
        val original = OcrBlock(
            text = "Horizontal text",
            symbolBounds = listOf(RectF(0f, 0f, 100f, 20f)),
            isVertical = false
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrBlock.CREATOR.createFromParcel(parcel)

            assertFalse(unparceled.isVertical)
            assertEquals("Horizontal text", unparceled.text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrBlock_verticalText_canBeParceled() {
        val original = OcrBlock(
            text = "縦書き",
            symbolBounds = listOf(
                RectF(0f, 0f, 20f, 20f),
                RectF(0f, 20f, 20f, 40f),
                RectF(0f, 40f, 20f, 60f)
            ),
            isVertical = true
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrBlock.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.isVertical)
            assertEquals("縦書き", unparceled.text)
            assertEquals(3, unparceled.symbolBounds.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrBlock_emptyBounds_canBeParceled() {
        val original = OcrBlock(
            text = "No bounds",
            symbolBounds = emptyList(),
            isVertical = false
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrBlock.CREATOR.createFromParcel(parcel)

            assertEquals("No bounds", unparceled.text)
            assertEquals(0, unparceled.symbolBounds.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrBlock_largeNumberOfBounds_canBeParceled() {
        val bounds = (0 until 100).map { i ->
            RectF(i * 10f, 0f, (i + 1) * 10f, 20f)
        }

        val original = OcrBlock(
            text = "A".repeat(100),
            symbolBounds = bounds,
            isVertical = false
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrBlock.CREATOR.createFromParcel(parcel)

            assertEquals(100, unparceled.symbolBounds.size)
            assertEquals(100, unparceled.text.length)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrBlock_describeContents_returnsZero() {
        val block = OcrBlock("test", emptyList(), false)
        assertEquals(0, block.describeContents())
    }
}
