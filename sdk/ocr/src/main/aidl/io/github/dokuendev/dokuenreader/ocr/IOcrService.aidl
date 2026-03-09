package io.github.dokuendev.dokuenreader.ocr;

import android.os.Bundle;
import io.github.dokuendev.dokuenreader.ocr.IOcrCallback;
import io.github.dokuendev.dokuenreader.ocr.OcrImageData;
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema;
import io.github.dokuendev.dokuenreader.plugin.core.InitResult;
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback;

/**
 * OCR Plugin Interface
 * 
 * Plugin Metadata:
 * Plugin information (name, version, author, description, license, website, support email)
 * must be declared in AndroidManifest.xml using meta-data tags. This allows Dokuen to
 * discover plugin information without binding to the service.
 * 
 * Lifecycle:
 * 1. Settings Menu (Discovery & Configuration):
 *    - Query PackageManager for services with intent filter
 *    - Read metadata from AndroidManifest
 *    - bind() -> getCapabilities() -> getConfigSchema() -> unbind()
 *    - User enters config, app saves it locally
 * 
 * 2. Reading Session (Initialization & Execution):
 *    - bind() -> initialize() -> process() [multiple times] -> shutdown() -> unbind()
 */
interface IOcrService {
    // --- Discovery Phase (Settings Menu) ---
    
    /**
     * Returns a Bundle of key-value pairs describing plugin capabilities.
     * Use keys from PluginCapabilityKeys or define custom ones.
     * 
     * Example:
     *   bundle.putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
     *   bundle.putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, ["ja", "en"])
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
     * This is called when the user starts an OCR session (presses the "Start" button).
     * 
     * The plugin should:
     * 1. Validate the configuration
     * 2. If valid, load heavy resources (ML models, network connections, buffers)
     * 3. Return InitResult with success=true and optional requirements
     * 4. If invalid, return InitResult with success=false and error message
     * 
     * Use OcrRequirementKeys to specify preprocessing requirements:
     * - OcrRequirementKeys.MAX_WIDTH (Int)
     * - OcrRequirementKeys.MAX_HEIGHT (Int)
     * - OcrRequirementKeys.DOWNSAMPLE (Boolean)
     * - OcrRequirementKeys.CONVERT_TO_GRAYSCALE (Boolean)
     * 
     * @param config A Bundle containing all fields populated by the user according to
     *               getConfigSchema(), and Host keys as defined in PluginHostConfigKeys.
     * @param callback Callback to receive the initialization result.
     */
    oneway void initialize(in Bundle config, IInitCallback callback);
    
    // --- Execution Phase (During Reading Session) ---
    
    /**
     * Process an image and extract text.
     * 
     * Called when the user taps the capture button.
     * The app will have applied preprocessing based on requirements from initialize().
     * 
     * @param imageData The image to process.
     *                  FORMAT: Always RGBA_8888 (4 bytes per pixel).
     *                  SIZE: SharedMemory size is always width * height * 4 bytes.
     *                  LIFECYCLE: The plugin MUST call imageData.sharedMemory.close() when
     *                  finished to release resources. The plugin MAY retain the SharedMemory
     *                  beyond this call for asynchronous processing if needed.
     *                  
     *                  Use imageData.width and height for dimensions.
     * @param textDirection "horizontal", "vertical", or "auto" (if supported)
     * @param callback Callback to receive the result or error
     */
    oneway void process(in OcrImageData imageData, String textDirection, IOcrCallback callback);

    /**
     * Notifies the plugin that the user has cancelled the current OCR request.
     * 
     * When the user cancels an OCR request, the host immediately cancels the coroutine
     * that `process` was called from and will ignore any subsequent `IOcrCallback` invocations
     * from that call. This notification allows the plugin to make a best effort to stop
     * doing wasted work, if possible.
     * 
     * Whether and when cancellation is feasible depends on the underlying OCR engine.
     * For example, a GPU-based engine may only be able to check between pipeline stages,
     * while a web API can stop listening for responses but cannot cancel in-flight requests.
     *
     * After cancel() is called:
     * - Plugins MAY skip calling IOcrCallback (host will ignore it anyway)
     * - Plugins MUST remain ready for new process() calls
     * - Plugins MUST NOT change initialization state
     * - Multiple cancel() calls MUST be idempotent
     * - cancel() MUST be thread-safe with process()
     *
     * See README for implementation patterns.
     */
    oneway void cancel();

    // --- Cleanup Phase (Reading Session End) ---
    
    /**
     * Release all resources allocated during initialize().
     * 
     * Called when the user taps the Stop button or closes the app.
     * - Unload ML models from memory
     * - Close network connections
     * - Free processing buffers
     */
    void shutdown();
}
