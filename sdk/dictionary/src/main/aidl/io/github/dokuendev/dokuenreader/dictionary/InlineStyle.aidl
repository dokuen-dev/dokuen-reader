package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Formatting directives for a text span.
 * 
 * InlineStyle defines visual formatting properties that can be applied to character ranges
 * within StyledText. Multiple styles can be combined additively (e.g., bold + italic).
 * 
 * Color Sentinel Values:
 * - foregroundColor = 0: Use default text color (theme-dependent)
 * - backgroundColor = 0: No background color (transparent)
 * 
 * List Item Properties:
 * - listItemOrdinal = 0: Not a list item (default)
 * - listItemOrdinal > 0: 1-based position in a numbered list (e.g., 1 = "1.", 2 = "2.")
 * - listItemOrdinal = -1 (LIST_ITEM_BULLET): Bullet list item with default marker ("•")
 * - listMarkerOverride: Custom marker string (e.g., "α. ", "① ") (only has effect when listItemOrdinal != 0)
 * - listIndentLevel: Indent depth (0 = no indent, 1+ = 16dp per level)
 */
parcelable InlineStyle {
    /**
     * Whether the text should be bold.
     */
    boolean bold;
    
    /**
     * Whether the text should be italic.
     */
    boolean italic;
    
    /**
     * Relative font size multiplier (1.0 = normal, 0.8 = smaller, 1.2 = larger).
     */
    float fontSize;
    
    /**
     * Foreground color in ARGB format. 0 = unset (use default theme color).
     */
    int foregroundColor;
    
    /**
     * Background color in ARGB format. 0 = unset (no background).
     */
    int backgroundColor;
    
    /**
     * List item type and ordinal. Determines whether this span is a list item
     * and what kind of marker it receives:
     *   0  = not a list item (default)
     *  -1  = bullet list item (rendered with "•" unless listMarkerOverride is set)
     *  >0  = numbered list item at 1-based position (e.g., 1 → "1.", 2 → "2.")
     *
     * Use the LIST_ITEM_BULLET constant (-1) for bullet items.
     * This field is the sole gate for list item behavior; a non-zero value is required
     * for the span to be a list item. listMarkerOverride only has effect if this is non-zero.
     */
    int listItemOrdinal;
    
    /**
     * Indent depth for list items. 0 = no indent, 1+ = indent level.
     * Each level adds 16dp of left padding. Default is 1 for list items.
     */
    int listIndentLevel;
    
    /**
     * Custom list marker override. Overrides the default marker derived from listItemOrdinal
     * (e.g. "α. ", "① ", "→ ").
     * Note: This field only has an effect if listItemOrdinal is non-zero (i.e., the span is a list item).
     */
    @nullable String listMarkerOverride;

    /**
     * Whether this span represents a block container.
     * 
     * When isBlock is true, the text content of this span represents a standalone visual block
     * (such as an extra-box/example box) that should be rendered as a block instead of inline text.
     * This allows rendering block containers even if they do not contain newline ('\n') characters.
     */
    boolean isBlock;

    /**
     * Whether this span represents a table block.
     * 
     * When isTable is true, the text content of the span represents a structured table block.
     * The host app renders this in a formatted, aligned grid layout.
     * 
     * Table formatting guidelines:
     * - The text content should follow a flat markdown-like pipe grid layout.
     * - Each line in the block represents a row, formatted as: | Cell 1 | Cell 2 | ... |
     * - Formatting (such as bold headers, colors, or cell backgrounds) is specified explicitly
     *   by the plugin using nested styled spans within each cell range.
     * - Text, child styled spans, and ruby annotations inside cells are fully supported.
     * - Line breaks within cells are supported using HTML-style `<br>` or `<br/>` tags, which are
     *   parsed into newlines with child styled/ruby span indices automatically adjusted.
     * - Tables support nested blocks and can be optionally indented by setting listIndentLevel.
     */
    boolean isTable;

    /**
     * If non-null, this span represents a clickable range of text that will show the specified
     * additional text in a pop-up when clicked.
     */
    @nullable String hoverText;

    /**
     * If non-null, this span represents a hyperlinked text range with the specified target URL.
     * 
     * The host application automatically styles the hyperlinked span with a blue/cyan color
     * and a visual underline (TextDecoration.Underline).
     * 
     * Supported Link Formats:
     * 1. External URLs: Standard web URLs using the "http://" or "https://" scheme (e.g., "https://example.com"). Must be URL-encoded.
     * 2. Internal lookups ("lookup:{target}"): Triggers a new lookup for the specified target. `{target}` is passed verbatim to the plugin's `lookup` method when this span is clicked.
     * 3. Custom actions ("action:{payload}"): Triggers `executeCustomAction`. `{payload}` is passed verbatim to the plugin's `executeCustomAction` method when this span is clicked.
     */
    @nullable String linkUrl;
}
