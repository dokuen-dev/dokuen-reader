package io.github.dokuendev.dokuenreader.plugins.dictionary.simple

import android.os.Bundle
import android.util.Log
import io.github.dokuendev.dokuenreader.dictionary.DictionaryEntry
import io.github.dokuendev.dokuenreader.dictionary.DictionaryErrorCode
import io.github.dokuendev.dokuenreader.dictionary.DictionaryException
import io.github.dokuendev.dokuenreader.dictionary.DictionaryPluginService
import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult
import io.github.dokuendev.dokuenreader.dictionary.InlineStyle
import io.github.dokuendev.dokuenreader.dictionary.RubySpan
import io.github.dokuendev.dokuenreader.dictionary.StyledSpan
import io.github.dokuendev.dokuenreader.dictionary.StyledText
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Simple Dictionary Plugin Sample
 *
 * This sample demonstrates:
 * - Loading a dictionary file from assets in onInitialize()
 * - Basic dictionary lookup functionality
 * - Returning structured definition data with styled text
 * - Using ruby annotations for pronunciation
 * - Simple text styling (bold, italic)
 * - No segmentation handling (HANDLES_SEGMENTATION = false)
 * - Requires dictionary form (REQUIRES_DICTIONARY_FORM = true)
 *
 * The plugin loads a CSV dictionary file from assets/dictionary.csv containing
 * common Japanese words with their readings and definitions.
 *
 * IMPORTANT: CSV is for demonstration only. This sample uses CSV to demonstrate
 * asset loading patterns in a simple, readable way. Production dictionary plugins
 * should use SQLite with Room instead.
 *
 * See the README.md "Extending This Sample" section for SQLite + Room implementation.
 */
class SimpleDictionaryPluginService : DictionaryPluginService() {

    companion object {
        private const val TAG = "SimpleDictionaryPlugin"
        private const val DICTIONARY_FILE = "dictionary.csv"
    }

    /**
     * Represents a single dictionary entry loaded from the CSV file.
     */
    private data class DictEntry(
        val headword: String,
        val reading: String,
        val partOfSpeech: String,
        val definition: String
    )

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, false)
        putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, true)
        putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, arrayOf("ja"))
        putStringArray(PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES, arrayOf("en"))
    }

    // Dictionary data loaded from CSV file
    private val dictionary = mutableMapOf<String, DictEntry>()

    /**
     * Load the dictionary file from assets during initialization.
     *
     * This demonstrates the recommended pattern of loading heavy resources
     * in onInitialize() rather than in the constructor or on first lookup.
     */
    override suspend fun onInitialize(config: Bundle?): InitResult {
        try {
            Log.d(TAG, "Loading dictionary from assets/$DICTIONARY_FILE")

            val inputStream = applicationContext.assets.open(DICTIONARY_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            var lineCount = 0
            var entryCount = 0

            reader.useLines { lines ->
                lines.forEach { line ->
                    lineCount++

                    // Skip comments and empty lines
                    if (line.startsWith("#") || line.isBlank()) {
                        return@forEach
                    }

                    // Parse CSV line: headword,reading,part_of_speech,definition
                    val parts = parseCsvLine(line)
                    if (parts.size >= 4) {
                        val entry = DictEntry(
                            headword = parts[0].trim(),
                            reading = parts[1].trim(),
                            partOfSpeech = parts[2].trim(),
                            definition = parts[3].trim()
                        )
                        dictionary[entry.headword] = entry
                        entryCount++
                    } else {
                        Log.w(TAG, "Skipping malformed line $lineCount: $line")
                    }
                }
            }

            Log.d(TAG, "Successfully loaded $entryCount dictionary entries from $lineCount lines")
            return InitResultFactory.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dictionary file", e)
            return InitResultFactory.failure("Failed to load dictionary: ${e.message}")
        }
    }

    /**
     * Parse a CSV line, handling quoted fields that may contain commas.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val char = line[i]

            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }

                char == ',' && !inQuotes -> {
                    result.add(currentField.toString())
                    currentField.clear()
                }

                else -> {
                    currentField.append(char)
                }
            }
        }

        // Add the last field
        result.add(currentField.toString())

        return result
    }

    /**
     * Perform dictionary lookup.
     *
     * Since HANDLES_SEGMENTATION = false and REQUIRES_DICTIONARY_FORM = true,
     * the host app will:
     * 1. Get the selected text based on tap selection mode
     * 2. Deinflect it to dictionary form
     * 3. Pass the dictionary form to this method
     *
     * @param contextText The full text context (not used in this simple implementation)
     * @param cursorStartIndex Start index of the query word (not used)
     * @param cursorEndIndex End index of the query word (not used)
     * @return DictionaryResult containing matching entries, or empty if not found
     */
    override suspend fun onLookup(
        contextText: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int
    ): DictionaryResult {
        // Extract the query word from the context
        val queryWord = contextText.substring(cursorStartIndex, cursorEndIndex)

        // Look up the word in our loaded dictionary
        val dictEntry = dictionary[queryWord]
            ?: throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "Word '$queryWord' not found in dictionary"
            )

        // Convert the loaded entry to a DictionaryEntry with styled text
        val entry = createDictionaryEntry(dictEntry)

        return DictionaryResult(entries = arrayOf(entry))
    }

    /**
     * Create a formatted DictionaryEntry from a loaded dictionary entry.
     */
    private fun createDictionaryEntry(dictEntry: DictEntry): DictionaryEntry {
        // Build the body text with part of speech and definition
        val bodyText = buildString {
            append(dictEntry.partOfSpeech)
            append("\n")
            append(dictEntry.definition)
        }

        val posLength = dictEntry.partOfSpeech.length

        return DictionaryEntry(
            headword = dictEntry.headword,
            pronunciation = buildRubySpans(dictEntry.headword, dictEntry.reading)
                .takeIf { it.isNotEmpty() }
                ?.toTypedArray(),
            body = StyledText(
                text = bodyText,
                styledSpans = arrayOf(
                    // Style the part of speech tag
                    StyledSpan(
                        startIndex = 0,
                        endIndex = posLength,
                        style = InlineStyle(
                            bold = true,
                            italic = true,
                            fontSize = 0.9f,
                            foregroundColor = 0xFF1976D2.toInt()
                        )
                    ),
                    // Style the definition
                    StyledSpan(
                        startIndex = posLength + 1,
                        endIndex = bodyText.length,
                        style = InlineStyle()
                    )
                ),
                rubySpans = emptyArray()
            )
        )
    }

    /**
     * Build ruby span annotations by stripping the common kana suffix shared between
     * the headword and its reading.
     *
     * This is for illustrative purposes only. A real implementation would require a
     * more sophisticated algorithm to handle prefixes, disjoint internal kana, ambiguous
     * affixes, and other corner cases. Ideally the ruby spans would be embedded in the
     * dictionary data directly rather than being built dynamically as here.
     *
     * Algorithm:
     * 1. Walk backwards through both strings simultaneously while the headword
     *    character is kana AND matches the reading character exactly.
     * 2. The remaining prefix [0, kanjiEnd) is the annotated region.
     * 3. If the entire headword is consumed (all kana, no kanji), return an empty
     *    list, no annotation is needed.
     *
     * Examples:
     *   食べる / たべる  → RubySpan(0, 1, "た")    ("べる" stripped)
     *   行く   / いく    → RubySpan(0, 1, "い")    ("く" stripped)
     *   学校   / がっこう → RubySpan(0, 2, "がっこう") (no kana suffix)
     *   本     / ほん    → RubySpan(0, 1, "ほん")   (no kana suffix)
     */
    private fun buildRubySpans(headword: String, reading: String): List<RubySpan> {
        val hwLen = headword.length
        val rdLen = reading.length

        // Count matching kana characters from the end
        var suffixLen = 0
        while (suffixLen < hwLen && suffixLen < rdLen) {
            val hwChar = headword[hwLen - 1 - suffixLen]
            val rdChar = reading[rdLen - 1 - suffixLen]
            if (hwChar.isKana() && hwChar == rdChar) {
                suffixLen++
            } else {
                break
            }
        }

        val kanjiEnd = hwLen - suffixLen
        val readingEnd = rdLen - suffixLen

        // Entire headword was kana, no annotation needed
        if (kanjiEnd == 0) return emptyList()

        return listOf(
            RubySpan(
                startIndex = 0,
                endIndex = kanjiEnd,
                rubyText = reading.substring(0, readingEnd)
            )
        )
    }

    /** Returns true for hiragana (U+3040–U+309F) and katakana (U+30A0–U+30FF). */
    private fun Char.isKana(): Boolean = this in '\u3040'..'\u30FF'

    /**
     * Clean up resources when the session ends.
     */
    override fun onShutdown() {
        dictionary.clear()
        Log.d(TAG, "Dictionary cleared")
    }
}
