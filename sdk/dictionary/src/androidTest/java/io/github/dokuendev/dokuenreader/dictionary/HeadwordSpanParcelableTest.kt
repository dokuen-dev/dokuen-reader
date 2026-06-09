package io.github.dokuendev.dokuenreader.dictionary

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for HeadwordSpan parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class HeadwordSpanParcelableTest {

    @Test
    fun headwordSpan_basicSpan_canBeParceled() {
        val original = HeadwordSpan().apply {
            startIndex = 0
            endIndex = 2
            linkUrl = "lookup:日本"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = HeadwordSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(2, unparceled.endIndex)
            assertEquals("lookup:日本", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun headwordSpan_actionLink_canBeParceled() {
        val original = HeadwordSpan().apply {
            startIndex = 5
            endIndex = 7
            linkUrl = "action:play_audio"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = HeadwordSpan.CREATOR.createFromParcel(parcel)

            assertEquals(5, unparceled.startIndex)
            assertEquals(7, unparceled.endIndex)
            assertEquals("action:play_audio", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun headwordSpan_externalLink_canBeParceled() {
        val original = HeadwordSpan().apply {
            startIndex = 0
            endIndex = 2
            linkUrl = "https://example.com"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = HeadwordSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(2, unparceled.endIndex)
            assertEquals("https://example.com", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun headwordSpan_largeIndices_canBeParceled() {
        val original = HeadwordSpan().apply {
            startIndex = 1000
            endIndex = 1050
            linkUrl = "lookup:word"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = HeadwordSpan.CREATOR.createFromParcel(parcel)

            assertEquals(1000, unparceled.startIndex)
            assertEquals(1050, unparceled.endIndex)
            assertEquals("lookup:word", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun headwordSpan_describeContents_returnsZero() {
        val span = HeadwordSpan()
        assertEquals(0, span.describeContents())
    }
}
