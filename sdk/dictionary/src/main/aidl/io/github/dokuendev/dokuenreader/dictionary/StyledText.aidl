package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.StyledSpan;
import io.github.dokuendev.dokuenreader.dictionary.RubySpan;

/**
 * Text with optional inline formatting and ruby annotations.
 * 
 * StyledText is the core formatting primitive for dictionary definitions. It pairs
 * plain text with optional style and ruby annotations, similar to Compose AnnotatedString
 * but designed for IPC serialization.
 * 
 * Components:
 * - text: The plain text content (required)
 * - styledSpans: Optional array of StyledSpan for formatting (bold, italic, colors, lists)
 * - rubySpans: Optional array of RubySpan for pronunciation annotations (furigana/bopomofo)
 * 
 * Usage Patterns:
 * 
 * 1. Plain text (no formatting):
 *    StyledText(text = "Simple definition")
 * 
 * 2. Formatted text:
 *    StyledText(
 *        text = "Bold and italic text",
 *        styledSpans = [
 *            StyledSpan(0, 4, InlineStyle(bold=true)),
 *            StyledSpan(9, 15, InlineStyle(italic=true))
 *        ]
 *    )
 * 
 * 3. Text with ruby annotations:
 *    StyledText(
 *        text = "漢字",
 *        rubySpans = [RubySpan(0, 2, "かんじ")]
 *    )
 * 
 * 4. Numbered list:
 *    StyledText(
 *        text = "First item\nSecond item",
 *        styledSpans = [
 *            StyledSpan(0, 10, InlineStyle(listItemOrdinal=1)),
 *            StyledSpan(11, 22, InlineStyle(listItemOrdinal=2))
 *        ]
 *    )
 * 
 * Security Limits:
 * - text.length <= 50,000 characters per entry
 * - styledSpans.length + rubySpans.length <= 500 per entry
 * - Out-of-bounds spans are ignored
 */
parcelable StyledText {
    /**
     * The plain text content.
     */
    String text;
    
    /**
     * Optional style annotations applied to character ranges.
     */
    @nullable StyledSpan[] styledSpans;
    
    /**
     * Optional ruby text annotations for character ranges.
     */
    @nullable RubySpan[] rubySpans;
}
