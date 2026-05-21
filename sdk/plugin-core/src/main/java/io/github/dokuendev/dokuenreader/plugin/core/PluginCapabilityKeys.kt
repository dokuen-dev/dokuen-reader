package io.github.dokuendev.dokuenreader.plugin.core

/**
 * Standard capability keys that plugins can report via getCapabilities() Bundle.
 * Plugins are free to add custom keys beyond these standard ones.
 */
object PluginCapabilityKeys {

    // -------------------------------------------------------------------------
    // Common capabilities (all plugin types)
    // -------------------------------------------------------------------------

    /**
     * Set this to true or false to report to Dokuen whether this plugin
     * requires an internet connection to function. Examples include OCR
     * plugins that use a Cloud OCR provider as opposed to running on-device,
     * or dictionary plugins that fetch definitions from a server rather than
     * local dictionary files.
     */
    const val REQUIRES_INTERNET = "requires_internet"

    /**
     * A String array of BCP-47 codes of the source languages supported by this
     * plugin. Used by **OCR plugins** to declare which scripts they can recognise.
     * Currently supported values are `"ja"` and `"zh"`.
     *
     * If "ja" is in the array, this plugin will be recognized by and pluggable
     * into Dokuen Japanese Reader. If "zh" is in the array, this plugin will
     * be recognized by and pluggable into Dokuen Chinese Reader.
     *
     * If more than one language is supported, the plugin must read the
     * `PluginHostConfigKeys.LANGUAGE` key in the config Bundle passed to its
     * `initialize()` method to know which of the plugin's supported languages
     * to use for the given session.
     *
     * **Dictionary plugins** should use [SUPPORTED_SOURCE_LANGUAGES] and
     * [SUPPORTED_TARGET_LANGUAGES] instead, which separate the language being
     * looked up from the language definitions are returned in.
     */
    const val SUPPORTED_LANGUAGES = "supported_languages"


    // -------------------------------------------------------------------------
    // OCR-specific capabilities
    // -------------------------------------------------------------------------

    /**
     * If true, the "vertical" option in the "Text Direction" settings screen
     * will be enabled when this plugin is selected. The user's selection
     * will be passed to each `IOcrService.process()` call. If "vertical" is
     * selected, the plugin is expected to send back `OcrBlock`s with
     * `isVertical = true` and with symbols ordered top-to-bottom and
     * right-to-left, in Japanese tategaki reading order.
     *
     * If false, the "Text Direction" setting in the Dokuen UI will be disabled
     * when this plugin is selected, and the textDirection argument to
     * `IOcrService.process()` will always be "horizontal". The plugin should
     * set `isVertical = false` on all returned `OcrBlock`s.
     */
    const val SUPPORTS_VERTICAL_TEXT = "supports_vertical_text"

    /**
     * If true, the "auto" option in the "Text Direction" settings screen
     * will be enabled when this plugin is selected. The user's selection
     * will be passed to each `IOcrService.process()` call. If "auto" is
     * selected, the plugin must report the detected text direction of each
     * `OcrBlock` via the per-block `isVertical` member.
     *
     * If false, the "auto" option in the "Text Direction" settings screen
     * will be disabled when this plugin is selected, and the textDirection
     * argument to `IOcrService.process()` will never be "auto".
     */
    const val SUPPORTS_AUTO_TEXT_DIRECTION = "supports_auto_text_direction"


    // -------------------------------------------------------------------------
    // Dictionary-specific capabilities
    // -------------------------------------------------------------------------

    /**
     * A String array of BCP-47 codes of the **source** languages this dictionary
     * plugin can look up. Currently supported values for Dokuen are `"ja"` and
     * `"zh"`.
     *
     * Dokuen Japanese Reader only surfaces plugins whose array contains `"ja"`.
     * A plugin that supports multiple source languages (e.g. a machine-translation
     * fallback covering both Japanese and Chinese) should list all supported codes.
     *
     * If more than one language is supported, the plugin must read the
     * `PluginHostConfigKeys.LANGUAGE` key in the config Bundle passed to its
     * `initialize()` method to know which of the plugin's supported languages
     * to use for the given session.
     */
    const val SUPPORTED_SOURCE_LANGUAGES = "supported_source_languages"

    /**
     * A String array of BCP-47 codes of the **target** languages this dictionary
     * plugin can produce definitions in (e.g. `["en", "es", "fr"]`).
     *
     * When this array contains more than one value, Dokuen automatically adds a
     * target-language selector to the plugin's settings UI. The user's choice is
     * delivered to the plugin in the `PluginHostConfigKeys.TARGET_LANGUAGE` key
     * of the config Bundle passed to `initialize()`. When the array contains
     * exactly one value, no selector is shown but the value is still delivered via
     * `TARGET_LANGUAGE` for consistency.
     */
    const val SUPPORTED_TARGET_LANGUAGES = "supported_target_languages"

    /**
     * Controls whether Dokuen passes the full OCR block text with cursor indices
     * to the plugin, or passes only the selected text.
     *
     * **`HANDLES_SEGMENTATION = true`**
     *
     * The plugin receives `contextText` and `cursorStartIndex`/`cursorEndIndex`
     * indicating where the user's selection sits within that text. The plugin is
     * responsible for segmenting the text and finding the word at the cursor, as
     * well as for deinflection (`REQUIRES_DICTIONARY_FORM` is ignored).
     *
     * What `contextText` contains depends on the shape of the user's selection:
     *
     * - **Single tap or continuous drag within one OCR block:** `contextText` is the
     *   full OCR block text. `cursorStartIndex` and `cursorEndIndex` identify the
     *   selected characters within it. This gives the plugin surrounding context for
     *   disambiguation. A normal left-to-right drag that sweeps over adjacent words
     *   qualifies here just as a single tap does.
     *
     * - **Disjoint selection or selection spanning multiple OCR blocks:** `contextText`
     *   is only the concatenated selected text (same as `HANDLES_SEGMENTATION = false`),
     *   with `cursorStartIndex = 0` and `cursorEndIndex = contextText.length`. Full
     *   block context cannot be provided because a gap in the selection, or characters
     *   from different blocks, means there is no single coherent text that honestly
     *   represents the cursor position.
     *
     * In all cases the invariant `contextText.substring(cursorStartIndex, cursorEndIndex)`
     * yields the primary text of interest.
     *
     * `HANDLES_SEGMENTATION = true` is required for translator dictionary plugins.
     * Translator sources receive `tappedWord` directly and must be capable of handling
     * raw text without pre-segmentation by the host.
     *
     * **`HANDLES_SEGMENTATION = false` (default)**
     *
     * Dokuen passes only the selected text as `contextText`, with
     * `cursorStartIndex = 0` and `cursorEndIndex = contextText.length`. No
     * segmentation is performed by the host. The text is optionally deinflected
     * first if `REQUIRES_DICTIONARY_FORM = true`.
     */
    const val HANDLES_SEGMENTATION = "handles_segmentation"

    /**
     * Controls whether Dokuen deinflects the selected word to its dictionary form
     * before passing it to the plugin.
     *
     * This capability has no effect when `HANDLES_SEGMENTATION = true`; in that case
     * the plugin receives the raw selected text (or full block) and is responsible for
     * its own deinflection.
     *
     * **`REQUIRES_DICTIONARY_FORM = true`**
     *
     * The host deinflects the selected word using the built-in deinflector and
     * passes the dictionary form (e.g. "食べる" from "食べて") to the plugin.
     *
     * **`REQUIRES_DICTIONARY_FORM = false` (default)**
     *
     * The plugin receives the surface form as selected by the user (e.g. "食べて").
     * The plugin is responsible for any deinflection it needs.
     */
    const val REQUIRES_DICTIONARY_FORM = "requires_dictionary_form"
}
