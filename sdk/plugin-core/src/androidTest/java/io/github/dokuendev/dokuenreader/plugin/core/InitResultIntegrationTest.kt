package io.github.dokuendev.dokuenreader.plugin.core

import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for InitResult parcelable implementation.
 */
@RunWith(AndroidJUnit4::class)
class InitResultIntegrationTest {

    @Test
    fun initResult_success_canBeParceled() {
        val requirements = Bundle().apply {
            putInt("max_width", 2048)
            putBoolean("downsample", true)
            putString("custom_key", "custom_value")
        }

        val original = InitResult(
            success = true,
            errorMessage = null,
            requirements = requirements
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InitResult.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.success)
            assertNull(unparceled.errorMessage)
            assertNotNull(unparceled.requirements)
            assertEquals(2048, unparceled.requirements?.getInt("max_width"))
            assertEquals(true, unparceled.requirements?.getBoolean("downsample"))
            assertEquals("custom_value", unparceled.requirements?.getString("custom_key"))
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun initResult_failure_canBeParceled() {
        val original = InitResult(
            success = false,
            errorMessage = "API key is required",
            requirements = null
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InitResult.CREATOR.createFromParcel(parcel)

            assertFalse(unparceled.success)
            assertEquals("API key is required", unparceled.errorMessage)
            assertNull(unparceled.requirements)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun initResult_withEmptyRequirements_canBeParceled() {
        val original = InitResult(
            success = true,
            errorMessage = null,
            requirements = Bundle()
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InitResult.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.success)
            assertNotNull(unparceled.requirements)
            assertTrue(unparceled.requirements!!.isEmpty)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun initResultFactory_success_createsParcelableResult() {
        val requirements = Bundle().apply {
            putInt("test_key", 123)
        }

        val result = InitResultFactory.success(requirements)

        val parcel = Parcel.obtain()
        try {
            result.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InitResult.CREATOR.createFromParcel(parcel)

            assertTrue(unparceled.success)
            assertEquals(123, unparceled.requirements?.getInt("test_key"))
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun initResultFactory_failure_createsParcelableResult() {
        val result = InitResultFactory.failure("Test error")

        val parcel = Parcel.obtain()
        try {
            result.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = InitResult.CREATOR.createFromParcel(parcel)

            assertFalse(unparceled.success)
            assertEquals("Test error", unparceled.errorMessage)
        } finally {
            parcel.recycle()
        }
    }
}
