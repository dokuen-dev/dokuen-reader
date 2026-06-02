package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BlockSpan parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class BlockSpanParcelableTest {

    @Test
    fun blockSpan_defaultValues_canBeParceled() {
        val original = BlockSpan()

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = BlockSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(0, unparceled.endIndex)
            assertEquals(0, unparceled.blockType)
            assertEquals(0, unparceled.indentLevel)
            assertNull(unparceled.listMarker)
            assertEquals(0, unparceled.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun blockSpan_listItem_canBeParceled() {
        val original = BlockSpan().apply {
            startIndex = 5
            endIndex = 25
            blockType = 1 // BLOCK_TYPE_LIST_ITEM
            indentLevel = 2
            listMarker = "•"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = BlockSpan.CREATOR.createFromParcel(parcel)

            assertEquals(5, unparceled.startIndex)
            assertEquals(25, unparceled.endIndex)
            assertEquals(1, unparceled.blockType)
            assertEquals(2, unparceled.indentLevel)
            assertEquals("•", unparceled.listMarker)
            assertEquals(0, unparceled.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun blockSpan_boxWithBackground_canBeParceled() {
        val original = BlockSpan().apply {
            startIndex = 0
            endIndex = 100
            blockType = 2 // BLOCK_TYPE_BOX
            indentLevel = 0
            backgroundColor = Color.LTGRAY
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = BlockSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(100, unparceled.endIndex)
            assertEquals(2, unparceled.blockType)
            assertEquals(0, unparceled.indentLevel)
            assertNull(unparceled.listMarker)
            assertEquals(Color.LTGRAY, unparceled.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun blockSpan_describeContents_returnsZero() {
        val span = BlockSpan()
        assertEquals(0, span.describeContents())
    }
}
