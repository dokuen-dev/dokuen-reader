package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Defines a block-level layout container for a character range.
 *
 * BlockSpan separates structural layout concerns from inline text styling.
 * Each BlockSpan defines how a segment of text should be laid out as a container,
 * independent of the text formatting within it.
 *
 * BlockSpans use nesting semantics. Blocks define hierarchical structure and can be
 * nested within each other (e.g., a list item within a box, a table within a list item).
 * Child blocks inherit context from their parent blocks.
 *
 * Block Types:
 * - 0 = Normal paragraph (default)
 * - 1 = List Item (rendered with marker and hanging indent)
 * - 2 = Box (rendered with background color and optional indent)
 * - 3 = Table (parsed as pipe-delimited grid)
 *
 * Character Range:
 * - startIndex: Inclusive start position in the text string
 * - endIndex: Exclusive end position in the text string
 * - Range must be valid: 0 <= startIndex < endIndex <= text.length
 * - Out-of-bounds spans are ignored by the host app
 *
 * Example Usage:
 *
 * List Item:
 *   BlockSpan(0, 10, blockType=1, indentLevel=1, listMarker="•")
 *
 * Nested List:
 *   BlockSpan(11, 25, blockType=1, indentLevel=2, listMarker="1.")
 *
 * Box with Background:
 *   BlockSpan(26, 50, blockType=2, backgroundColor=0xFFF0F0F0, indentLevel=0)
 *
 * Table:
 *   BlockSpan(51, 150, blockType=3)
 */
parcelable BlockSpan {
    /**
     * Start index in the text (inclusive).
     */
    int startIndex;

    /**
     * End index in the text (exclusive).
     */
    int endIndex;

    /**
     * Block type: 0=Normal, 1=List Item, 2=Box, 3=Table.
     */
    int blockType;

    /**
     * Indent depth for the block container.
     * 0 = no indent, 1+ = indent level (typically 16dp per level).
     */
    int indentLevel;

    /**
     * Optional list marker string for list items (e.g., "•", "1.", "α.").
     * Only used when blockType = 1 (List Item).
     */
    @nullable String listMarker;

    /**
     * Background color for the block container in ARGB format.
     * 0 = no background (transparent).
     * Primarily used with blockType = 2 (Box).
     */
    int backgroundColor;
}
