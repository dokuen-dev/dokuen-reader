package io.github.dokuendev.dokuenreader.ocr

import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.os.SharedMemory
import androidx.core.graphics.createBitmap
import io.github.dokuendev.dokuenreader.plugin.core.BasePluginService
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Base class for building Dokuen OCR plugins.
 *
 * Extend this class and implement [onProcessImage] to create a plugin that performs
 * text recognition for the Dokuen Reader application. The SDK handles all service
 * lifecycle, security verification, coroutine management, and error reporting
 * automatically.
 *
 * ## Minimal Example
 *
 * ```kotlin
 * class MyOcrPlugin : OcrPluginService() {
 *
 *     override suspend fun onProcessImage(
 *         bitmap: Bitmap,
 *         textDirection: String?
 *     ): List<OcrBlock> {
 *         val inputImage = InputImage.fromBitmap(bitmap, 0)
 *         val visionText = recognizer.process(inputImage).await()
 *         return visionText.textBlocks.map { block ->
 *             OcrBlock(
 *                 text = block.text,
 *                 symbolBounds = block.boundingBoxes,
 *                 isVertical = false
 *             )
 *         }
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
 * When the user starts an OCR reading session:
 *
 * 1. **[onInitialize]** is called once with the user's configuration. Load heavy
 *    resources here (ML models, network clients, buffers). Return preprocessing
 *    requirements if needed (see [OcrRequirementKeys]).
 * 2. **[onProcessImage]** is called each time the user captures an image. The SDK
 *    provides a standard [Bitmap]. Pass it to your OCR engine and return the
 *    recognized [OcrBlock] list.
 * 3. **[onShutdown]** is called when the session ends. Release all resources
 *    allocated during [onInitialize].
 *
 * ## Cancellation
 *
 * If the user cancels an in-progress OCR request, the SDK cancels the coroutine
 * running [onProcessImage]. A [CancellationException] is thrown at the next
 * suspension point, and the SDK catches it internally -- you do not need to catch
 * it yourself.
 *
 * However, there are several things to be aware of:
 *
 * **State consistency:** After cancellation, [onProcessImage] may be called again
 * with a new image. Your plugin must remain in a usable state. If you update
 * mutable state during processing (e.g., an intermediate result buffer), make sure
 * it is reset or otherwise safe when the coroutine is cancelled partway through.
 *
 * **Local resource cleanup:** If you allocate resources within [onProcessImage]
 * (e.g., temporary files, native handles), wrap them in `try/finally` or use
 * Kotlin's `.use {}` to ensure they are released on cancellation:
 *
 * ```kotlin
 * override suspend fun onProcessImage(
 *     bitmap: Bitmap,
 *     textDirection: String?
 * ): List<OcrBlock> {
 *     val tempFile = createTempFile()
 *     try {
 *         // ... process ...
 *         return results
 *     } finally {
 *         tempFile.delete()
 *     }
 * }
 * ```
 *
 * **CPU-bound work:** Cancellation is *cooperative* in Kotlin coroutines. If your
 * `onProcessImage` does long-running CPU work without calling any suspending
 * function, the cancellation cannot take effect until the next suspension point.
 * For CPU-intensive loops, periodically call `ensureActive()` to check for
 * cancellation:
 *
 * ```kotlin
 * for (tile in tiles) {
 *     ensureActive() // Throws CancellationException if cancelled
 *     processTile(tile)
 * }
 * ```
 *
 * **Do not catch [CancellationException]:** The SDK relies on it for cleanup.
 * If you have a broad `catch (e: Exception)` block, re-throw if the exception is
 * a `CancellationException`.
 *
 * ## Error Handling
 *
 * If [onProcessImage] or [onInitialize] throw an exception, the SDK catches it and
 * reports it to the host app with an appropriate error code. If you want to communicate
 * a specific error (e.g., network failure, invalid API key), throw an [OcrException]:
 *
 * ```kotlin
 * throw OcrException(OcrErrorCode.NETWORK_ERROR, "Could not reach OCR server")
 * ```
 *
 * Unhandled exceptions are reported as [OcrErrorCode.INTERNAL_ERROR].
 *
 * Error messages should be user-friendly and actionable (e.g., "Check your internet
 * connection" rather than "SocketTimeoutException"). Localize messages based on
 * [PluginHostConfigKeys.USER_LOCALE][io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys.USER_LOCALE]
 * if possible.
 *
 * ## AndroidManifest.xml
 *
 * Register your service with the required intent filter and metadata:
 *
 * ```xml
 * <service android:name=".MyOcrPlugin" android:exported="true">
 *     <intent-filter>
 *         <action android:name="io.github.dokuendev.dokuenreader.ocr.BIND_OCR_SERVICE" />
 *     </intent-filter>
 *
 *     <!-- Required metadata. Plugins missing any of these will not be recognized. -->
 *     <meta-data android:name="plugin_name" android:value="My OCR Plugin" />
 *     <meta-data android:name="plugin_version" android:value="1.0.0" />
 *     <meta-data android:name="plugin_author" android:value="Your Name" />
 *     <meta-data android:name="plugin_description" android:value="High-accuracy OCR" />
 *     <meta-data android:name="plugin_license" android:value="Apache 2.0" />
 *
 *     <!-- Optional but recommended -->
 *     <meta-data android:name="plugin_website" android:value="https://example.com" />
 *     <meta-data android:name="plugin_support_email" android:value="support@example.com" />
 * </service>
 * ```
 *
 * @see OcrBlock The data structure returned for each recognized text region.
 * @see OcrErrorCode Error codes for reporting failures.
 * @see OcrRequirementKeys Keys for requesting image preprocessing from the host app.
 * @see PluginCapabilityKeys Standard capability keys for [capabilities].
 */
abstract class OcrPluginService : BasePluginService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentJob: Job? = null

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
     * with `_` are reserved for host-provided values (see
     * [PluginHostConfigKeys][io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys]).
     *
     * Example:
     * ```kotlin
     * override val configSchema = listOf(
     *     ConfigField(
     *         key = "api_key",
     *         displayName = "API Key",
     *         description = "Required for cloud OCR processing",
     *         type = ConfigFieldType.STRING,
     *         defaultValue = null,
     *         isRequired = true
     *     )
     * )
     * ```
     */
    open val configSchema: List<ConfigField> = emptyList()

    /**
     * The class name of an Activity in this plugin's APK that the host
     * should launch for configuration.
     *
     * When set, the host ignores [configSchema] for UI building and instead
     * opens this Activity when the user taps "Configure". The Activity runs
     * in the plugin's own process and has full access to the plugin's storage
     * and UI toolkit.
     *
     * Use a leading dot (e.g. `".MyConfigActivity"`) for a class relative to
     * the plugin's package, or a full class name (e.g.
     * `"com.example.plugin.MyConfigActivity"`).
     *
     * **Coupled with [isConfigured]:** Plugins that set this property **must**
     * also override [isConfigured] to report whether the plugin has been
     * fully configured. The host cannot determine readiness from
     * [configSchema] for plugins that manage their own configuration.
     *
     * Defaults to `null`, meaning the host builds the config UI from
     * [configSchema].
     */
    open val configActivityName: String? = null

    /**
     * Reports whether this plugin considers itself fully configured and
     * ready to be activated.
     *
     * **This method is only meaningful when [configActivityName] is set.**
     * Plugins that host their own config Activity manage their own
     * configuration storage, so the host relies on this method to determine
     * whether the plugin may be selected as active.
     *
     * For schema-based plugins ([configActivityName] is null), the host
     * uses the [configSchema] `isRequired` flags instead and ignores this
     * method.
     *
     * The default implementation returns `true`. Override this when using
     * [configActivityName] to perform your own validation (e.g., checking
     * that an API key file exists on disk).
     */
    open fun isConfigured(): Boolean = true

    /**
     * A [Bundle] of key-value pairs describing this plugin's capabilities.
     *
     * Override this property to report what your plugin supports. Dokuen reads these
     * capabilities to adjust its UI (e.g., enabling/disabling the vertical text
     * direction option) and to determine compatibility with the reader.
     *
     * Use the standard keys from
     * [PluginCapabilityKeys][io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys]
     * and add any custom keys as needed.
     *
     * Example:
     * ```kotlin
     * override val capabilities = Bundle().apply {
     *     putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
     *     putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
     *     putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja"))
     * }
     * ```
     *
     * @see PluginCapabilityKeys for the complete list of standard keys and their
     *   meanings.
     */
    open val capabilities: Bundle = Bundle()

    /**
     * Called once when the user starts a reading session.
     *
     * Use this method to perform heavy one-time setup such as loading ML models,
     * establishing network connections, or allocating processing buffers. This method
     * runs in a coroutine, so you can call suspending functions directly.
     *
     * The [config] Bundle contains two categories of keys:
     *
     * 1. **User configuration values**: All settings the user entered through the
     *    plugin settings UI, keyed by the [ConfigField.key] strings from [configSchema].
     * 2. **Host configuration values**: Standard keys provided by Dokuen, as defined in
     *    [PluginHostConfigKeys][io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys]
     *    (e.g., `_language` for the active reading language, `_user_locale` for the
     *    user's preferred UI locale).
     *
     * You may optionally return preprocessing requirements by passing a Bundle of
     * [OcrRequirementKeys] to [InitResultFactory.success]. The host app will apply
     * the requested preprocessing (e.g., downsampling, grayscale conversion) to every
     * image before delivering it to [onProcessImage].
     *
     * If initialization fails (e.g., invalid API key, model not found), return
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
     * Called each time the user captures an image for text recognition.
     *
     * The SDK provides a standard Android [Bitmap] in ARGB_8888 format. Pass this
     * bitmap directly to your OCR engine and return the recognized text as a list of
     * [OcrBlock] objects.
     *
     * **Bitmap lifecycle:** The SDK manages the bitmap's lifecycle. Do not call
     * [Bitmap.recycle]. The SDK handles cleanup after this method returns (or is
     * cancelled).
     *
     * **Image preprocessing:** If you returned preprocessing requirements from
     * [onInitialize] (via [OcrRequirementKeys]), the host app will have already applied
     * them (e.g., downsampling, grayscale conversion) before this method is called.
     *
     * **Return value:** Return a list of [OcrBlock] objects, one per recognized text
     * region. Each block must include:
     * - [OcrBlock.text]: The recognized text string.
     * - [OcrBlock.symbolBounds]: An array of [android.graphics.RectF] bounding boxes,
     *   one per character in [OcrBlock.text]. Coordinates are in the bitmap's pixel
     *   space.
     * - [OcrBlock.isVertical]: `true` if the text in this block runs vertically
     *   (top-to-bottom, right-to-left), `false` for horizontal text.
     *
     * If no text is detected, return an empty list.
     *
     * **Cancellation:** This method runs inside a coroutine. If the user cancels the
     * request, the coroutine is cancelled and a [CancellationException] is thrown at
     * the next suspension point. The SDK catches this automatically -- do not catch it.
     * However, you are responsible for ensuring your plugin's internal state remains
     * consistent after cancellation, and that any locally allocated resources are
     * cleaned up via `try/finally`. See the class-level Cancellation section for
     * details and examples.
     *
     * @param bitmap The captured image to process, in ARGB_8888 format.
     * @param textDirection A hint for the expected text orientation:
     *   - `TEXT_DIRECTION_HORIZONTAL`: The user expects horizontal (left-to-right) text.
     *   - `TEXT_DIRECTION_VERTICAL`: The user expects vertical (top-to-bottom) Japanese text.
     *   - `TEXT_DIRECTION_AUTO`: The plugin should detect the orientation per block.
     *   This value is determined by the user's settings and is only `TEXT_DIRECTION_VERTICAL`
     *   or `TEXT_DIRECTION_AUTO` if your plugin reported the corresponding capability in
     *   [PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT] /
     *   [PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION].
     * @return A list of recognized [OcrBlock] objects, or an empty list if no text
     *   was detected.
     * @throws OcrException if a domain-specific error occurs (e.g., network failure).
     *   The error code and message will be forwarded to the user.
     * @throws CancellationException if the user cancels the request. Do not catch this.
     */
    abstract suspend fun onProcessImage(
        bitmap: Bitmap,
        textDirection: String?
    ): List<OcrBlock>

    /**
     * Called when the reading session ends.
     *
     * Release all resources allocated during [onInitialize]: unload ML models, close
     * network connections, free processing buffers, etc.
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

    private val binder = object : IOcrService.Stub() {
        override fun getCapabilities(): Bundle = this@OcrPluginService.capabilities

        override fun getConfigSchema(): PluginConfigSchema {
            return PluginConfigSchema(this@OcrPluginService.configSchema)
        }

        override fun getConfigActivityName(): String? {
            return this@OcrPluginService.configActivityName
        }

        override fun isConfigured(): Boolean {
            return this@OcrPluginService.isConfigured()
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
                    callback?.onFailure(e.message ?: "Failed to initialize plugin")
                }
            }
        }

        override fun process(imageData: OcrImageData?, textDirection: String?, callback: IOcrCallback?) {
            if (!isCallingAppRegistered()) {
                callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, "Calling app not verified")
                return
            }
            currentJob?.cancel()

            if (imageData == null || imageData.sharedMemory == null) {
                callback?.onFailure(OcrErrorCode.INVALID_ARGUMENT, "Missing image data")
                return
            }

            currentJob = serviceScope.launch {
                var bitmap: Bitmap? = null
                try {
                    bitmap = extractBitmapFromSharedMemory(imageData.sharedMemory, imageData.width, imageData.height)

                    val blocks = onProcessImage(bitmap, textDirection)

                    val result = OcrResult().apply { ocrBlocks = blocks.toTypedArray() }
                    callback?.onSuccess(result)

                } catch (e: CancellationException) {
                    callback?.onFailure(OcrErrorCode.CANCELED, "OCR processing cancelled by user")
                } catch (e: OcrException) {
                    callback?.onFailure(e.errorCode, e.message ?: "OCR processing failed")
                } catch (e: Exception) {
                    callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, e.message ?: "Unknown error")
                } finally {
                    bitmap?.recycle()
                    imageData.sharedMemory?.close()
                }
            }
        }

        override fun cancel() {
            if (!isCallingAppRegistered()) return
            currentJob?.cancel()
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

    private fun extractBitmapFromSharedMemory(sharedMemory: SharedMemory, width: Int, height: Int): Bitmap {
        val buffer = sharedMemory.mapReadOnly()
        return try {
            val bitmap = createBitmap(width, height)
            bitmap.copyPixelsFromBuffer(buffer)
            bitmap
        } finally {
            SharedMemory.unmap(buffer)
        }
    }
}
