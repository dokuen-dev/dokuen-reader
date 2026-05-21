package io.github.dokuendev.dokuenreader.dictionary

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for RubySpan parcelable implementation.
 * Tests parcelable write/read round-trip for all fields and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class RubySpanParcelableTest {

    @Test
    fun rubySpan_basicSpan_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 0
            endIndex = 2
            rubyText = "かんじ"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(2, unparceled.endIndex)
            assertEquals("かんじ", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_hiraganaReading_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 5
            endIndex = 7
            rubyText = "ひらがな"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(5, unparceled.startIndex)
            assertEquals(7, unparceled.endIndex)
            assertEquals("ひらがな", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_bopomofoReading_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 0
            endIndex = 2
            rubyText = "ㄏㄢˋ ㄗˋ"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(2, unparceled.endIndex)
            assertEquals("ㄏㄢˋ ㄗˋ", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_singleCharacter_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 3
            endIndex = 4
            rubyText = "ねこ"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(3, unparceled.startIndex)
            assertEquals(4, unparceled.endIndex)
            assertEquals("ねこ", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_longReading_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 0
            endIndex = 5
            rubyText = "とうきょうとちゅうおうく"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(5, unparceled.endIndex)
            assertEquals("とうきょうとちゅうおうく", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_emptyRubyText_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 0
            endIndex = 1
            rubyText = ""
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(1, unparceled.endIndex)
            assertEquals("", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_largeIndices_canBeParceled() {
        val original = RubySpan().apply {
            startIndex = 1000
            endIndex = 1050
            rubyText = "よみがな"
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = RubySpan.CREATOR.createFromParcel(parcel)

            assertEquals(1000, unparceled.startIndex)
            assertEquals(1050, unparceled.endIndex)
            assertEquals("よみがな", unparceled.rubyText)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun rubySpan_describeContents_returnsZero() {
        val span = RubySpan()
        assertEquals(0, span.describeContents())
    }
}
