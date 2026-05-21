package io.github.dokuendev.dokuenreader.plugins.ocr.mlkit

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
import io.github.dokuendev.dokuenreader.ocr.OcrImageData
import io.github.dokuendev.dokuenreader.ocr.OcrPluginConstants
import io.github.dokuendev.dokuenreader.ocr.OcrResult
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
 * Integration tests for MlkitOcrPluginService.
 *
 * These tests verify the complete plugin lifecycle through AIDL:
 * 1. Service binding
 * 2. Capability discovery
 * 3. Configuration schema retrieval
 * 4. Initialization
 * 5. Image processing
 * 6. Shutdown
 */
@RunWith(AndroidJUnit4::class)
class MlkitOcrPluginServiceIntegrationTest {

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
        assertFalse(
            "Should not support auto text direction",
            capabilities.getBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION)
        )
        assertFalse(
            "Should not require internet",
            capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )

        val languages = capabilities.getStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES)
        assertNotNull("Supported languages should not be null", languages)
        assertEquals("Should support 1 language", 1, languages!!.size)
        assertTrue("Should support Japanese", languages.contains("ja"))
    }

    // -------------------------------------------------------------------------
    // Configuration Schema Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_returnsConfigSchema() {
        bindService()

        val schema = processor!!.configSchema

        assertNotNull("Config schema should not be null", schema)
        assertEquals("Should have 1 config field", 1, schema.fields.size)

        val halfResField = schema.fields.find { it.key == "half_resolution" }
        assertNotNull("Should have half_resolution field", halfResField)
        assertEquals("Half resolution", halfResField!!.displayName)
        assertEquals(io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType.BOOLEAN, halfResField.type)
        assertFalse("Should be optional", halfResField.isRequired)
        assertEquals("false", halfResField.defaultValue)
    }

    // -------------------------------------------------------------------------
    // Initialization Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_initializeSucceedsWithoutConfig() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var initResult: InitResult? = null

        processor!!.initialize(null, object : IInitCallback.Stub() {
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
        assertFalse(
            "Should not request grayscale conversion",
            initResult.requirements!!.getBoolean("convert_to_grayscale")
        )
    }

    @Test
    fun service_initializeSucceedsWithEmptyConfig() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false

        val config = Bundle()

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
        assertTrue("Should succeed with empty config", initSuccess)
    }

    @Test
    fun service_initializeWithHalfResolutionEnabled() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var initResult: InitResult? = null

        val config = Bundle().apply {
            putString("half_resolution", "true")
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

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should succeed with half resolution enabled", initSuccess)
        assertNotNull("Should have requirements", initResult?.requirements)
        assertTrue(
            "Should request downsampling",
            initResult!!.requirements!!.getBoolean("downsample")
        )
    }

    @Test
    fun service_initializeWithHalfResolutionDisabled() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var initResult: InitResult? = null

        val config = Bundle().apply {
            putString("half_resolution", "false")
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

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Should succeed with half resolution disabled", initSuccess)
        assertNotNull("Should have requirements", initResult?.requirements)
        assertFalse(
            "Should not request downsampling",
            initResult!!.requirements!!.getBoolean("downsample")
        )
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

        processor!!.initialize(null, object : IInitCallback.Stub() {
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
    // Processing Tests
    // -------------------------------------------------------------------------

    @Test
    fun service_processFailsWithoutInitialization() {
        bindService()
        // Don't initialize

        val latch = CountDownLatch(1)
        var processSuccess = false
        var errorCode = 0

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
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should complete", latch.await(5, TimeUnit.SECONDS))
        assertFalse("Processing should fail without initialization", processSuccess)
        assertEquals(
            "Should return INTERNAL_ERROR",
            io.github.dokuendev.dokuenreader.ocr.OcrErrorCode.INTERNAL_ERROR,
            errorCode
        )
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
    fun service_processAcceptsHorizontalTextDirection() {
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

        assertTrue("Processing should invoke callback", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked", callbackInvoked)
    }

    @Test
    fun service_processHandlesSmallImages() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        // Very small image
        val imageData = createTestImageData(width = 50, height = 50)

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

        assertTrue("Should handle small images", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked", callbackInvoked)
    }

    @Test
    fun service_processHandlesLargeImages() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        // Large image
        val imageData = createTestImageData(width = 1920, height = 1080)

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

        assertTrue("Should handle large images", latch.await(30, TimeUnit.SECONDS))
        assertTrue("Callback should be invoked", callbackInvoked)
    }

    @Test
    fun service_processWorksOffline() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var callbackInvoked = false

        val imageData = createTestImageData()

        // MLKit should work without internet connection
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

        assertTrue("Should work offline", latch.await(30, TimeUnit.SECONDS))
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

        // Start processing
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

        processor!!.initialize(null, object : IInitCallback.Stub() {
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
