package io.github.dokuendev.dokuenreader.plugin.core;

import io.github.dokuendev.dokuenreader.plugin.core.InitResult;

/**
 * Callback interface for asynchronous plugin initialization.
 * 
 * This callback is provided to plugins during the initialize() call and must be invoked
 * exactly once to report the initialization result back to the host app.
 * 
 * Usage Pattern:
 * 1. Plugin receives initialize(config, callback)
 * 2. Plugin validates configuration and loads resources
 * 3. Plugin calls EITHER callback.onSuccess() OR callback.onFailure()
 * 4. Plugin must NOT call both methods
 * 5. Plugin must NOT call the callback more than once
 * 
 * Threading:
 * - The callback can be invoked from any thread
 * - The host app handles thread safety internally
 * - Plugins using oneway initialize() receive the call on a Binder background thread
 *   and can invoke the callback synchronously from that same thread
 * 
 * Example (Synchronous on Binder thread):
 * ```
 * override fun initialize(config: Bundle?, callback: IInitCallback?) {
 *     try {
 *         validateConfig(config)
 *         loadHeavyResources()
 *         val requirements = buildRequirements()
 *         callback?.onSuccess(InitResult.success(requirements))
 *     } catch (e: Exception) {
 *         callback?.onFailure("Failed to initialize: ${e.message}")
 *     }
 * }
 * ```
 * 
 * Example (Async with custom threading):
 * ```
 * override fun initialize(config: Bundle?, callback: IInitCallback?) {
 *     executor.execute {
 *         try {
 *             validateConfig(config)
 *             loadHeavyResources()
 *             callback?.onSuccess(InitResult.success())
 *         } catch (e: Exception) {
 *             callback?.onFailure(e.message)
 *         }
 *     }
 * }
 * ```
 */
interface IInitCallback {
    /**
     * Report successful initialization.
     * 
     * Call this method when the plugin has successfully:
     * - Validated the configuration
     * - Loaded all required resources (ML models, dictionaries, network connections)
     * - Is ready to process requests
     * 
     * The InitResult can optionally include domain-specific requirements that tell
     * the host app how to preprocess data before sending it to the plugin.
     * 
     * @param result InitResult containing success status and optional requirements Bundle
     */
    void onSuccess(in InitResult result);
    
    /**
     * Report initialization failure.
     * 
     * Call this method when initialization fails for any reason, including:
     * - Invalid configuration (missing API key, malformed settings)
     * - Resource loading errors (model file not found, network unavailable)
     * - Authentication failures (invalid API credentials)
     * - Any other error that prevents the plugin from functioning
     * 
     * The error message should be:
     * - User-friendly and actionable (e.g., "API key is required" not "NullPointerException")
     * - Localized if possible (optional; fall back to English if not supported)
     * - Specific enough for users to fix the problem
     * 
     * The host app will display this message to the user in the plugin settings UI.
     * 
     * After calling onFailure(), the plugin should remain in an uninitialized state.
     * The host app may call initialize() again if the user updates the configuration.
     * 
     * @param errorMessage Human-readable error message describing why initialization failed.
     *                     Must not be null or empty. Should be localized based on the
     *                     `USER_LOCALE` provided in the `initialize()` config Bundle.
     */
    void onFailure(String errorMessage);
}
