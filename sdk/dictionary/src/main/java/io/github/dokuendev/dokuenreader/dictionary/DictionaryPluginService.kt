package io.github.dokuendev.dokuenreader.dictionary

import android.os.Bundle
import android.os.IBinder
import io.github.dokuendev.dokuenreader.plugin.core.BasePluginService
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema
import io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Base class for building Dokuen Dictionary plugins.
 *
 * Extend this class and implement [onLookup] to create a plugin that provides
 * dictionary definitions for the Dokuen Reader application. The SDK handles all service
 * lifecycle, security verification, coroutine management, and error reporting
 * automatically.
 *
 * ## Minimal Example
 *
 * ```kotlin
 * class MyDictionaryPlugin : DictionaryPluginService() {
 *
 *     override val capabilities = Bundle().apply {
 *         putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, arrayOf("ja"))
 *         putStringArray(PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES, arrayOf("en"))
 *         putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, false)
 *         putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, true)
 *     }
 *
 *     override suspend fun onLookup(
 *         contextText: String,
 *         cursorStartIndex: Int,
 *         cursorEndIndex: Int
 *     ): DictionaryResult {
 *         // contextText.substring(cursorStartIndex, cursorEndIndex) is always the
 *         // primary text of interest, regardless of HANDLES_SEGMENTATION setting
 *         // or interaction type.
 *         val word = contextText.substring(cursorStartIndex, cursorEndIndex)
 *         val definitions = dictionaryEngine.lookup(word)
 *
 *         val entries = definitions.map { def ->
 *             DictionaryEntry(
 *                 headword = def.word,
 *                 pronunciation = null,
 *                 body = StyledText(text = def.definition)
 *             )
 *         }
 *
 *         return DictionaryResult(entries)
 *     }
 * }
 * ```
 *
 * ## Plugin Lifecycle
 *
 * A plugin goes through two phases during its lifetime:
 *
 * ### 1. Discovery & Configuration (Settings Menu)
 *
 * When the user opens the Dokuen settings screen, the app queries installed plugins
 * and reads their [capabilities] and [configSchema]. This allows Dokuen to display
 * plugin information and build a dynamic settings UI for user-configurable fields
 * (e.g., API keys).
 *
 * ### 2. Reading Session (Initialization, Execution, Cleanup)
 *
 * When the user starts a reading session:
 *
 * 1. **[onInitialize]** is called once with the user's configuration. Load heavy
 *    resources here (network clients, caches). Return success or failure.
 * 2. **[onLookup]** is called each time the user taps or drags to select a word.
 *    Return the [DictionaryResult] with matching entries.
 * 3. **[onShutdown]** is called when the session ends. Release all resources
 *    allocated during [onInitialize].
 *
 * ## Error Handling
 *
 * If [onLookup] or [onInitialize] throw an exception, the SDK catches it and
 * reports it to the host app with an appropriate error code. If you want to communicate
 * a specific error (e.g., network failure, word not found), throw a [DictionaryException]:
 *
 * ```kotlin
 * throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "No definition found")
 * ```
 *
 * Unhandled exceptions are reported as [DictionaryErrorCode.INTERNAL_ERROR].
 * [CancellationException] is handled separately and reported as [DictionaryErrorCode.CANCELED].
 *
 * Error messages should be user-friendly and actionable. Localize messages based on
 * [PluginHostConfigKeys.USER_LOCALE] if possible.
 *
 * ## AndroidManifest.xml
 *
 * Register your service with the required intent filter and metadata:
 *
 * ```xml
 * <service android:name=".MyDictionaryPlugin" android:exported="true">
 *     <intent-filter>
 *         <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
 *         <!-- Declare classification categories: Dictionary, Translator, or both -->
 *         <category android:name="io.github.dokuendev.dokuenreader.category.DICTIONARY" />
 *         <category android:name="io.github.dokuendev.dokuenreader.category.TRANSLATOR" />
 *     </intent-filter>
 *
 *     <!-- Required metadata. Plugins missing any of these will not be recognized. -->
 *     <meta-data android:name="plugin_name" android:value="My Dictionary Plugin" />
 *     <meta-data android:name="plugin_version" android:value="1.0.0" />
 *     <meta-data android:name="plugin_author" android:value="Your Name" />
 *     <meta-data android:name="plugin_description" android:value="Japanese-English dictionary" />
 *     <meta-data android:name="plugin_license" android:value="Apache 2.0" />
 *
 *     <!-- Optional but recommended -->
 *     <meta-data android:name="plugin_website" android:value="https://example.com" />
 *     <meta-data android:name="plugin_support_email" android:value="support@example.com" />
 * </service>
 * ```
 *
 * @see DictionaryResult The data structure returned for lookup results.
 * @see DictionaryErrorCode Error codes for reporting failures.
 * @see PluginCapabilityKeys Standard capability keys for [capabilities].
 */
abstract class DictionaryPluginService : BasePluginService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Configuration fields presented to the user in the Dokuen Settings UI.
     *
     * Override this property to declare settings your plugin needs. Each [ConfigField]
     * becomes an input in Dokuen's plugin settings screen. The values entered by the
     * user are delivered to [onInitialize] in the `config` Bundle, keyed by
     * [ConfigField.key].
     *
     * This property is optional. If your plugin does not require any user configuration,
     * simply do not override this property (it defaults to an empty list).
     *
     * User-defined config keys must not start with an underscore (`_`). Keys starting
     * with `_` are reserved for host-provided values (see [PluginHostConfigKeys]).
     *
     * Example:
     * ```kotlin
     * override val configSchema = listOf(
     *     ConfigField(
     *         key = "api_key",
     *         displayName = "API Key",
     *         description = "Required for online dictionary access",
     *         type = ConfigFieldType.STRING,
     *         defaultValue = null,
     *         isRequired = true
     *     )
     * )
     * ```
     */
    open val configSchema: List<ConfigField> = emptyList()

    /**
     * A [Bundle] of key-value pairs describing this plugin's capabilities.
     *
     * Override this property to report what your plugin supports. Dokuen reads these
     * capabilities to determine compatibility and to configure the dictionary lookup flow.
     *
     * Use the standard keys from [PluginCapabilityKeys] and add any custom keys as needed.
     *
     * Example:
     * ```kotlin
     * override val capabilities = Bundle().apply {
     *     putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, arrayOf("ja"))
     *     putStringArray(PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES, arrayOf("en", "es"))
     *     putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, true)
     *     putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
     * }
     * ```
     *
     * @see PluginCapabilityKeys for the complete list of standard keys and their meanings.
     */
    open val capabilities: Bundle = Bundle()

    /**
     * Called once when the user starts a reading session.
     *
     * Use this method to perform heavy one-time setup such as loading dictionaries,
     * establishing network connections, or allocating caches. This method runs in a
     * coroutine, so you can call suspending functions directly.
     *
     * The [config] Bundle contains two categories of keys:
     *
     * 1. **User configuration values**: All settings the user entered through the
     *    plugin settings UI, keyed by the [ConfigField.key] strings from [configSchema].
     * 2. **Host configuration values**: Standard keys provided by Dokuen, as defined in
     *    [PluginHostConfigKeys]:
     *    - `LANGUAGE`: The active session source language (e.g., "ja")
     *    - `TARGET_LANGUAGE`: The user-selected target language (for multi-target plugins)
     *    - `USER_LOCALE`: The user's preferred UI locale for localizing messages
     *    - `UI_THEME`: "dark" or "light" for choosing legible colors
     *
     * If initialization fails (e.g., invalid API key, dictionary not found), return
     * [InitResultFactory.failure] with a user-friendly error message. Dokuen will
     * display this message and will not proceed with the reading session.
     *
     * @param config Bundle containing user and host configuration values, or null
     *   if no configuration was provided.
     * @return An [InitResult] indicating success or failure. Use [InitResultFactory]
     *   to create the result.
     */
    open suspend fun onInitialize(config: Bundle?): InitResult {
        return InitResultFactory.success()
    }

    /**
     * Called each time the user taps or drags to select a word for dictionary lookup.
     *
     * The SDK provides the text context and the cursor position of the selection within
     * it. Return a [DictionaryResult] containing all matching dictionary entries.
     *
     * ### The contextText / cursor contract
     *
     * The expression `contextText.substring(cursorStartIndex, cursorEndIndex)` always
     * yields the primary text of interest, the word or phrase the user selected.
     * This invariant holds for all interaction types and all capability combinations.
     *
     * What `contextText` contains depends on your plugin's `HANDLES_SEGMENTATION`
     * capability and the shape of the user's selection:
     *
     * **`HANDLES_SEGMENTATION = false` (default)**
     *
     * `contextText` is always the selected text only. `cursorStartIndex` is always 0
     * and `cursorEndIndex` is always `contextText.length`. The host optionally
     * deinflects this text first if `REQUIRES_DICTIONARY_FORM = true`.
     *
     * **`HANDLES_SEGMENTATION = true` - contiguous selection within one OCR block**
     *
     * This applies to single taps and to normal continuous drag selections where the
     * user sweeps over adjacent characters without gaps. In these cases, `contextText`
     * is the full OCR block text, and `cursorStartIndex`/`cursorEndIndex` identify
     * the selected characters within it. The plugin receives the surrounding sentence
     * for disambiguation.
     *
     * Example: single tap on "食べて" in "今日ご飯食べてから出かけた":
     * ```
     * contextText      = "今日ご飯食べてから出かけた"
     * cursorStartIndex = 4
     * cursorEndIndex   = 7
     * ```
     *
     * Example: continuous drag selecting "食べてから" in the same sentence:
     * ```
     * contextText      = "今日ご飯食べてから出かけた"
     * cursorStartIndex = 4
     * cursorEndIndex   = 9
     * ```
     *
     * **`HANDLES_SEGMENTATION = true` - disjoint selection or multi-block selection**
     *
     * When the user makes a disjoint selection (characters with a gap between them,
     * e.g. selecting "食" and "る" while skipping "べて") or selects across multiple
     * OCR blocks, the host cannot provide a single coherent block text with honest
     * cursor indices. In this case the plugin falls back to the same behavior as
     * `HANDLES_SEGMENTATION = false`: `contextText` is the concatenated selected text,
     * `cursorStartIndex = 0`, `cursorEndIndex = contextText.length`.
     *
     * ### Return value
     *
     * Return a [DictionaryResult] with a list of [DictionaryEntry] objects. Each entry
     * should include:
     * - [DictionaryEntry.headword]: The dictionary form of the word
     * - [DictionaryEntry.pronunciation]: Optional ruby annotations for pronunciation
     * - [DictionaryEntry.body]: [StyledText] with the full definition content
     *
     * If no matches are found, return an empty [DictionaryResult]. Do not return null
     * and do not throw [DictionaryException] for the "not found" case.
     *
     * ### Timeout
     *
     * This method must complete within 30 seconds. The host enforces a timeout and
     * will ignore results that arrive after it expires.
     *
     * @param contextText The text context. See above for how this varies by capability
     *   and selection type.
     * @param cursorStartIndex Inclusive start of the selected region in [contextText].
     * @param cursorEndIndex Exclusive end of the selected region in [contextText].
     * @return A [DictionaryResult] with matching entries, or empty if no matches found.
     * @throws DictionaryException if a domain-specific error occurs (e.g., network
     *   failure). The error code and message will be forwarded to the user.
     */
    abstract suspend fun onLookup(
        contextText: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int
    ): DictionaryResult

    /**
     * Called when the reading session ends.
     *
     * Release all resources allocated during [onInitialize]: close network connections,
     * clear caches, free processing buffers, etc.
     *
     * This method may be called even if [onInitialize] was not called or failed. Your
     * implementation should be safe to call in any state.
     *
     * After this method returns, the service may be unbound and destroyed by the
     * system. A new session will start a fresh [onInitialize] call.
     */
    open fun onShutdown() {}

    // =========================================================================
    // Internal: AIDL bridge -- plugin authors do not interact with anything below.
    // =========================================================================

    private val binder = object : IDictionaryService.Stub() {
        override fun getCapabilities(): Bundle = this@DictionaryPluginService.capabilities

        override fun getConfigSchema(): PluginConfigSchema {
            return PluginConfigSchema(this@DictionaryPluginService.configSchema)
        }

        override fun initialize(config: Bundle?, callback: IInitCallback?) {
            if (!verifyAndRegisterCallingApp()) {
                callback?.onFailure("Calling app is not authorized")
                return
            }
            serviceScope.launch {
                try {
                    val result = onInitialize(config)
                    if (result.success) {
                        callback?.onSuccess(result)
                    } else {
                        callback?.onFailure(result.errorMessage ?: "Initialization failed")
                    }
                } catch (e: Exception) {
                    callback?.onFailure(e.message ?: "Initialization failed")
                }
            }
        }

        override fun lookup(
            contextText: String?,
            cursorStartIndex: Int,
            cursorEndIndex: Int,
            callback: IDictionaryCallback?
        ) {
            if (!isCallingAppRegistered()) {
                callback?.onFailure(DictionaryErrorCode.INTERNAL_ERROR, "Calling app not verified")
                return
            }

            if (contextText == null) {
                callback?.onFailure(DictionaryErrorCode.INVALID_QUERY, "Missing required parameters")
                return
            }

            serviceScope.launch {
                try {
                    val result = onLookup(contextText, cursorStartIndex, cursorEndIndex)
                    callback?.onSuccess(result)
                } catch (_: CancellationException) {
                    callback?.onFailure(DictionaryErrorCode.CANCELED, "Lookup canceled")
                } catch (e: DictionaryException) {
                    callback?.onFailure(e.errorCode, e.message ?: "Lookup failed")
                } catch (e: Exception) {
                    callback?.onFailure(DictionaryErrorCode.INTERNAL_ERROR, e.message ?: "Unknown error")
                }
            }
        }

        override fun shutdown() {
            if (!isCallingAppRegistered()) return
            serviceScope.coroutineContext.cancelChildren()
            onShutdown()
        }
    }

    override fun getBinder(): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
