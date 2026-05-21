package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.InlineStyle;

/**
 * Associates a character range with formatting style.
 * 
 * StyledSpan applies an InlineStyle to a specific character range within StyledText.
 * Multiple spans can overlap, with their properties merged additively.
 * 
 * Overlap Behavior:
 * - Overlapping boolean properties (bold, italic) are combined (both can be true)
 * - Overlapping scalar properties (fontSize, colors) use the later span's value
 * - This matches Compose AnnotatedString semantics
 * 
 * Character Range:
 * - startIndex: Inclusive start position in the text string
 * - endIndex: Exclusive end position in the text string
 * - Range must be valid: 0 <= startIndex < endIndex <= text.length
 * - Out-of-bounds spans are ignored by the host app
 * 
 * Example:
 * Text: "Hello World"
 * StyledSpan(0, 5, InlineStyle(bold=true)) → "Hello" is bold
 * StyledSpan(6, 11, InlineStyle(italic=true)) → "World" is italic
 * StyledSpan(0, 11, InlineStyle(fontSize=1.2)) → Entire text is 20% larger
 */
parcelable StyledSpan {
    /**
     * Start index in the text (inclusive).
     */
    int startIndex;
    
    /**
     * End index in the text (exclusive).
     */
    int endIndex;
    
    /**
     * The style to apply to this range.
     */
    InlineStyle style;
}
