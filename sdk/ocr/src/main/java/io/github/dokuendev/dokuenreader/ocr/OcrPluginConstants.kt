package io.github.dokuendev.dokuenreader.ocr

/**
 * Constants for OCR plugin integration.
 */
object OcrPluginConstants {
    /**
     * Intent action that OCR plugins must declare in their service intent filter.
     * 
     * Example AndroidManifest.xml:
     * ```xml
     * <service android:name=".MyOcrService" android:exported="true">
     *     <intent-filter>
     *         <action android:name="io.github.dokuendev.dokuenreader.ocr.BIND_OCR_SERVICE" />
     *     </intent-filter>
     * </service>
     * ```
     */
    const val BIND_OCR_SERVICE_ACTION = "io.github.dokuendev.dokuenreader.ocr.BIND_OCR_SERVICE"

    /**
     * Text direction constants.
     */
    const val TEXT_DIRECTION_HORIZONTAL = "horizontal"
    const val TEXT_DIRECTION_VERTICAL = "vertical"
    const val TEXT_DIRECTION_AUTO = "auto"
}
