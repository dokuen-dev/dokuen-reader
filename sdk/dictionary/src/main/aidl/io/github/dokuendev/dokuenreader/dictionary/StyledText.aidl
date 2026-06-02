package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.StyledSpan;
import io.github.dokuendev.dokuenreader.dictionary.RubySpan;
import io.github.dokuendev.dokuenreader.dictionary.BlockSpan;

/**
 * Text with optional inline formatting, ruby annotations, and block-level layout.
 *
 * StyledText is the core formatting primitive for dictionary definitions. It pairs
 * plain text with three independent span types:
 * - blockSpans: Define layout containers (lists, boxes, tables) with nesting semantics
 * - styledSpans: Define inline text formatting (bold, italic, colors) with innermost-wins semantics
 * - rubySpans: Define pronunciation annotations (furigana/bopomofo)
 *
 * Architecture: "Separated Span" Model
 * The flat text string is the single source of truth. BlockSpans define the structure
 * (where paragraphs, list items, boxes, and tables start/end), while StyledSpans and
 * RubySpans apply formatting within those structures.
 *
 * Span Semantics Summary:
 * - BlockSpan: NESTING - blocks define hierarchical structure, can be nested
 * - StyledSpan (InlineStyle): INNERMOST-WINS - scalar properties override, boolean properties combine
 * - RubySpan: ANNOTATION - pronunciation markers for character ranges
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
 * 4. List items with inline formatting:
 *    StyledText(
 *        text = "First item\nSecond item",
 *        blockSpans = [
 *            BlockSpan(0, 10, blockType=1, listMarker="•"),
 *            BlockSpan(11, 22, blockType=1, listMarker="•")
 *        ],
 *        styledSpans = [
 *            StyledSpan(0, 5, InlineStyle(bold=true))  // "First" is bold
 *        ]
 *    )
 *
 * Security Limits:
 * - text.length <= 50,000 characters per entry
 * - blockSpans.length + styledSpans.length + rubySpans.length <= 500 per entry
 * - Out-of-bounds spans are ignored
 */
parcelable StyledText {
    /**
     * The plain text content.
     */
    String text;

    /**
     * Optional block-level layout containers applied to character ranges.
     */
    @nullable BlockSpan[] blockSpans;

    /**
     * Optional inline style annotations applied to character ranges.
     */
    @nullable StyledSpan[] styledSpans;

    /**
     * Optional ruby text annotations for character ranges.
     */
    @nullable RubySpan[] rubySpans;
}
