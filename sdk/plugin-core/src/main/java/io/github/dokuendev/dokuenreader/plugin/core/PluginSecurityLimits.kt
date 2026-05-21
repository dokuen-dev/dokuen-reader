package io.github.dokuendev.dokuenreader.plugin.core

/**
 * Security limits for plugin data to prevent memory exhaustion and DoS attacks.
 *
 * These constants define reasonable upper bounds for data returned by plugins.
 *
 * **For Plugin Authors:**
 * Be aware of these limits when designing your plugin. Returning data that exceeds
 * these limits may cause the host app to reject your results.
 *
 * **For Host App Developers:**
 * Enforce these limits when processing plugin responses to protect against
 * malicious or buggy plugins.
 */
object PluginSecurityLimits {
    /**
     * Maximum number of OCR blocks that can be returned in a single OcrResult.
     *
     * If your OCR result contains more blocks than this, consider:
     * - Merging adjacent blocks with the same orientation
     * - Filtering out low-confidence results
     */
    const val MAX_OCR_BLOCKS = 1_000

    /**
     * Maximum text length per OCR block in characters.
     *
     * If a single block exceeds this limit, consider splitting it into
     * multiple blocks at natural boundaries (sentences, paragraphs).
     */
    const val MAX_TEXT_LENGTH_PER_BLOCK = 10_000

    /**
     * Maximum number of dictionary entries that can be returned in a single result.
     *
     * For dictionary plugins: limit your results to the most relevant entries.
     */
    const val MAX_DICTIONARY_ENTRIES = 100

    /**
     * Maximum text length per dictionary entry body in characters.
     *
     * If a single entry's body exceeds this limit, the host app will truncate it.
     */
    const val MAX_BODY_LENGTH = 50_000

    /**
     * Maximum number of spans (StyledSpan + RubySpan) per dictionary entry.
     *
     * This limit applies to the combined total of:
     * - StyledSpan objects in the body
     * - RubySpan objects in the body
     * - RubySpan objects in the pronunciation
     */
    const val MAX_SPANS_PER_ENTRY = 500

    /**
     * Default timeout for plugin operations in milliseconds (30 seconds).
     *
     * Plugin operations should complete within this time. The host app may
     * cancel operations that exceed this timeout.
     */
    const val OPERATION_TIMEOUT_MS = 30_000L

    /**
     * Maximum size for configuration string values in characters.
     *
     * Configuration values (API keys, URLs, etc.) should not exceed this length.
     */
    const val MAX_CONFIG_STRING_LENGTH = 1_000
}
