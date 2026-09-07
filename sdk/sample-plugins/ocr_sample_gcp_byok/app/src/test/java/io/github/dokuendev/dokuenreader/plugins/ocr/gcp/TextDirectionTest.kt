package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import org.junit.Assert.assertEquals
import org.junit.Test

class TextDirectionTest {

    @Test
    fun `TextDirection enum has correct values`() {
        assertEquals("", TextDirection.NOT_SET.value)
        assertEquals("horizontal", TextDirection.HORIZONTAL.value)
        assertEquals("vertical", TextDirection.VERTICAL.value)
        assertEquals("auto", TextDirection.AUTO.value)
    }

    @Test
    fun `fromString returns correct TextDirection for valid strings`() {
        assertEquals(TextDirection.HORIZONTAL, TextDirection.fromString("horizontal"))
        assertEquals(TextDirection.VERTICAL, TextDirection.fromString("vertical"))
        assertEquals(TextDirection.AUTO, TextDirection.fromString("auto"))
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString(""))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(TextDirection.HORIZONTAL, TextDirection.fromString("HORIZONTAL"))
        assertEquals(TextDirection.VERTICAL, TextDirection.fromString("Vertical"))
        assertEquals(TextDirection.AUTO, TextDirection.fromString("AuTo"))
    }

    @Test
    fun `fromString returns NOT_SET for invalid strings`() {
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString("invalid"))
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString("diagonal"))
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString("random"))
    }

    @Test
    fun `fromString returns NOT_SET for null input`() {
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString(null))
    }

    @Test
    fun `fromString returns NOT_SET for empty string`() {
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString(""))
    }

    @Test
    fun `fromString handles whitespace`() {
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString(" horizontal "))
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString("horizontal "))
        assertEquals(TextDirection.NOT_SET, TextDirection.fromString(" horizontal"))
    }

    @Test
    fun `all enum values can be converted back from string`() {
        for (direction in TextDirection.entries) {
            if (direction != TextDirection.NOT_SET) {
                assertEquals(direction, TextDirection.fromString(direction.value))
            }
        }
    }
}
