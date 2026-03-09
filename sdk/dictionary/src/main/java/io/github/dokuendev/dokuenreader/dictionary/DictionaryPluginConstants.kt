package io.github.dokuendev.dokuenreader.dictionary

/**
 * Constants for Dictionary plugin integration.
 */
object DictionaryPluginConstants {
    /**
     * Intent action that Dictionary plugins must declare in their service intent filter.
     * 
     * Example AndroidManifest.xml:
     * ```xml
     * <service android:name=".MyDictionaryService" android:exported="true">
     *     <intent-filter>
     *         <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
     *     </intent-filter>
     * </service>
     * ```
     */
    const val BIND_DICTIONARY_SERVICE_ACTION = "io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE"
}
