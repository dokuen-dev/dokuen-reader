package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Describes the pitch accent of a run of kana.
 *
 * A PitchAccentSpan says what the pitch is and nothing about how to show it. The host may
 * draw the pitch however it likes, or ignore the span.
 *
 * Rules:
 * - The text in the range is complete and readable by itself. It must stay correct when the
 *   host ignores the span.
 * - A span describes pitch only. Other spans, such as a StyledSpan, may overlap it.
 * - Two PitchAccentSpans must not overlap. If they do, the behavior is undefined.
 * - The host app ignores the whole span, not only the part that is wrong, when any of these
 *   is true:
 *   - startIndex < 0, endIndex > text.length, or startIndex >= endIndex.
 *   - moraLengths is null or empty, contains a length of 0 or less, or does not add up to
 *     endIndex - startIndex.
 *   - pitchPattern is null, is not moraLengths.size + 1 characters long, or contains a
 *     character other than uppercase H and L.
 *   - nasalMorae or devoicedMorae contains an index that is negative or not less than
 *     moraLengths.size.
 *
 * Fields:
 * - startIndex, endIndex: The range of the kana in StyledText.text, inclusive and exclusive,
 *   as on every other span.
 * - moraLengths: The length of each mora in UTF-16 code units, in order. They add up to
 *   endIndex - startIndex. They are relative to the span rather than to the text, so that
 *   moving a span only changes its two indices.
 * - pitchPattern: One character per mora, H for high and L for low, followed by one more
 *   character for the pitch of whatever follows the word (such as a particle). Its length is
 *   moraLengths.size + 1. It is always this length and contains only H and L, whatever form
 *   the dictionary's data took.
 * - nasalMorae: The 0-based indices of the nasal morae (bidakuon), or null for none.
 * - devoicedMorae: The 0-based indices of the devoiced morae, or null for none.
 *
 * Examples:
 * - いたい (downstep after mora 2): moraLengths = [1, 1, 1], pitchPattern = "LHLL"
 * - はし (no downstep): moraLengths = [1, 1], pitchPattern = "LHH"
 * - きょう (downstep after mora 1): moraLengths = [2, 1], pitchPattern = "HLL"
 * - ばんこ゚う (downstep after mora 3, third mora nasal): moraLengths = [1, 1, 2, 1],
 *   pitchPattern = "LHHLL", nasalMorae = [2]. The nasal kana is written as the base kana plus
 *   a combining handakuten (U+309A), which is how it appears in print. The two code units are
 *   one mora.
 * - どぼくこうじ (downstep after mora 4, third mora devoiced): moraLengths = [1, 1, 1, 1, 1, 1],
 *   pitchPattern = "LHHHLLL", devoicedMorae = [2]
 *
 * @since 0.10.0
 */
parcelable PitchAccentSpan {
    /**
     * Start index in the text (inclusive).
     */
    int startIndex;

    /**
     * End index in the text (exclusive).
     */
    int endIndex;

    /**
     * The length of each mora in UTF-16 code units, in order. The lengths add up to
     * endIndex - startIndex.
     */
    int[] moraLengths;

    /**
     * One H or L per mora, followed by one more character for the pitch of whatever follows
     * the word. Its length is moraLengths.size + 1.
     */
    String pitchPattern;

    /**
     * The 0-based indices of the nasal morae, or null for none.
     */
    @nullable int[] nasalMorae;

    /**
     * The 0-based indices of the devoiced morae, or null for none.
     */
    @nullable int[] devoicedMorae;
}
