package io.github.dokuendev.dokuenreader.plugin.core

/**
 * Standard keys provided by the Dokuen host app in the config Bundle passed to `initialize()`.
 * 
 * All host config keys start with an underscore (_) to prevent clashes with user configuration
 * keys defined in a plugin's PluginConfigSchema.
 */
object PluginHostConfigKeys {

    /**
     * A BCP-47 language code (String) indicating which language the reader is currently
     * processing. As of the current version, this will only ever be "ja" or "zh".
     * 
     * This key may be omitted if the plugin reported only a single supported language in its
     * `PluginCapabilityKeys.SUPPORTED_LANGUAGES` capabilities. In that case, the language is
     * assumed to be the single supported language.
     * 
     * The plugin should use this value to dynamically switch its internal configurations (e.g.
     * which URL endpoints to hit, or which ML model to load) to match the current reading session.
     */
    const val LANGUAGE = "_language"

    /**
     * A locale tag (String) indicating the user's preferred UI language.
     * 
     * In the current version, this will be one of:
     * - "en" (English)
     * - "ja" (Japanese)
     * - "de" (German)
     * - "es" (Spanish)
     * - "fr" (French)
     * - "in" (Indonesian)
     * - "ko" (Korean)
     * - "pt-BR" (Brazilian Portuguese)
     * - "sv" (Swedish)
     * - "th" (Thai)
     * - "vi" (Vietnamese)
     * - "zh" (Chinese Simplified)
     * - "zh-TW" (Chinese Traditional)
     * 
     * This is provided so plugins can localize error messages, configuration field descriptions,
     * and other user-facing text.
     * 
     * If the plugin does not support the requested locale, it should fall back to English ("en").
     * 
     * This key is always provided by the host app.
     */
    const val USER_LOCALE = "_user_locale"
}
