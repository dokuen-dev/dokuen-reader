package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Associates a character range with ruby text (furigana/bopomofo).
 * 
 * RubySpan pairs a base text run with its pronunciation reading, typically rendered
 * above (horizontal layout) or beside (vertical layout) the base text.
 * 
 * Common Use Cases:
 * - Japanese furigana: 漢字(かんじ) → base="漢字", ruby="かんじ"
 * - Chinese bopomofo: 漢字(ㄏㄢˋ ㄗˋ) → base="漢字", ruby="ㄏㄢˋ ㄗˋ"
 * 
 * Character Range:
 * - startIndex: Inclusive start position in the text string
 * - endIndex: Exclusive end position in the text string
 * - Range must be valid: 0 <= startIndex < endIndex <= text.length
 * - Out-of-bounds spans are ignored by the host app
 */
parcelable RubySpan {
    /**
     * Start index in the text (inclusive).
     */
    int startIndex;
    
    /**
     * End index in the text (exclusive).
     */
    int endIndex;
    
    /**
     * The ruby text to display above/beside the base text.
     */
    String rubyText;
}
