package io.github.dokuendev.dokuenreader.dictionary

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for DictionaryResult parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and array serialization.
 */
@RunWith(AndroidJUnit4::class)
class DictionaryResultParcelableTest {

    @Test
    fun dictionaryResult_emptyResult_canBeParceled() {
        val original = DictionaryResult().apply {
            entries = arrayOf()
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryResult.CREATOR.createFromParcel(parcel)

            assertNotNull(unparceled.entries)
            assertEquals(0, unparceled.entries.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryResult_singleEntry_canBeParceled() {
        val body = StyledText().apply {
            text = "cat; feline"
            styledSpans = null
            rubySpans = null
        }

        val entry = DictionaryEntry().apply {
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

        val original = DictionaryResult().apply {
            entries = arrayOf(entry)
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryResult.CREATOR.createFromParcel(parcel)

            assertNotNull(unparceled.entries)
            assertEquals(1, unparceled.entries.size)
            assertEquals("猫", unparceled.entries[0].headword)
            assertEquals("cat; feline", unparceled.entries[0].body.text)
            assertNotNull(unparceled.entries[0].pronunciation)
            assertEquals("ねこ", unparceled.entries[0].pronunciation!![0].rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryResult_multipleEntries_canBeParceled() {
        val entry1Body = StyledText().apply {
            text = "bridge"
            styledSpans = null
            rubySpans = null
        }

        val entry1 = DictionaryEntry().apply {
            headword = "橋"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 1
                    rubyText = "はし"
                }
            )
            body = entry1Body
        }

        val entry2Body = StyledText().apply {
            text = "chopsticks"
            styledSpans = null
            rubySpans = null
        }

        val entry2 = DictionaryEntry().apply {
            headword = "箸"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 1
                    rubyText = "はし"
                }
            )
            body = entry2Body
        }

        val original = DictionaryResult().apply {
            entries = arrayOf(entry1, entry2)
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryResult.CREATOR.createFromParcel(parcel)

            assertNotNull(unparceled.entries)
            assertEquals(2, unparceled.entries.size)

            assertEquals("橋", unparceled.entries[0].headword)
            assertEquals("bridge", unparceled.entries[0].body.text)

            assertEquals("箸", unparceled.entries[1].headword)
            assertEquals("chopsticks", unparceled.entries[1].body.text)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryResult_manyEntries_canBeParceled() {
        val entries = (1..20).map { i ->
            val body = StyledText().apply {
                text = "Definition $i"
                styledSpans = null
                rubySpans = null
            }

            DictionaryEntry().apply {
                headword = "Word$i"
                pronunciation = null
                this.body = body
            }
        }.toTypedArray()

        val original = DictionaryResult().apply {
            this.entries = entries
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryResult.CREATOR.createFromParcel(parcel)

            assertNotNull(unparceled.entries)
            assertEquals(20, unparceled.entries.size)

            for (i in 0 until 20) {
                assertEquals("Word${i + 1}", unparceled.entries[i].headword)
                assertEquals("Definition ${i + 1}", unparceled.entries[i].body.text)
            }
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryResult_entriesWithComplexFormatting_canBeParceled() {
        val bodyStyle = InlineStyle().apply {
            bold = true
            italic = true
        }

        val body = StyledText().apply {
            text = "Complex definition with 漢字"
            styledSpans = arrayOf(
                StyledSpan().apply {
                    startIndex = 0
                    endIndex = 7
                    style = bodyStyle
                }
            )
            rubySpans = arrayOf(
                RubySpan().apply {
                    startIndex = 24
                    endIndex = 26
                    rubyText = "かんじ"
                }
            )
        }

        val entry = DictionaryEntry().apply {
            headword = "複雑"
            pronunciation = arrayOf(
                RubySpan().apply {
                    startIndex = 0
                    endIndex = 2
                    rubyText = "ふくざつ"
                }
            )
            this.body = body
        }

        val original = DictionaryResult().apply {
            entries = arrayOf(entry)
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = DictionaryResult.CREATOR.createFromParcel(parcel)

            assertEquals(1, unparceled.entries.size)
            assertEquals("複雑", unparceled.entries[0].headword)
            assertEquals("ふくざつ", unparceled.entries[0].pronunciation!![0].rubyText)
            assertEquals("Complex definition with 漢字", unparceled.entries[0].body.text)
            assertNotNull(unparceled.entries[0].body.styledSpans)
            assertNotNull(unparceled.entries[0].body.rubySpans)
            assertEquals(1, unparceled.entries[0].body.styledSpans!!.size)
            assertEquals(1, unparceled.entries[0].body.rubySpans!!.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun dictionaryResult_describeContents_returnsZero() {
        val result = DictionaryResult()
        assertEquals(0, result.describeContents())
    }
}
