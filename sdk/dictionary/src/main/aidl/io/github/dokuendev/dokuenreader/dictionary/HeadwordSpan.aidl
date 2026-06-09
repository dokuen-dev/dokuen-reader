package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Associates a character range in a headword with a link target.
 *
 * HeadwordSpan is intentionally narrower than StyledSpan. Headwords have
 * constrained rendering (size, font, stroke-order mode) so general inline
 * styling properties are not supported here.
 *
 * Supported link formats are identical to InlineStyle.linkUrl:
 * 1. External URLs: Standard web URLs using the "http://" or "https://" scheme
 *    (e.g., "https://example.com"). Must be URL-encoded.
 * 2. Internal lookups ("lookup:{target}"): Triggers a new lookup for the specified
 *    target. {target} is passed verbatim to the plugin's onLookup method.
 * 3. Custom actions ("action:{payload}"): Triggers executeCustomAction. {payload}
 *    is passed verbatim to the plugin's onExecuteCustomAction method.
 *
 * Character Range:
 * - startIndex: Inclusive start position in the headword string
 * - endIndex: Exclusive end position in the headword string
 * - Range must be valid: 0 <= startIndex < endIndex <= headword.length
 * - Ranges must not overlap; if they do, the behavior is undefined
 * - Out-of-bounds spans are ignored by the host app
 *
 * Example - "AB" where "A" and "B" are separate lookup links:
 *   headword = "AB"
 *   headwordSpans = [
 *       HeadwordSpan(0, 1, "lookup:A"),
 *       HeadwordSpan(1, 2, "lookup:B")
 *   ]
 *
 * Example - Compound word where each component links to its own entry:
 *   headword = "日本語"
 *   headwordSpans = [
 *       HeadwordSpan(0, 2, "lookup:日本"),
 *       HeadwordSpan(2, 3, "lookup:語")
 *   ]
 */
parcelable HeadwordSpan {
    /**
     * Start index in the headword string (inclusive).
     */
    int startIndex;

    /**
     * End index in the headword string (exclusive).
     */
    int endIndex;

    /**
     * The link target. Must not be null or empty.
     * See class-level documentation for supported link formats.
     */
    String linkUrl;
}
