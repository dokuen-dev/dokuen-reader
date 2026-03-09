package io.github.dokuendev.dokuenreader.plugin.core

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ConfigField parcelable implementation.
 * 
 * These tests verify that AIDL-generated parcelables work correctly
 * across process boundaries.
 */
@RunWith(AndroidJUnit4::class)
class ConfigFieldIntegrationTest {

    @Test
    fun configField_canBeParceledAndUnparceled() {
        val original = ConfigField(
            key = "api_key",
            displayName = "API Key",
            description = "Your API key for the service",
            type = ConfigFieldType.STRING,
            defaultValue = "default_value",
            isRequired = true
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = ConfigField.CREATOR.createFromParcel(parcel)

            assertEquals(original.key, unparceled.key)
            assertEquals(original.displayName, unparceled.displayName)
            assertEquals(original.description, unparceled.description)
            assertEquals(original.type, unparceled.type)
            assertEquals(original.defaultValue, unparceled.defaultValue)
            assertEquals(original.isRequired, unparceled.isRequired)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun configField_withNullDefaultValue_canBeParceled() {
        val original = ConfigField(
            key = "optional_key",
            displayName = "Optional",
            description = "Optional field",
            type = ConfigFieldType.BOOLEAN,
            defaultValue = null,
            isRequired = false
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = ConfigField.CREATOR.createFromParcel(parcel)

            assertNull(unparceled.defaultValue)
            assertFalse(unparceled.isRequired)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun configField_allFieldTypes_canBeParceled() {
        val types = listOf(
            ConfigFieldType.STRING,
            ConfigFieldType.BOOLEAN,
            ConfigFieldType.INT,
            ConfigFieldType.ENUM
        )

        types.forEach { type ->
            val original = ConfigField(
                key = "test_$type",
                displayName = "Test",
                description = "Test field",
                type = type,
                defaultValue = "test",
                isRequired = true
            )

            val parcel = Parcel.obtain()
            try {
                original.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)

                val unparceled = ConfigField.CREATOR.createFromParcel(parcel)
                assertEquals(type, unparceled.type)
            } finally {
                parcel.recycle()
            }
        }
    }

    @Test
    fun configField_withEnumValues_canBeParceled() {
        val original = ConfigField(
            key = "quality",
            displayName = "Quality",
            description = "Model quality level",
            type = ConfigFieldType.ENUM,
            defaultValue = "standard",
            isRequired = false,
            enumValues = listOf("low", "standard", "high")
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = ConfigField.CREATOR.createFromParcel(parcel)

            assertEquals(original.key, unparceled.key)
            assertEquals(original.type, unparceled.type)
            assertEquals(3, unparceled.enumValues?.size)
            assertEquals("low", unparceled.enumValues?.get(0))
            assertEquals("standard", unparceled.enumValues?.get(1))
            assertEquals("high", unparceled.enumValues?.get(2))
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun configField_withNullEnumValues_canBeParceled() {
        val original = ConfigField(
            key = "text_field",
            displayName = "Text",
            description = "Text input",
            type = ConfigFieldType.STRING,
            defaultValue = null,
            isRequired = false,
            enumValues = null
        )

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = ConfigField.CREATOR.createFromParcel(parcel)

            assertNull(unparceled.enumValues)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun configField_describeContents_returnsZero() {
        val field = ConfigField(
            key = "test",
            displayName = "Test",
            description = "Test",
            type = ConfigFieldType.STRING,
            defaultValue = null,
            isRequired = false
        )

        assertEquals(0, field.describeContents())
    }
}
