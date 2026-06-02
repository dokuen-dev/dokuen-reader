package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Formatting directives for inline text styling.
 *
 * InlineStyle defines visual formatting properties that can be applied to character ranges
 * within StyledText. Multiple styles can be combined additively (e.g., bold + italic).
 *
 * This parcelable is for inline text formatting. Block-level layout properties
 * (lists, boxes, tables, containers) are handled separately by BlockSpan.
 *
 * InlineStyle uses "innermost-wins" override semantics:
 * - Boolean properties (bold, italic): Multiple spans combine additively.
 * - Scalar properties (fontSize, foregroundColor, textBackgroundColor): The innermost (narrowest range)
 *   span wins for overlapping ranges. If ranges are identical, the last span in the array wins.
 *
 * Example - Additive Boolean:
 *   StyledSpan(0, 10, InlineStyle(bold=true))
 *   StyledSpan(5, 10, InlineStyle(italic=true))
 *   Result: [0-5) bold, [5-10) bold+italic
 *
 * Example - Innermost-Wins Scalar:
 *   StyledSpan(0, 10, InlineStyle(foregroundColor=RED))
 *   StyledSpan(5, 10, InlineStyle(foregroundColor=BLUE))
 *   Result: [0-5) red, [5-10) blue (override)
 *
 * Color Sentinel Values:
 * - foregroundColor = 0: Use default text color (theme-dependent)
 * - textBackgroundColor = 0: No text background (transparent)
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
     * Foreground (text) color in ARGB format. 0 = unset (use default theme color).
     */
    int foregroundColor;

    /**
     * Text background color in ARGB format. 0 = unset (no background).
     * This highlights individual characters/words, distinct from block backgrounds.
     */
    int textBackgroundColor;

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
