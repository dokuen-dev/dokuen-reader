package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for StyledSpan parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class StyledSpanParcelableTest {

    @Test
    fun styledSpan_basicSpan_canBeParceled() {
        val style = InlineStyle().apply {
            bold = true
        }
        val original = StyledSpan().apply {
            startIndex = 0
            endIndex = 5
            this.style = style
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(5, unparceled.endIndex)
            assertNotNull(unparceled.style)
            assertTrue(unparceled.style.bold)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledSpan_withBoldStyle_canBeParceled() {
        val style = InlineStyle().apply {
            bold = true
            italic = false
        }
        val original = StyledSpan().apply {
            startIndex = 10
            endIndex = 20
            this.style = style
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledSpan.CREATOR.createFromParcel(parcel)

            assertEquals(10, unparceled.startIndex)
            assertEquals(20, unparceled.endIndex)
            assertTrue(unparceled.style.bold)
            assertFalse(unparceled.style.italic)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledSpan_withComplexStyle_canBeParceled() {
        val style = InlineStyle().apply {
            bold = true
            italic = true
            fontSize = 1.5f
            foregroundColor = Color.RED
            backgroundColor = Color.YELLOW
        }
        val original = StyledSpan().apply {
            startIndex = 5
            endIndex = 15
            this.style = style
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledSpan.CREATOR.createFromParcel(parcel)

            assertEquals(5, unparceled.startIndex)
            assertEquals(15, unparceled.endIndex)
            assertTrue(unparceled.style.bold)
            assertTrue(unparceled.style.italic)
            assertEquals(1.5f, unparceled.style.fontSize, 0.001f)
            assertEquals(Color.RED, unparceled.style.foregroundColor)
            assertEquals(Color.YELLOW, unparceled.style.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledSpan_withListStyle_canBeParceled() {
        val style = InlineStyle().apply {
            listItemOrdinal = 3
            listIndentLevel = 2
            listMarkerOverride = "• "
        }
        val original = StyledSpan().apply {
            startIndex = 0
            endIndex = 10
            this.style = style
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(10, unparceled.endIndex)
            assertEquals(3, unparceled.style.listItemOrdinal)
            assertEquals(2, unparceled.style.listIndentLevel)
            assertEquals("• ", unparceled.style.listMarkerOverride)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledSpan_largeIndices_canBeParceled() {
        val style = InlineStyle()
        val original = StyledSpan().apply {
            startIndex = 1000
            endIndex = 2000
            this.style = style
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledSpan.CREATOR.createFromParcel(parcel)

            assertEquals(1000, unparceled.startIndex)
            assertEquals(2000, unparceled.endIndex)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledSpan_describeContents_returnsZero() {
        val span = StyledSpan()
        assertEquals(0, span.describeContents())
    }
}
