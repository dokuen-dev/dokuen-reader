package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.HeadwordSpan;
import io.github.dokuendev.dokuenreader.dictionary.RubySpan;
import io.github.dokuendev.dokuenreader.dictionary.StyledText;

/**
 * Represents a single dictionary entry for a headword.
 * 
 * DictionaryEntry is the top-level data structure returned by dictionary plugins.
 * Each entry represents one headword with its pronunciation and definition content.
 * 
 * Components:
 * - headword: The dictionary form of the word being defined (required)
 * - pronunciation: Optional ruby annotations for the headword (e.g., furigana)
 * - headwordSpans: Optional link annotations for character ranges within the headword
 * - body: The complete definition content with formatting (required)
 * 
 * Headword Pronunciation:
 * The pronunciation field annotates the headword string with ruby text. This is
 * typically used for:
 * - Japanese: Furigana readings for kanji (e.g., 漢字 → かんじ)
 * - Chinese: Bopomofo or pinyin (e.g., 漢字 → ㄏㄢˋ ㄗˋ)
 * 
 * If the headword has no special pronunciation (e.g., kana-only words), leave
 * pronunciation null or empty.
 * 
 * Headword Links:
 * The headwordSpans field annotates character ranges within the headword string
 * with link targets, enabling individual components of a compound headword to be
 * independently clickable. Ranges must not overlap.
 * 
 * Definition Body:
 * The body contains the full definition content as StyledText, which supports:
 * - Multiple senses/meanings
 * - Part-of-speech tags
 * - Example sentences
 * - Usage notes
 * - Formatted lists
 * - Inline ruby annotations
 * 
 * Example Usage:
 * 
 * 1. Simple entry (no pronunciation):
 *    DictionaryEntry(
 *        headword = "こんにちは",
 *        pronunciation = null,
 *        body = StyledText(text = "Hello; Good afternoon")
 *    )
 * 
 * 2. Entry with furigana:
 *    DictionaryEntry(
 *        headword = "漢字",
 *        pronunciation = [RubySpan(0, 2, "かんじ")],
 *        body = StyledText(text = "Chinese characters; kanji")
 *    )
 * 
 * 3. Entry with headword links (compound word, each component links to its own entry):
 *    DictionaryEntry(
 *        headword = "日本語",
 *        pronunciation = [RubySpan(0, 3, "にほんご")],
 *        headwordSpans = [
 *            HeadwordSpan(0, 2, "lookup:日本"),
 *            HeadwordSpan(2, 3, "lookup:語")
 *        ],
 *        body = StyledText(text = "Japanese language")
 *    )
 * 
 * 4. Entry with formatted definition:
 *    DictionaryEntry(
 *        headword = "食べる",
 *        pronunciation = [RubySpan(0, 1, "た")],
 *        body = StyledText(
 *            text = "to eat\nto live on",
 *            styledSpans = [
 *                StyledSpan(0, 6, InlineStyle(listItemOrdinal=1)),
 *                StyledSpan(7, 17, InlineStyle(listItemOrdinal=2))
 *            ]
 *        )
 *    )
 * 
 * Multiple Entries:
 * A single lookup can return multiple DictionaryEntry objects (e.g., homonyms,
 * different parts of speech). The host app renders them in the order received.
 */
parcelable DictionaryEntry {
    /**
     * The headword (dictionary form) being defined.
     */
    String headword;
    
    /**
     * Optional ruby annotations for the headword pronunciation.
     * Each RubySpan pairs a character range in the headword with its reading.
     */
    @nullable RubySpan[] pronunciation;

    /**
     * Optional link annotations for character ranges within the headword.
     * Each HeadwordSpan pairs a character range in the headword with a link target,
     * making that range clickable in the host UI.
     *
     * Ranges must not overlap. Out-of-bounds spans are ignored.
     *
     * NOTE: HeadwordSpan only supports links. General inline styling (bold, color,
     * font size, etc.) is not supported on headwords.
     */
    @nullable HeadwordSpan[] headwordSpans;

    /**
     * The definition body containing all senses, examples, and formatting.
     */
    StyledText body;
    
    /**
     * Bitmask for entry-level presentation flags.
     * 0 = Default presentation
     * 1 = FLAG_HEADWORD_STROKE_ORDER (Host renders headword with stroke order font)
     * * NOTE: If FLAG_HEADWORD_STROKE_ORDER is set then [pronunciation] is ignored.
     */
    int displayFlags;
}
