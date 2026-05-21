package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.DictionaryEntry;

/**
 * The complete result of a dictionary lookup.
 * 
 * DictionaryResult wraps an array of DictionaryEntry objects returned by a single
 * lookup() call. This is the top-level response structure passed from plugin to host.
 * 
 * Result Semantics:
 * - Empty array (entries.length == 0): No definitions found for the query
 * - Non-empty array: One or more matching definitions
 * 
 * The host app renders entries in the order received without sorting, deduplication,
 * or filtering. Plugins are responsible for ordering and filtering results.
 * 
 * Security Limits:
 * The host app enforces the following limits on DictionaryResult:
 * - Maximum 100 entries per result (MAX_DICTIONARY_ENTRIES)
 * - Maximum 50,000 characters per entry body (MAX_BODY_LENGTH)
 * - Maximum 500 spans per entry (MAX_SPANS_PER_ENTRY)
 * 
 * Results exceeding these limits are truncated with a warning logged.
 * 
 * Example Usage:
 * 
 * 1. No results found:
 *    DictionaryResult(entries = [])
 * 
 * 2. Single result:
 *    DictionaryResult(entries = [
 *        DictionaryEntry(
 *            headword = "猫",
 *            pronunciation = [RubySpan(0, 1, "ねこ")],
 *            body = StyledText(text = "cat; feline")
 *        )
 *    ])
 * 
 * 3. Multiple results (homonyms):
 *    DictionaryResult(entries = [
 *        DictionaryEntry(
 *            headword = "橋",
 *            pronunciation = [RubySpan(0, 1, "はし")],
 *            body = StyledText(text = "bridge")
 *        ),
 *        DictionaryEntry(
 *            headword = "箸",
 *            pronunciation = [RubySpan(0, 1, "はし")],
 *            body = StyledText(text = "chopsticks")
 *        )
 *    ])
 * 
 * Performance Considerations:
 * - Keep entry count reasonable (typically < 20 for good UX)
 * - Avoid extremely long definition bodies (consider pagination for large entries)
 * - Minimize span count for faster rendering
 */
parcelable DictionaryResult {
    /**
     * The list of dictionary entries matching the query.
     * Empty array indicates no matches found.
     */
    DictionaryEntry[] entries;
}
