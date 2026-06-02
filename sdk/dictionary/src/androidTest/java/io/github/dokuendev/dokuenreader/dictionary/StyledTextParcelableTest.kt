package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for StyledText parcelable implementation.
 * Tests parcelable write/read round-trip for all fields, null handling, and array serialization.
 */
@RunWith(AndroidJUnit4::class)
class StyledTextParcelableTest {

    @Test
    fun styledText_plainText_canBeParceled() {
        val original = StyledText().apply {
            text = "Simple plain text"
            blockSpans = null
            styledSpans = null
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("Simple plain text", unparceled.text)
            assertNull(unparceled.blockSpans)
            assertNull(unparceled.styledSpans)
            assertNull(unparceled.rubySpans)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_withBlockSpans_canBeParceled() {
        val original = StyledText().apply {
            text = "List item text"
            blockSpans = arrayOf(
                BlockSpan().apply {
                    startIndex = 0
                    endIndex = 14
                    blockType = 1 // BLOCK_TYPE_LIST_ITEM
                    indentLevel = 1
                    listMarker = "•"
                }
            )
            styledSpans = null
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("List item text", unparceled.text)
            assertNotNull(unparceled.blockSpans)
            assertEquals(1, unparceled.blockSpans!!.size)
            assertEquals(0, unparceled.blockSpans!![0].startIndex)
            assertEquals(14, unparceled.blockSpans!![0].endIndex)
            assertEquals(1, unparceled.blockSpans!![0].blockType)
            assertEquals(1, unparceled.blockSpans!![0].indentLevel)
            assertEquals("•", unparceled.blockSpans!![0].listMarker)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_withStyledSpans_canBeParceled() {
        val style1 = InlineStyle().apply { bold = true }
        val style2 = InlineStyle().apply { italic = true }

        val original = StyledText().apply {
            text = "Bold and italic text"
            styledSpans = arrayOf(
                StyledSpan().apply {
                    startIndex = 0
                    endIndex = 4
                    style = style1
                },
                StyledSpan().apply {
                    startIndex = 9
                    endIndex = 15
                    style = style2
                }
            )
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("Bold and italic text", unparceled.text)
            assertNotNull(unparceled.styledSpans)
            assertEquals(2, unparceled.styledSpans!!.size)

            assertEquals(0, unparceled.styledSpans!![0].startIndex)
            assertEquals(4, unparceled.styledSpans!![0].endIndex)
            assertTrue(unparceled.styledSpans!![0].style.bold)

            assertEquals(9, unparceled.styledSpans!![1].startIndex)
            assertEquals(15, unparceled.styledSpans!![1].endIndex)
            assertTrue(unparceled.styledSpans!![1].style.italic)

            assertNull(unparceled.rubySpans)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_withRubySpans_canBeParceled() {
        val original = StyledText().apply {
            text = "漢字"
            styledSpans = null
            rubySpans = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 2
                    rubyText = "かんじ"
                }
            )
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("漢字", unparceled.text)
            assertNull(unparceled.styledSpans)
            assertNotNull(unparceled.rubySpans)
            assertEquals(1, unparceled.rubySpans!!.size)
            assertEquals(0, unparceled.rubySpans!![0].startIndex)
            assertEquals(2, unparceled.rubySpans!![0].endIndex)
            assertEquals("かんじ", unparceled.rubySpans!![0].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_withAllSpanTypes_canBeParceled() {
        val style = InlineStyle().apply {
            bold = true
            foregroundColor = Color.BLUE
        }

        val original = StyledText().apply {
            text = "漢字 with formatting"
            blockSpans = arrayOf(
                BlockSpan().apply {
                    startIndex = 0
                    endIndex = 20
                    blockType = 0
                }
            )
            styledSpans = arrayOf(
                StyledSpan().apply {
                    startIndex = 0
                    endIndex = 2
                    this.style = style
                }
            )
            rubySpans = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 2
                    rubyText = "かんじ"
                }
            )
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("漢字 with formatting", unparceled.text)

            assertNotNull(unparceled.blockSpans)
            assertEquals(1, unparceled.blockSpans!!.size)

            assertNotNull(unparceled.styledSpans)
            assertEquals(1, unparceled.styledSpans!!.size)
            assertTrue(unparceled.styledSpans!![0].style.bold)
            assertEquals(Color.BLUE, unparceled.styledSpans!![0].style.foregroundColor)

            assertNotNull(unparceled.rubySpans)
            assertEquals(1, unparceled.rubySpans!!.size)
            assertEquals("かんじ", unparceled.rubySpans!![0].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_emptyArrays_canBeParceled() {
        val original = StyledText().apply {
            text = "Text with empty arrays"
            blockSpans = arrayOf()
            styledSpans = arrayOf()
            rubySpans = arrayOf()
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("Text with empty arrays", unparceled.text)
            assertNotNull(unparceled.blockSpans)
            assertEquals(0, unparceled.blockSpans!!.size)
            assertNotNull(unparceled.styledSpans)
            assertEquals(0, unparceled.styledSpans!!.size)
            assertNotNull(unparceled.rubySpans)
            assertEquals(0, unparceled.rubySpans!!.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_multipleStyledSpans_canBeParceled() {
        val spans = arrayOf(
            StyledSpan().apply {
                startIndex = 0
                endIndex = 5
                style = InlineStyle().apply { bold = true }
            },
            StyledSpan().apply {
                startIndex = 6
                endIndex = 11
                style = InlineStyle().apply { italic = true }
            },
            StyledSpan().apply {
                startIndex = 12
                endIndex = 17
                style = InlineStyle().apply { fontSize = 1.5f }
            }
        )

        val original = StyledText().apply {
            text = "First Second Third"
            styledSpans = spans
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("First Second Third", unparceled.text)
            assertNotNull(unparceled.styledSpans)
            assertEquals(3, unparceled.styledSpans!!.size)
            assertTrue(unparceled.styledSpans!![0].style.bold)
            assertTrue(unparceled.styledSpans!![1].style.italic)
            assertEquals(1.5f, unparceled.styledSpans!![2].style.fontSize, 0.001f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_multipleRubySpans_canBeParceled() {
        val spans = arrayOf(
            RubySpan().apply {
                startIndex = 0
                endIndex = 1
                rubyText = "かん"
            },
            RubySpan().apply {
                startIndex = 1
                endIndex = 2
                rubyText = "じ"
            }
        )

        val original = StyledText().apply {
            text = "漢字"
            styledSpans = null
            rubySpans = spans
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals("漢字", unparceled.text)
            assertNotNull(unparceled.rubySpans)
            assertEquals(2, unparceled.rubySpans!!.size)
            assertEquals("かん", unparceled.rubySpans!![0].rubyText)
            assertEquals("じ", unparceled.rubySpans!![1].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_longText_canBeParceled() {
        val longText = "あ".repeat(1000)
        val original = StyledText().apply {
            text = longText
            styledSpans = null
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals(longText, unparceled.text)
            assertEquals(1000, unparceled.text.length)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_manySpans_canBeParceled() {
        val spans = (0 until 50).map { i ->
            StyledSpan().apply {
                startIndex = i * 2
                endIndex = i * 2 + 1
                style = InlineStyle().apply { bold = (i % 2 == 0) }
            }
        }.toTypedArray()

        val original = StyledText().apply {
            text = "A".repeat(100)
            styledSpans = spans
            rubySpans = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = StyledText.CREATOR.createFromParcel(parcel)

            assertEquals(100, unparceled.text.length)
            assertNotNull(unparceled.styledSpans)
            assertEquals(50, unparceled.styledSpans!!.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun styledText_describeContents_returnsZero() {
        val text = StyledText()
        assertEquals(0, text.describeContents())
    }
}
