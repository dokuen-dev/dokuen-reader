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
    /**
     * Returns a Bundle of key-value pairs describing plugin capabilities.
     * Use keys from PluginCapabilityKeys or define custom ones.
     */
    Bundle getCapabilities();
    PluginConfigSchema getConfigSchema();
    oneway void initialize(in Bundle config, IInitCallback callback);
    
    // Simplistic placeholder looking up a word and invoking a callback with its string definition.
    oneway void lookup(String query, IDictionaryCallback callback);
    
    void shutdown();
}
