package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.RubySpan;

/**
 * One word of a readings request's text, as the host segments it, with its ruby.
 *
 * @since 0.8.0
 */
parcelable WordReading {
    /** Start of the word in the requested text (inclusive), in UTF-16 code units. */
    int startIndex;

    /** End of the word in the requested text (exclusive). */
    int endIndex;

    /**
     * The word's ruby, which gives a reading for each part of the word that carries one. Offsets
     * are into the requested text, not into the word. The array is empty for a word with no
     * reading, such as a kana word or punctuation.
     */
    RubySpan[] ruby;
}
