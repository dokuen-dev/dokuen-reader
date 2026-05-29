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
            assertEquals(0, unparceled.backgroundColor)
            assertEquals(0, unparceled.listItemOrdinal)
            assertEquals(0, unparceled.listIndentLevel)
            assertNull(unparceled.listMarkerOverride)
            assertFalse(unparceled.isBlock)
            assertFalse(unparceled.isTable)
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
    fun inlineStyle_backgroundColor_canBeParceled() {
        val original = InlineStyle().apply {
            backgroundColor = Color.YELLOW
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(Color.YELLOW, unparceled.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_bothColors_canBeParceled() {
        val original = InlineStyle().apply {
            foregroundColor = Color.WHITE
            backgroundColor = Color.BLACK
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(Color.WHITE, unparceled.foregroundColor)
            assertEquals(Color.BLACK, unparceled.backgroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_listItemOrdinal_canBeParceled() {
        val original = InlineStyle().apply {
            listItemOrdinal = 3
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(3, unparceled.listItemOrdinal)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_listIndentLevel_canBeParceled() {
        val original = InlineStyle().apply {
            listIndentLevel = 2
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(2, unparceled.listIndentLevel)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_listMarkerOverride_canBeParceled() {
        val original = InlineStyle().apply {
            listMarkerOverride = "• "
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals("• ", unparceled.listMarkerOverride)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_nullListMarkerOverride_canBeParceled() {
        val original = InlineStyle().apply {
            listMarkerOverride = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertNull(unparceled.listMarkerOverride)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_customListMarkers_canBeParceled() {
        val markers = listOf("α. ", "① ", "→ ", "★ ")

        markers.forEach { marker ->
            val original = InlineStyle().apply {
                listMarkerOverride = marker
            }

            val parcel = Parcel.obtain()
            try {
                original.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)

                val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

                assertEquals(marker, unparceled.listMarkerOverride)
            } finally {
                parcel.recycle()
            }
        }
    }

    @Test
    fun inlineStyle_allFieldsSet_canBeParceled() {
        val original = InlineStyle().apply {
            bold = true
            italic = true
            fontSize = 1.2f
            foregroundColor = Color.BLUE
            backgroundColor = Color.LTGRAY
            listItemOrdinal = 5
            listIndentLevel = 3
            listMarkerOverride = "→ "
            isBlock = true
            isTable = true
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
            assertEquals(Color.LTGRAY, unparceled.backgroundColor)
            assertEquals(5, unparceled.listItemOrdinal)
            assertEquals(3, unparceled.listIndentLevel)
            assertEquals("→ ", unparceled.listMarkerOverride)
            assertTrue(unparceled.isBlock)
            assertTrue(unparceled.isTable)
            assertEquals("Pop-up description text", unparceled.hoverText)
            assertEquals("https://example.com", unparceled.linkUrl)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun inlineStyle_bulletListSentinel_canBeParceled() {
        val original = InlineStyle().apply {
            listItemOrdinal = -1 // LIST_ITEM_BULLET
            listIndentLevel = 1
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InlineStyle.CREATOR.createFromParcel(parcel)

            assertEquals(-1, unparceled.listItemOrdinal)
            assertEquals(1, unparceled.listIndentLevel)
            assertNull(unparceled.listMarkerOverride)
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
