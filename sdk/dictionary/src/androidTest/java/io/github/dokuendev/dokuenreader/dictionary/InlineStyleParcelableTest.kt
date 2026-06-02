package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for InlineStyle parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class InlineStyleParcelableTest {

    @Test
    fun inlineStyle_defaultValues_canBeParceled() {
        val original = InlineStyle()

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertFalse(unparceled.bold)
            assertFalse(unparceled.italic)
            assertEquals(0f, unparceled.fontSize, 0.001f)
            assertEquals(0, unparceled.foregroundColor)
            assertEquals(0, unparceled.textBackgroundColor)
            assertNull(unparceled.hoverText)
            assertNull(unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_boldOnly_canBeParceled() {
        val original = InlineStyle().apply {
            bold = true
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.bold)
            assertFalse(unparceled.italic)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_italicOnly_canBeParceled() {
        val original = InlineStyle().apply {
            italic = true
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertFalse(unparceled.bold)
            assertTrue(unparceled.italic)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_boldAndItalic_canBeParceled() {
        val original = InlineStyle().apply {
            bold = true
            italic = true
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.bold)
            assertTrue(unparceled.italic)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_fontSize_canBeParceled() {
        val original = InlineStyle().apply {
            fontSize = 1.5f
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(1.5f, unparceled.fontSize, 0.001f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_smallerFontSize_canBeParceled() {
        val original = InlineStyle().apply {
            fontSize = 0.8f
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(0.8f, unparceled.fontSize, 0.001f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_foregroundColor_canBeParceled() {
        val original = InlineStyle().apply {
            foregroundColor = Color.RED
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(Color.RED, unparceled.foregroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_textBackgroundColor_canBeParceled() {
        val original = InlineStyle().apply {
            textBackgroundColor = Color.YELLOW
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(Color.YELLOW, unparceled.textBackgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_bothColors_canBeParceled() {
        val original = InlineStyle().apply {
            foregroundColor = Color.WHITE
            textBackgroundColor = Color.BLACK
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(Color.WHITE, unparceled.foregroundColor)
            assertEquals(Color.BLACK, unparceled.textBackgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_allFieldsSet_canBeParceled() {
        val original = InlineStyle().apply {
            bold = true
            italic = true
            fontSize = 1.2f
            foregroundColor = Color.BLUE
            textBackgroundColor = Color.LTGRAY
            hoverText = "Pop-up description text"
            linkUrl = "https://example.com"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.bold)
            assertTrue(unparceled.italic)
            assertEquals(1.2f, unparceled.fontSize, 0.001f)
            assertEquals(Color.BLUE, unparceled.foregroundColor)
            assertEquals(Color.LTGRAY, unparceled.textBackgroundColor)
            assertEquals("Pop-up description text", unparceled.hoverText)
            assertEquals("https://example.com", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_describeContents_returnsZero() {
        val style = InlineStyle()
        assertEquals(0, style.describeContents())
    }
}
