package io.github.dokuendev.dokuenreader.ocr

import android.app.Service
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.os.IBinder
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import io.github.dokuendev.dokuenreader.plugin.core.PluginConfigSchema

/**
 * Mock OCR plugin service for integration testing.
 * 
 * This service implements the IOcrService interface and can be used to test
 * the full AIDL communication flow without requiring signature verification.
 */
class MockOcrPluginService : Service() {

    private var isInitialized = false
    private var isCancelled = false

    private val binder = object : IOcrService.Stub() {

        override fun getCapabilities(): Bundle {
            return Bundle().apply {
                putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
                putBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION, false)
                putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
                putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja", "en"))
            }
        }

        override fun getConfigSchema(): PluginConfigSchema {
            return PluginConfigSchema(
                fields = listOf(
                    ConfigField(
                        key = "test_key",
                        displayName = "Test Key",
                        description = "Test configuration field",
                        type = ConfigFieldType.STRING,
                        defaultValue = "default",
                        isRequired = false
                    )
                )
            )
        }

        override fun initialize(config: Bundle?, callback: IInitCallback?) {
            try {
                // Simulate initialization
                Thread.sleep(100)

                val requirements = Bundle().apply {
                    putInt(OcrRequirementKeys.MAX_WIDTH, 2048)
                    putInt(OcrRequirementKeys.MAX_HEIGHT, 2048)
                    putBoolean(OcrRequirementKeys.DOWNSAMPLE, false)
                }

                isInitialized = true
                callback?.onSuccess(InitResultFactory.success(requirements))
            } catch (e: Exception) {
                callback?.onFailure("Initialization failed: ${e.message}")
            }
        }

        override fun process(
            imageData: OcrImageData?,
            textDirection: String?,
            callback: IOcrCallback?
        ) {
            if (!isInitialized) {
                callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, "Not initialized")
                return
            }

            if (imageData == null) {
                callback?.onFailure(OcrErrorCode.INVALID_ARGUMENT, "Image data is null")
                return
            }

            isCancelled = false

            try {
                // Simulate OCR processing
                Thread.sleep(200)

                if (isCancelled) {
                    callback?.onFailure(OcrErrorCode.CANCELED, "Canceled")
                    return
                }

                // Return mock OCR result
                val blocks = listOf(
                    OcrBlock(
                        text = "テスト",
                        symbolBounds = listOf(
                            RectF(0f, 0f, 20f, 20f),
                            RectF(20f, 0f, 40f, 20f),
                            RectF(40f, 0f, 60f, 20f)
                        ),
                        isVertical = textDirection == OcrPluginConstants.TEXT_DIRECTION_VERTICAL
                    )
                )

                val result = OcrResult(blocks)
                callback?.onSuccess(result)
            } catch (e: Exception) {
                callback?.onFailure(OcrErrorCode.INTERNAL_ERROR, e.message)
            }
        }

        override fun cancel() {
            isCancelled = true
        }

        override fun shutdown() {
            isInitialized = false
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
