package io.github.dokuendev.dokuenreader.plugin.core

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for PluginConfigSchema parcelable implementation.
 */
@RunWith(AndroidJUnit4::class)
class PluginConfigSchemaIntegrationTest {

    @Test
    fun pluginConfigSchema_canBeParceledAndUnparceled() {
        val fields = listOf(
            ConfigField("key1", "Name1", "Desc1", ConfigFieldType.STRING, null, true),
            ConfigField("key2", "Name2", "Desc2", ConfigFieldType.BOOLEAN, "true", false),
            ConfigField("key3", "Name3", "Desc3", ConfigFieldType.INT, "42", false)
        )

        val original = PluginConfigSchema(fields)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PluginConfigSchema.CREATOR.createFromParcel(parcel)

            assertEquals(original.fields.size, unparceled.fields.size)
            for (i in original.fields.indices) {
                assertEquals(original.fields[i].key, unparceled.fields[i].key)
                assertEquals(original.fields[i].displayName, unparceled.fields[i].displayName)
                assertEquals(original.fields[i].type, unparceled.fields[i].type)
            }
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pluginConfigSchema_emptyFields_canBeParceled() {
        val original = PluginConfigSchema(emptyList())

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PluginConfigSchema.CREATOR.createFromParcel(parcel)

            assertEquals(0, unparceled.fields.size)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pluginConfigSchema_largeFieldList_canBeParceled() {
        val fields = (1..20).map { i ->
            ConfigField(
                key = "field_$i",
                displayName = "Field $i",
                description = "Description $i",
                type = ConfigFieldType.STRING,
                defaultValue = "default_$i",
                isRequired = i % 2 == 0
            )
        }

        val original = PluginConfigSchema(fields)

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val unparceled = PluginConfigSchema.CREATOR.createFromParcel(parcel)

            assertEquals(20, unparceled.fields.size)
            assertEquals("field_1", unparceled.fields[0].key)
            assertEquals("field_20", unparceled.fields[19].key)
        } finally {
            parcel.recycle()
        }
    }
}
