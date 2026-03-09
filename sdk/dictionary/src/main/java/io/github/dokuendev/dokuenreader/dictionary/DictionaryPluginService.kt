package io.github.dokuendev.dokuenreader.dictionary

import android.os.Bundle
import android.os.IBinder
import io.github.dokuendev.dokuenreader.plugin.core.BasePluginService
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema
import kotlinx.coroutines.*

/**
 * Base class for building Dokuen Dictionary plugins.
 *
 * Extend this class and implement [onLookup] to create a plugin that provides word
 * definitions for the Dokuen Reader application. The SDK handles service lifecycle,
 * security verification, coroutine management, and error reporting automatically.
 *
 * > **Note:** This API is an early placeholder. The return type and parameters of
 * > [onLookup] are intentionally kept simple for now. Future versions will support
 * > structured entries with readings, parts of speech, example sentences, etc.
 *
 * ## Minimal Example
 *
 * ```kotlin
 * class MyDictionaryPlugin : DictionaryPluginService() {
 *
 *     override suspend fun onLookup(query: String): String {
 *         return myDictionaryDb.findDefinition(query)
 *             ?: throw DictionaryException(
 *                 DictionaryErrorCode.WORD_NOT_FOUND,
 *                 "No entry found for \"$query\""
 *             )
 *     }
 * }
 * ```
 *
 * ## Plugin Lifecycle
 *
 * ### 1. Discovery & Configuration (Settings Menu)
 *
 * When the user opens the Dokuen settings screen, the app reads [capabilities] and
 * [configSchema] to display plugin information and build a dynamic settings UI.
 *
 * ### 2. Reading Session (Initialization, Execution, Cleanup)
 *
 * 1. **[onInitialize]**: Called once when the session starts. Load dictionaries,
 *    open database connections, or prepare network clients here.
 * 2. **[onLookup]**: Called each time the user taps a word for lookup.
 * 3. **[onShutdown]**: Called when the session ends. Release all resources.
 *
 * ## Error Handling
 *
 * Throw a [DictionaryException] with an appropriate [DictionaryErrorCode] to report
 * domain-specific errors. Unhandled exceptions are reported as
 * [DictionaryErrorCode.INTERNAL_ERROR].
 *
 * ## AndroidManifest.xml
 *
 * ```xml
 * <service android:name=".MyDictionaryPlugin" android:exported="true">
 *     <intent-filter>
 *         <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
 *     </intent-filter>
 *
 *     <meta-data android:name="plugin_name" android:value="My Dictionary" />
 *     <meta-data android:name="plugin_version" android:value="1.0.0" />
 *     <meta-data android:name="plugin_author" android:value="Your Name" />
 *     <meta-data android:name="plugin_description" android:value="Offline J-E dictionary" />
 *     <meta-data android:name="plugin_license" android:value="MIT" />
 * </service>
 * ```
 *
 * @see DictionaryErrorCode Error codes for reporting failures.
 * @see PluginCapabilityKeys Standard capability keys for [capabilities].
 */
abstract class DictionaryPluginService : BasePluginService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Configuration fields presented to the user in the Dokuen Settings UI.
     *
     * Override this property to declare settings your plugin needs (e.g., API keys
     * for cloud dictionary services). Each [ConfigField] becomes an input in Dokuen's
     * plugin settings screen.
     *
     * @see OcrPluginService.configSchema for a detailed example.
     */
    open val configSchema: List<ConfigField> = emptyList()

    /**
     * A [Bundle] of key-value pairs describing this plugin's capabilities.
     *
     * Override this property to report what your plugin supports. Relevant standard
     * keys include:
     * - [PluginCapabilityKeys.SUPPORTED_LANGUAGES][io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys.SUPPORTED_LANGUAGES]
     * - [PluginCapabilityKeys.REQUIRES_INTERNET][io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys.REQUIRES_INTERNET]
     * - [PluginCapabilityKeys.HANDLES_SEGMENTATION][io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys.HANDLES_SEGMENTATION]
     * - [PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM][io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM]
     */
    open val capabilities: Bundle = Bundle()

    /**
     * Called once when the user starts a reading session.
     *
     * Use this method to load dictionaries into memory, open database connections,
     * or perform other heavy one-time setup. The [config] Bundle contains user settings
     * and host-provided values (see
     * [PluginHostConfigKeys][io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys]).
     *
     * @param config Bundle containing user and host configuration values.
     * @return An [InitResult] indicating success or failure. Use [InitResultFactory].
     */
    open suspend fun onInitialize(config: Bundle?): InitResult {
        return InitResultFactory.success()
    }

    /**
     * Called each time the user taps a word for dictionary lookup.
     *
     * > **Note:** This is a simplified placeholder API. The return type is a plain
     * > String representing the definition text. Future versions will support
     * > structured dictionary entries (readings, parts of speech, examples, etc.).
     *
     * @param query The word to look up. Depending on the plugin's reported capabilities:
     *   - If [PluginCapabilityKeys.HANDLES_SEGMENTATION] is `false` (default), Dokuen
     *     segments the sentence and passes the isolated word.
     *   - If [PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM] is `true`, the word will
     *     be deinflected to dictionary form before being passed here.
     * @return The definition text for the queried word.
     * @throws DictionaryException if the word is not found or a domain-specific
     *   error occurs.
     */
    abstract suspend fun onLookup(query: String): String

    /**
     * Called when the reading session ends. Release all resources allocated during
     * [onInitialize] (database connections, loaded dictionaries, etc.).
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
                    callback?.onSuccess(result)
                } catch (e: Exception) {
                    callback?.onFailure(e.message ?: "Failed to initialize dictionary plugin")
                }
            }
        }

        override fun lookup(query: String?, callback: IDictionaryCallback?) {
            if (!isCallingAppRegistered()) {
                callback?.onFailure(DictionaryErrorCode.INTERNAL_ERROR, "Calling app not verified")
                return
            }
            if (query.isNullOrEmpty()) {
                callback?.onFailure(DictionaryErrorCode.INVALID_ARGUMENT, "Invalid or empty query")
                return
            }

            serviceScope.launch {
                try {
                    val definition = onLookup(query)
                    callback?.onSuccess(definition)
                } catch (e: DictionaryException) {
                    callback?.onFailure(e.errorCode, e.message ?: "Dictionary lookup failed")
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
