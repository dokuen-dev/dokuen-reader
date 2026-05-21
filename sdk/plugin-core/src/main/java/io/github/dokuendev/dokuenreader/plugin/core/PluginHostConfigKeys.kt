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

    /**
     * A BCP-47 language code (String) indicating the target language the user has selected
     * for dictionary definitions.
     *
     * This key is provided to **dictionary plugins** when the plugin declares multiple target
     * languages in `PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES`. When a plugin supports
     * multiple target languages (e.g. a Japanese→English/Spanish/French dictionary), Dokuen
     * automatically adds a target-language selector to the plugin's settings UI. The user's
     * choice is delivered via this key.
     *
     * When a plugin declares exactly one target language, this key is still provided with that
     * single value for consistency.
     *
     * The plugin should use this value to produce definitions in the selected language.
     *
     * Example values: "en", "es", "fr", "de", "zh"
     *
     * This key is only provided to dictionary plugins.
     */
    const val TARGET_LANGUAGE = "_target_language"

    /**
     * A String indicating the current system UI theme, either "dark" or "light".
     *
     * This key is provided to **dictionary plugins** so they can choose text colors that are
     * legible against the Dokuen UI background. When using `InlineStyle` to set foreground
     * and background colors in `StyledText`, plugins should consider the current theme to
     * ensure sufficient contrast.
     *
     * Text runs without an explicit foreground color automatically receive the host app's
     * default contrasting color for the active theme.
     *
     * Possible values:
     * - "dark" - Dark theme is active (light text on dark background)
     * - "light" - Light theme is active (dark text on light background)
     *
     * This key is only provided to dictionary plugins.
     */
    const val UI_THEME = "_ui_theme"
}
