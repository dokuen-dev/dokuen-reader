package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.SharedMemory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import io.github.dokuendev.dokuenreader.ocr.IOcrCallback
import io.github.dokuendev.dokuenreader.ocr.IOcrService
import io.github.dokuendev.dokuenreader.ocr.OcrErrorCode
import io.github.dokuendev.dokuenreader.ocr.OcrImageData
import io.github.dokuendev.dokuenreader.ocr.OcrPluginConstants
import io.github.dokuendev.dokuenreader.ocr.OcrResult
import io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for GcpOcrPluginService.
 *
 * These tests verify the complete plugin lifecycle through AIDL:
 * 1. Service binding
 * 2. Capability discovery
 * 3. Configuration schema retrieval
 * 4. Initialization with various configs
 * 5. Shutdown
 *
 * Note: These tests do NOT make actual network calls to GCP.
 * They test the service binding and configuration logic only.
 */
@RunWith(AndroidJUnit4::class)
class GcpOcrPluginServiceIntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var processor: IOcrService? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // -------------------------------------------------------------------------
    // Service Binding Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_canBind() {
        bindService()
        assertNotNull("Processor should not be null", processor)
    }

    @Test
    fun service_canUnbind() {
        bindService()
        // ServiceTestRule unbinds automatically - just verify no exception was thrown
        assertTrue("Unbind should complete without error", true)
    }

    // -------------------------------------------------------------------------
    // Capabilities Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_returnsCapabilities() {
        bindService()

        val capabilities = processor!!.capabilities

        assertNotNull("Capabilities should not be null", capabilities)
        assertTrue(
            "Should support vertical text",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT)
        )
        assertTrue(
            "Should support auto text direction",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION)
        )
        assertTrue(
            "Should require internet",
            capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )
        assertTrue(
            "Should have custom config",
            capabilities.getBoolean(PluginCapabilityKeys.HAS_CUSTOM_CONFIG)
        )

        val languages = capabilities.getStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES)
        assertNotNull("Supported languages should not be null", languages)
        assertEquals("Should support 2 languages", 2, languages!!.size)
        assertTrue("Should support Japanese", languages.contains("ja"))
        assertTrue("Should support Chinese", languages.contains("zh"))
    }

    // -------------------------------------------------------------------------
    // Configuration Schema Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_returnsConfigSchema() {
        bindService()

        val schema = processor!!.configSchema

        assertNotNull("Config schema should not be null", schema)
        assertEquals("Should have 2 config fields", 2, schema.fields.size)

        val apiKeyField = schema.fields.find { it.key == "api_key" }
        assertNotNull("Should have api_key field", apiKeyField)
        assertEquals("API key should be STRING type", ConfigFieldType.STRING, apiKeyField!!.type)
        assertTrue("API key should be required", apiKeyField.isRequired)
        assertEquals("API Key", apiKeyField.displayName)

        val detectionModeField = schema.fields.find { it.key == "detection_mode" }
        assertNotNull("Should have detection_mode field", detectionModeField)
        assertEquals("Detection mode should be ENUM type", ConfigFieldType.ENUM, detectionModeField!!.type)
        assertFalse("Detection mode should be optional", detectionModeField.isRequired)
        assertNotNull("Should have enum values", detectionModeField.enumValues)
        assertEquals("Should have 2 enum values", 2, detectionModeField.enumValues!!.size)
        assertTrue("Should include TEXT_DETECTION", detectionModeField.enumValues!!.contains("TEXT_DETECTION"))
        assertTrue(
            "Should include DOCUMENT_TEXT_DETECTION",
            detectionModeField.enumValues!!.contains("DOCUMENT_TEXT_DETECTION")
        )
        assertEquals(
            "Default should be DOCUMENT_TEXT_DETECTION",
            "DOCUMENT_TEXT_DETECTION",
            detectionModeField.defaultValue
        )
    }

    // -------------------------------------------------------------------------
    // Initialization Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_initializeFailsWithoutApiKey() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var errorMessage: String? = null

        val config = Bundle() // No API key

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                latch.countDown()
            }

            override fun onFailure(message: String?) {
                errorMessage = message
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertFalse("Initialization should fail", initSuccess)
        assertNotNull("Should have error message", errorMessage)
        assertTrue("Error should mention API key", errorMessage!!.contains("API key", ignoreCase = true))
    }

    @Test
    fun service_initializeSucceedsWithApiKey() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var initResult: InitResult? = null

        val config = Bundle().apply {
            putString("api_key", "test-api-key-12345")
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                initResult = result
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Initialization should succeed", initSuccess)
        assertNotNull("Init result should not be null", initResult)
        assertTrue("Init result should indicate success", initResult!!.success)
        assertNotNull("Should have requirements", initResult.requirements)
        assertTrue(
            "Should request grayscale conversion",
            initResult.requirements!!.getBoolean("convert_to_grayscale")
        )
    }

    @Test
    fun service_initializeWithTextDetectionMode() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false

        val config = Bundle().apply {
            putString("api_key", "test-api-key")
            putString("detection_mode", "TEXT_DETECTION")
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should succeed with TEXT_DETECTION mode", initSuccess)
    }

    @Test
    fun service_initializeWithDocumentTextDetectionMode() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false

        val config = Bundle().apply {
            putString("api_key", "test-api-key")
            putString("detection_mode", "DOCUMENT_TEXT_DETECTION")
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should succeed with DOCUMENT_TEXT_DETECTION mode", initSuccess)
    }

    @Test
    fun service_initializeWithLanguageHint() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false

        val config = Bundle().apply {
            putString("api_key", "test-api-key")
            putString("_language", "zh") // Host-provided language
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should succeed with language hint", initSuccess)
    }

    // -------------------------------------------------------------------------
    // Shutdown Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_canShutdown() {
        bindService()
        initializeService()

        processor!!.shutdown()

        // Should not throw exception
        assertTrue("Shutdown should complete without error", true)
    }

    @Test
    fun service_canReinitializeAfterShutdown() {
        bindService()
        initializeService()

        processor!!.shutdown()

        // Reinitialize
        val latch = CountDownLatch(1)
        var initSuccess = false

        val config = Bundle().apply {
            putString("api_key", "test-api-key-new")
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                initSuccess = true
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Reinitialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should be able to reinitialize after shutdown", initSuccess)
    }

    // -------------------------------------------------------------------------
    // Processing Tests (without real API calls)
    // -------------------------------------------------------------------------

    @Test
    fun service_processFailsWithoutInitialization() {
        bindService()
        // Don't initialize

        val latch = CountDownLatch(1)
        var processSuccess = false
        var errorCode = 0
        var errorMessage: String? = null

        val imageData = createTestImageData()

        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_HORIZONTAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {
                    processSuccess = true
                    latch.countDown()
                }

                override fun onFailure(code: Int, message: String?) {
                    errorCode = code
                    errorMessage = message
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertFalse("Processing should fail without initialization", processSuccess)
        assertEquals(OcrErrorCode.AUTHENTICATION_ERROR, errorCode)
        assertNotNull("Should have error message", errorMessage)
        assertTrue("Error should mention API key", errorMessage!!.contains("API key", ignoreCase = true))
    }

    @Test
    fun service_processAcceptsValidImageData() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        val imageData = createTestImageData()

        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_HORIZONTAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {
                    callbackInvoked = true
                    latch.countDown()
                }

                override fun onFailure(code: Int, message: String?) {
                    callbackInvoked = true
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should invoke callback within 30 seconds", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked (success or failure)", callbackInvoked)
        // Note: This will likely fail with a network/auth error since we don't have a real API key,
        // but it verifies the service accepts the request and processes it
    }

    @Test
    fun service_processAcceptsVerticalTextDirection() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        val imageData = createTestImageData()

        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_VERTICAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {
                    callbackInvoked = true
                    latch.countDown()
                }

                override fun onFailure(code: Int, message: String?) {
                    callbackInvoked = true
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should invoke callback", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked", callbackInvoked)
    }

    @Test
    fun service_processAcceptsAutoTextDirection() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        val imageData = createTestImageData()

        processor!!.process(
            imageData,
            "auto",
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {
                    callbackInvoked = true
                    latch.countDown()
                }

                override fun onFailure(code: Int, message: String?) {
                    callbackInvoked = true
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should invoke callback", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked", callbackInvoked)
    }

    // -------------------------------------------------------------------------
    // Cancel Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_canCancel() {
        bindService()
        initializeService()

        // Cancel should not throw even if no operation is in progress
        processor!!.cancel()

        assertTrue("Cancel should complete without error", true)
    }

    @Test
    fun service_canCancelDuringProcessing() {
        bindService()
        initializeService()

        val imageData = createTestImageData()

        // Start processing (will likely fail with network error, but that's ok)
        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_HORIZONTAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {}
                override fun onFailure(code: Int, message: String?) {}
            }
        )

        // Cancel immediately
        processor!!.cancel()

        // Should not throw exception
        assertTrue("Cancel during processing should complete without error", true)
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private fun bindService() {
        val intent = Intent("io.github.dokuendev.dokuenreader.ocr.BIND_OCR_SERVICE").apply {
            setPackage(context.packageName)
        }
        val binder: IBinder = serviceRule.bindService(intent)
        processor = IOcrService.Stub.asInterface(binder)
        assertNotNull("Processor should be bound", processor)
    }

    private fun initializeService() {
        val latch = CountDownLatch(1)

        val config = Bundle().apply {
            putString("api_key", "test-api-key")
        }

        processor!!.initialize(config, object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
    }

    private fun createTestImageData(width: Int = 640, height: Int = 480): OcrImageData {
        val sharedMemory = SharedMemory.create("test_image", width * height * 4)
        val buffer = sharedMemory.mapReadWrite()

        // Fill with test pattern (simple gradient)
        val byteBuffer = ByteBuffer.allocate(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val gray = ((x + y) % 256).toByte()
                byteBuffer.put(gray) // R
                byteBuffer.put(gray) // G
                byteBuffer.put(gray) // B
                byteBuffer.put(255.toByte()) // A
            }
        }
        buffer.put(byteBuffer.array())
        SharedMemory.unmap(buffer)

        return OcrImageData().apply {
            this.sharedMemory = sharedMemory
            this.width = width
            this.height = height
        }
    }
}
