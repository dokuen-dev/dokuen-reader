package io.github.dokuendev.dokuenreader.plugin.core

/**
 * Standard capability keys that plugins can report via getCapabilities() Bundle.
 * Plugins are free to add custom keys beyond these standard ones.
 */
object PluginCapabilityKeys {
    // Common capabilities

    /**
     * Set this to true or false to report to Dokuen whether this plugin
     * requires an internet connection to function. Examples include OCR
     * plugins that use a Cloud OCR provider as opposed to running on-device,
     * or dictionary plugins that fetch definitions from a server rather than
     * local dictionary files.
     */
    const val REQUIRES_INTERNET = "requires_internet"

    /**
     * Set this to true to report to Dokuen that this plugin provides a custom
     * configuration schema. If true, Dokuen will use the schema returned by
     * `getConfigSchema()` to dynamically build a settings UI for the plugin,
     * allowing the user to configure it.
     *
     * If false or unset, the settings UI for this plugin will be disabled.
     */
    const val HAS_CUSTOM_CONFIG = "has_custom_config"

    /**
     * A String array of BCP-47 codes of the languages supported by this
     * plugin. Currently supported languages are "ja" and "zh".
     *
     * If "ja" is in the array, this plugin will be recognized by and pluggable
     * into Dokuen Japanese Reader. If "zh" is in the array, this plugin will
     * be recognized by and pluggable into Dokuen Chinese Reader.
     *
     * If more than one language is supported, the plugin must read the
     * `PluginHostConfigKeys.LANGUAGE` key in the config Bundle passed to its
     * `initialize()` method to know which of the plugin's supported languages
     * to use for the given session.
     */
    const val SUPPORTED_LANGUAGES = "supported_languages"


    // OCR-specific capabilities

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


    // Dictionary-specific capabilities

    /**
     * If true, Dokuen will pass to the plugin the full sentence and the index
     * range of the tapped character or characters. The plugin is responsible
     * for segmenting the sentence and finding the word at the given index.
     *
     * If false, Dokuen will segment the sentence and pass the query word to
     * the plugin. The query word is usually the longest word or compound word
     * that overlaps with the tapped character or characters.
     */
    const val HANDLES_SEGMENTATION = "handles_segmentation"

    /**
     * If true, Dokuen will deinflect the query word and send the dictionary
     * form to the plugin for lookup.
     *
     * If false, Dokuen will send the full unmodified word.
     *
     * This setting has no effect if `HANDLES_SEGMENTATION` is true.
     * `HANDLES_SEGMENTATION` implies that it also handles deinflection.
     */
    const val REQUIRES_DICTIONARY_FORM = "requires_dictionary_form"
}
