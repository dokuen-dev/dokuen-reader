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
 * - listItemOrdinal = 0: Not a list item
 * - listItemOrdinal > 0: 1-based position in a numbered list (e.g., 1 = "1.", 2 = "2.")
 * - listMarkerOverride: Custom marker string (e.g., "• ", "α. ", "① ") overrides ordinal formatting
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
     * List item ordinal. 0 = not a list item, >0 = 1-based position.
     * Used when listMarkerOverride is null to generate default markers (e.g., "1.", "2.").
     */
    int listItemOrdinal;
    
    /**
     * Indent depth for list items. 0 = no indent, 1+ = indent level.
     * Each level adds 16dp of left padding. Default is 1 for list items.
     */
    int listIndentLevel;
    
    /**
     * Custom list marker override. If non-null, this exact string is used
     * as the list marker instead of formatting listItemOrdinal.
     * Examples: "• ", "α. ", "① ", "→ "
     */
    @nullable String listMarkerOverride;
}
