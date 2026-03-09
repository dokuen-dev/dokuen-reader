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
 * Integration tests for OcrResult parcelable implementation.
 */
@RunWith(AndroidJUnit4::class)
class OcrResultIntegrationTest {

    @Test
    fun ocrResult_canBeParceledAndUnparceled() {
        val blocks = listOf(
            OcrBlock("First block", listOf(RectF(0f, 0f, 100f, 20f)), false),
            OcrBlock("Second block", listOf(RectF(0f, 20f, 100f, 40f)), false),
            OcrBlock("Third block", listOf(RectF(0f, 40f, 100f, 60f)), true)
        )

        val original = OcrResult(blocks)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrResult.CREATOR.createFromParcel(parcel)

            assertEquals(original.ocrBlocks.size, unparceled.ocrBlocks.size)
            for (i in original.ocrBlocks.indices) {
                assertEquals(original.ocrBlocks[i].text, unparceled.ocrBlocks[i].text)
                assertEquals(original.ocrBlocks[i].isVertical, unparceled.ocrBlocks[i].isVertical)
            }
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrResult_emptyBlocks_canBeParceled() {
        val original = OcrResult(emptyList())

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrResult.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.ocrBlocks.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrResult_manyBlocks_canBeParceled() {
        val blocks = (1..50).map { i ->
            OcrBlock(
                text = "Block $i",
                symbolBounds = listOf(RectF(0f, i * 20f, 100f, (i + 1) * 20f)),
                isVertical = i % 2 == 0
            )
        }

        val original = OcrResult(blocks)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrResult.CREATOR.createFromParcel(parcel)

            assertEquals(50, unparceled.ocrBlocks.size)
            assertEquals("Block 1", unparceled.ocrBlocks[0].text)
            assertEquals("Block 50", unparceled.ocrBlocks[49].text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrResult_mixedOrientations_canBeParceled() {
        val blocks = listOf(
            OcrBlock("横書き", emptyList(), false),
            OcrBlock("縦書き", emptyList(), true),
            OcrBlock("Horizontal", emptyList(), false),
            OcrBlock("Vertical", emptyList(), true)
        )

        val original = OcrResult(blocks)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = OcrResult.CREATOR.createFromParcel(parcel)

            assertEquals(4, unparceled.ocrBlocks.size)
            assertFalse(unparceled.ocrBlocks[0].isVertical)
            assertTrue(unparceled.ocrBlocks[1].isVertical)
            assertFalse(unparceled.ocrBlocks[2].isVertical)
            assertTrue(unparceled.ocrBlocks[3].isVertical)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun ocrResult_describeContents_returnsZero() {
        val result = OcrResult(emptyList())
        assertEquals(0, result.describeContents())
    }
}
