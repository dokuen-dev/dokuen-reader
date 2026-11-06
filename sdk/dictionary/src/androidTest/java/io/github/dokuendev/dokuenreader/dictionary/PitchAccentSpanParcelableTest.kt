package io.github.dokuendev.dokuenreader.dictionary

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for PitchAccentSpan parcelable implementation.
 * Tests parcelable write/read round-trip for all fields, including the nullable arrays.
 */
@RunWith(AndroidJUnit4::class)
class PitchAccentSpanParcelableTest {

    @Test
    fun pitchAccentSpan_nasalExample_canBeParceled() {
        val original = PitchAccentSpan().apply {
            startIndex = 3
            endIndex = 8
            moraLengths = intArrayOf(1, 1, 2, 1)
            pitchPattern = "LHHLL"
            nasalMorae = intArrayOf(2)
            devoicedMorae = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PitchAccentSpan.CREATOR.createFromParcel(parcel)

            assertEquals(3, unparceled.startIndex)
            assertEquals(8, unparceled.endIndex)
            assertArrayEquals(intArrayOf(1, 1, 2, 1), unparceled.moraLengths)
            assertEquals("LHHLL", unparceled.pitchPattern)
            assertArrayEquals(intArrayOf(2), unparceled.nasalMorae)
            assertNull(unparceled.devoicedMorae)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pitchAccentSpan_devoicedExample_canBeParceled() {
        val original = PitchAccentSpan().apply {
            startIndex = 0
            endIndex = 6
            moraLengths = intArrayOf(1, 1, 1, 1, 1, 1)
            pitchPattern = "LHHHLLL"
            nasalMorae = null
            devoicedMorae = intArrayOf(2)
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PitchAccentSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(6, unparceled.endIndex)
            assertArrayEquals(intArrayOf(1, 1, 1, 1, 1, 1), unparceled.moraLengths)
            assertEquals("LHHHLLL", unparceled.pitchPattern)
            assertNull(unparceled.nasalMorae)
            assertArrayEquals(intArrayOf(2), unparceled.devoicedMorae)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pitchAccentSpan_bothOptionalArraysNull_stayNull() {
        val original = PitchAccentSpan().apply {
            startIndex = 0
            endIndex = 3
            moraLengths = intArrayOf(1, 1, 1)
            pitchPattern = "LHLL"
            nasalMorae = null
            devoicedMorae = null
        }

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PitchAccentSpan.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.startIndex)
            assertEquals(3, unparceled.endIndex)
            assertArrayEquals(intArrayOf(1, 1, 1), unparceled.moraLengths)
            assertEquals("LHLL", unparceled.pitchPattern)
            assertNull(unparceled.nasalMorae)
            assertNull(unparceled.devoicedMorae)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pitchAccentSpan_describeContents_returnsZero() {
        val span = PitchAccentSpan()
        assertEquals(0, span.describeContents())
    }
}
