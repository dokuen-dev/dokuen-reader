package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests for Kotlin extension functions and factory constructors
 * in DictionaryExtensions.kt.
 */
@RunWith(AndroidJUnit4::class)
class DictionaryExtensionsTest {

    @Test
    fun inlineStyle_factoryConstructor_correctlySetsDefaultValues() {
        val style = InlineStyle()
        assertFalse(style.bold)
        assertFalse(style.italic)
        assertEquals(0.0f, style.fontSize)
        assertEquals(0, style.foregroundColor)
        assertEquals(0, style.backgroundColor)
        assertEquals(0, style.listItemOrdinal)
        assertEquals(0, style.listIndentLevel)
        assertNull(style.listMarkerOverride)
        assertFalse(style.isBlock)
        assertFalse(style.isTable)
        assertNull(style.hoverText)
        assertNull(style.linkUrl)
    }

    @Test
    fun inlineStyle_factoryConstructor_setsSpecifiedValues() {
        val style = InlineStyle(
            bold = true,
            italic = true,
            fontSize = 1.4f,
            foregroundColor = Color.BLUE,
            backgroundColor = Color.RED,
            listItemOrdinal = 3,
            listIndentLevel = 2,
            listMarkerOverride = "•",
            isBlock = true,
            isTable = true,
            hoverText = "Hover Text",
            linkUrl = "action:test"
        )
        assertTrue(style.bold)
        assertTrue(style.italic)
        assertEquals(1.4f, style.fontSize, 0.001f)
        assertEquals(Color.BLUE, style.foregroundColor)
        assertEquals(Color.RED, style.backgroundColor)
        assertEquals(3, style.listItemOrdinal)
        assertEquals(2, style.listIndentLevel)
        assertEquals("•", style.listMarkerOverride)
        assertTrue(style.isBlock)
        assertTrue(style.isTable)
        assertEquals("Hover Text", style.hoverText)
        assertEquals("action:test", style.linkUrl)
    }

    @Test
    fun dictionaryEntry_factoryConstructor_setsSpecifiedValues() {
        val bodyText = StyledText(text = "Word definition")
        val rubySpans = arrayOf(RubySpan(0, 2, "furigana"))
        val entry = DictionaryEntry(
            headword = "test",
            pronunciation = rubySpans,
            body = bodyText
        )

        assertEquals("test", entry.headword)
        assertNotNull(entry.pronunciation)
        assertEquals(1, entry.pronunciation.size)
        assertEquals("furigana", entry.pronunciation[0].rubyText)
        assertEquals(bodyText, entry.body)
    }

    @Test
    fun styledText_factoryConstructor_setsSpecifiedValues() {
        val style = InlineStyle(bold = true)
        val styledSpans = arrayOf(StyledSpan(0, 5, style))
        val rubySpans = arrayOf(RubySpan(0, 5, "ruby"))
        val styledText = StyledText(
            text = "Hello",
            styledSpans = styledSpans,
            rubySpans = rubySpans
        )

        assertEquals("Hello", styledText.text)
        assertNotNull(styledText.styledSpans)
        assertEquals(1, styledText.styledSpans.size)
        assertEquals(style.bold, styledText.styledSpans[0].style.bold)
        assertNotNull(styledText.rubySpans)
        assertEquals(1, styledText.rubySpans.size)
        assertEquals("ruby", styledText.rubySpans[0].rubyText)
    }

    @Test
    fun dictionaryResult_arrayConstructor_setsSpecifiedValues() {
        val entry = DictionaryEntry("test", null, StyledText("def"))
        val result = DictionaryResult(arrayOf(entry))

        assertNotNull(result.entries)
        assertEquals(1, result.entries.size)
        assertEquals("test", result.entries[0].headword)
    }

    @Test
    fun dictionaryResult_listConstructor_setsSpecifiedValues() {
        val entry = DictionaryEntry("test", null, StyledText("def"))
        val result = DictionaryResult(listOf(entry).toTypedArray())

        assertNotNull(result.entries)
        assertEquals(1, result.entries.size)
        assertEquals("test", result.entries[0].headword)
    }

    @Test
    fun rubySpan_factoryConstructor_setsSpecifiedValues() {
        val span = RubySpan(0, 3, "test_ruby")
        assertEquals(0, span.startIndex)
        assertEquals(3, span.endIndex)
        assertEquals("test_ruby", span.rubyText)
    }

    @Test
    fun styledSpan_factoryConstructor_setsSpecifiedValues() {
        val style = InlineStyle(italic = true)
        val span = StyledSpan(0, 4, style)
        assertEquals(0, span.startIndex)
        assertEquals(4, span.endIndex)
        assertEquals(style, span.style)
    }
}
