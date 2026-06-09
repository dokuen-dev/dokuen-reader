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
 * Instrumented tests for DictionaryEntry parcelable implementation.
 * Tests parcelable write/read round-trip for all fields, null handling, and array serialization.
 */
@RunWith(AndroidJUnit4::class)
class DictionaryEntryParcelableTest {

    @Test
    fun dictionaryEntry_simpleEntry_canBeParceled() {
        val body = StyledText().apply {
            text = "cat; feline"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "猫"
            pronunciation = null
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("猫", unparceled.headword)
            assertNull(unparceled.pronunciation)
            assertEquals("cat; feline", unparceled.body.text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_withPronunciation_canBeParceled() {
        val body = StyledText().apply {
            text = "cat; feline"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "猫"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 1
                    rubyText = "ねこ"
                }
            )
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("猫", unparceled.headword)
            assertNotNull(unparceled.pronunciation)
            assertEquals(1, unparceled.pronunciation!!.size)
            assertEquals(0, unparceled.pronunciation!![0].startIndex)
            assertEquals(1, unparceled.pronunciation!![0].endIndex)
            assertEquals("ねこ", unparceled.pronunciation!![0].rubyText)
            assertEquals("cat; feline", unparceled.body.text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_withHeadwordSpans_canBeParceled() {
        val body = StyledText().apply {
            text = "Japanese language"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "日本語"
            pronunciation = null
            headwordSpans = arrayOf(
                HeadwordSpan().apply {
                    startIndex = 0
                    endIndex = 2
                    linkUrl = "lookup:日本"
                },
                HeadwordSpan().apply {
                    startIndex = 2
                    endIndex = 3
                    linkUrl = "lookup:語"
                }
            )
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("日本語", unparceled.headword)
            assertNotNull(unparceled.headwordSpans)
            assertEquals(2, unparceled.headwordSpans!!.size)
            assertEquals(0, unparceled.headwordSpans!![0].startIndex)
            assertEquals(2, unparceled.headwordSpans!![0].endIndex)
            assertEquals("lookup:日本", unparceled.headwordSpans!![0].linkUrl)
            assertEquals(2, unparceled.headwordSpans!![1].startIndex)
            assertEquals(3, unparceled.headwordSpans!![1].endIndex)
            assertEquals("lookup:語", unparceled.headwordSpans!![1].linkUrl)
            assertEquals("Japanese language", unparceled.body.text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_withMultiplePronunciationSpans_canBeParceled() {
        val body = StyledText().apply {
            text = "Chinese characters; kanji"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "漢字"
            pronunciation = arrayOf(
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
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("漢字", unparceled.headword)
            assertNotNull(unparceled.pronunciation)
            assertEquals(2, unparceled.pronunciation!!.size)
            assertEquals("かん", unparceled.pronunciation!![0].rubyText)
            assertEquals("じ", unparceled.pronunciation!![1].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_withStyledBody_canBeParceled() {
        val style = InlineStyle().apply {
            bold = true
            foregroundColor = Color.BLUE
        }

        val body = StyledText().apply {
            text = "Bold definition"
            styledSpans = arrayOf(
                StyledSpan().apply {
                    startIndex = 0
                    endIndex = 4
                    this.style = style
                }
            )
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "test"
            pronunciation = null
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("test", unparceled.headword)
            assertEquals("Bold definition", unparceled.body.text)
            assertNotNull(unparceled.body.styledSpans)
            assertEquals(1, unparceled.body.styledSpans!!.size)
            assertTrue(unparceled.body.styledSpans!![0].style.bold)
            assertEquals(Color.BLUE, unparceled.body.styledSpans!![0].style.foregroundColor)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_withBodyRubySpans_canBeParceled() {
        val body = StyledText().apply {
            text = "Example: 食べる means to eat"
            styledSpans = null
            rubySpans = arrayOf(
                RubySpan().apply {
                    startIndex = 9
                    endIndex = 11
                    rubyText = "たべる"
                }
            )
        }

        val original = DictionaryEntry().apply {
            headword = "食べる"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 1
                    rubyText = "た"
                }
            )
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("食べる", unparceled.headword)
            assertNotNull(unparceled.pronunciation)
            assertEquals(1, unparceled.pronunciation!!.size)
            assertNotNull(unparceled.body.rubySpans)
            assertEquals(1, unparceled.body.rubySpans!!.size)
            assertEquals("たべる", unparceled.body.rubySpans!![0].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_complexEntry_canBeParceled() {
        val block1 = BlockSpan().apply {
            startIndex = 0
            endIndex = 6
            blockType = 1 // BLOCK_TYPE_LIST_ITEM
            listMarker = "1."
        }
        val block2 = BlockSpan().apply {
            startIndex = 7
            endIndex = 17
            blockType = 1 // BLOCK_TYPE_LIST_ITEM
            listMarker = "2."
        }

        val body = StyledText().apply {
            text = "to eat\nto live on"
            blockSpans = arrayOf(block1, block2)
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "食べる"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 1
                    rubyText = "た"
                }
            )
            headwordSpans = arrayOf(
                HeadwordSpan().apply {
                    startIndex = 0
                    endIndex = 3
                    linkUrl = "lookup:食べる"
                }
            )
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("食べる", unparceled.headword)
            assertNotNull(unparceled.pronunciation)
            assertEquals(1, unparceled.pronunciation!!.size)
            assertEquals("た", unparceled.pronunciation!![0].rubyText)
            assertNotNull(unparceled.headwordSpans)
            assertEquals(1, unparceled.headwordSpans!!.size)
            assertEquals(0, unparceled.headwordSpans!![0].startIndex)
            assertEquals(3, unparceled.headwordSpans!![0].endIndex)
            assertEquals("lookup:食べる", unparceled.headwordSpans!![0].linkUrl)
            assertEquals("to eat\nto live on", unparceled.body.text)
            assertNotNull(unparceled.body.blockSpans)
            assertEquals(2, unparceled.body.blockSpans!!.size)
            assertEquals(1, unparceled.body.blockSpans!![0].blockType)
            assertEquals("1.", unparceled.body.blockSpans!![0].listMarker)
            assertEquals(1, unparceled.body.blockSpans!![1].blockType)
            assertEquals("2.", unparceled.body.blockSpans!![1].listMarker)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_emptyPronunciationArray_canBeParceled() {
        val body = StyledText().apply {
            text = "definition"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = "word"
            pronunciation = arrayOf()
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals("word", unparceled.headword)
            assertNotNull(unparceled.pronunciation)
            assertEquals(0, unparceled.pronunciation!!.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_longHeadword_canBeParceled() {
        val longHeadword = "あ".repeat(100)
        val body = StyledText().apply {
            text = "definition"
            styledSpans = null
            rubySpans = null
        }

        val original = DictionaryEntry().apply {
            headword = longHeadword
            pronunciation = null
            this.body = body
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryEntry.CREATOR.createFromParcel(parcel)

            assertEquals(longHeadword, unparceled.headword)
            assertEquals(100, unparceled.headword.length)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryEntry_describeContents_returnsZero() {
        val entry = DictionaryEntry()
        assertEquals(0, entry.describeContents())
    }
}
