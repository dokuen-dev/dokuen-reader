package io.github.dokuendev.dokuenreader.dictionary;

import android.os.Bundle;
import io.github.dokuendev.dokuenreader.dictionary.IDictionaryCallback;
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema;
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback;

/**
 * Dictionary Plugin Interface
 *
 * Plugin Metadata:
 * Plugin information (name, version, author, description, license, website, support email)
 * must be declared in AndroidManifest.xml using meta-data tags. This allows Dokuen to
 * discover plugin information without binding to the service.
 * Additionally, the plugin must declare the category tags:
 *   - "io.github.dokuendev.dokuenreader.category.DICTIONARY" (for standard dictionary lookups)
 *   - "io.github.dokuendev.dokuenreader.category.TRANSLATOR" (for translations)
 * inside its <intent-filter> to classify its plugin types.
 *
 * Lifecycle:
 * 1. Settings Menu (Discovery & Configuration):
 *    - Query PackageManager for services with intent filter
 *    - Read metadata from AndroidManifest
 *    - bind() -> getCapabilities() -> getConfigSchema() -> unbind()
 *    - User enters config, app saves it locally
 *
 * 2. Reading Session (Initialization & Execution):
 *    - bind() -> initialize() -> lookup() [multiple times] -> shutdown() -> unbind()
 */
interface IDictionaryService {
    // --- Discovery Phase (Settings Menu) ---

    /**
     * Returns a Bundle of key-value pairs describing plugin capabilities.
     * Use keys from PluginCapabilityKeys or define custom ones.
     *
     * Example:
     *   bundle.putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, false)
     *   bundle.putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, ["ja"])
     *   bundle.putStringArray(PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES, ["en", "es"])
     */
    Bundle getCapabilities();

    /**
     * Returns the configuration schema for user-configurable settings.
     * Return null or empty schema if no configuration is needed.
     */
    PluginConfigSchema getConfigSchema();

    // --- Initialization Phase (Reading Session Start) ---

    /**
     * Initialize the plugin with user configuration.
     *
     * This is called when the user starts a reading session.
     *
     * The plugin should:
     * 1. Validate the configuration
     * 2. If valid, load heavy resources (network connections, caches)
     * 3. Return InitResult with success=true
     * 4. If invalid, return InitResult with success=false and error message
     *
     * @param config A Bundle containing all fields populated by the user according to
     *               getConfigSchema(), and Host keys as defined in PluginHostConfigKeys:
     *               - LANGUAGE: The active session source language (e.g., "ja")
     *               - TARGET_LANGUAGE: The user-selected target language (for multi-target plugins)
     *               - USER_LOCALE: The user's UI language for localizing messages
     *               - UI_THEME: "dark" or "light" for color choices
     * @param callback Callback to receive the initialization result.
     */
    oneway void initialize(in Bundle config, IInitCallback callback);

    // --- Execution Phase (During Reading Session) ---

    /**
     * Look up a word or phrase and return definitions.
     *
     * The meaning of contextText, cursorStartIndex, and cursorEndIndex depends on
     * the plugin's HANDLES_SEGMENTATION capability and the shape of the selection;
     * see PluginCapabilityKeys.HANDLES_SEGMENTATION for the full specification.
     *
     * The invariant contextText.substring(cursorStartIndex, cursorEndIndex) always
     * yields the primary text of interest.
     *
     * @param contextText      The text context (full block or selected text).
     * @param cursorStartIndex Inclusive start of the selection in contextText.
     * @param cursorEndIndex   Exclusive end of the selection in contextText.
     * @param callback         Callback to receive the result or error.
     */
    oneway void lookup(String contextText, int cursorStartIndex, int cursorEndIndex,
                       IDictionaryCallback callback);

    // --- Cleanup Phase (Reading Session End) ---

    /**
     * Release all resources allocated during initialize().
     *
     * Called when the user ends the reading session.
     * - Close network connections
     * - Clear caches
     * - Free processing buffers
     */
    void shutdown();
}
