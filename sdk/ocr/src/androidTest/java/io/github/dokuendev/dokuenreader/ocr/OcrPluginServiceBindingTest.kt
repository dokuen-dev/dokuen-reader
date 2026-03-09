package io.github.dokuendev.dokuenreader.ocr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.SharedMemory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dokuendev.dokuenreader.plugin.core.IInitCallback
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for OCR plugin service binding and AIDL communication.
 * 
 * These tests verify the complete plugin lifecycle:
 * 1. Service binding
 * 2. Capability discovery
 * 3. Configuration schema retrieval
 * 4. Initialization
 * 5. OCR processing
 * 6. Cancellation
 * 7. Shutdown
 */
@RunWith(AndroidJUnit4::class)
class OcrPluginServiceBindingTest {

    private lateinit var context: Context
    private var processor: IOcrService? = null
    private var serviceConnection: ServiceConnection? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        serviceConnection?.let { context.unbindService(it) }
        processor = null
        serviceConnection = null
    }

    @Test
    fun service_canBind() {
        val latch = CountDownLatch(1)
        var bound = false

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                processor = IOcrService.Stub.asInterface(service)
                bound = true
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                processor = null
            }
        }

        serviceConnection = connection

        val intent = Intent(context, MockOcrPluginService::class.java).apply {
            setPackage(context.packageName)
        }
        val bindResult = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        assertTrue("bindService should return true", bindResult)
        assertTrue("Service should bind within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Service should be bound", bound)
        assertNotNull("Processor should not be null", processor)
    }

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
            "Should not require internet",
            capabilities.getBoolean(PluginCapabilityKeys.REQUIRES_INTERNET)
        )

        val languages = capabilities.getStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES)
        assertNotNull("Supported languages should not be null", languages)
        assertTrue("Should support Japanese", languages!!.contains("ja"))
    }

    @Test
    fun service_returnsConfigSchema() {
        bindService()

        val schema = processor!!.configSchema

        assertNotNull("Config schema should not be null", schema)
        assertTrue("Should have at least one field", schema.fields.isNotEmpty())
        assertEquals("test_key", schema.fields[0].key)
    }

    @Test
    fun service_canInitialize() {
        bindService()

        val latch = CountDownLatch(1)
        var initSuccess = false
        var initResult: InitResult? = null

        val config = Bundle().apply {
            putString("test_key", "test_value")
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
    }

    @Test
    fun service_canProcessImage() {
        bindService()
        initializeService()

        val latch = CountDownLatch(1)
        var processSuccess = false
        var ocrResult: OcrResult? = null

        // Create mock image data (RGBA_8888 format)
        val sharedMemory = SharedMemory.create("test_image", 640 * 480 * 4)
        val imageData = OcrImageData().apply {
            this.sharedMemory = sharedMemory
            width = 640
            height = 480
        }

        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_HORIZONTAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {
                    processSuccess = true
                    ocrResult = result
                    latch.countDown()
                }

                override fun onFailure(errorCode: Int, errorMessage: String?) {
                    latch.countDown()
                }
            }
        )

        assertTrue("Processing should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Processing should succeed", processSuccess)
        assertNotNull("OCR result should not be null", ocrResult)
        assertTrue("Should have at least one block", ocrResult!!.ocrBlocks.isNotEmpty())
    }

    @Test
    fun service_canCancel() {
        bindService()
        initializeService()

        // Start processing
        val imageData = OcrImageData().apply {
            width = 640
            height = 480
        }

        processor!!.process(
            imageData,
            OcrPluginConstants.TEXT_DIRECTION_HORIZONTAL,
            object : IOcrCallback.Stub() {
                override fun onSuccess(result: OcrResult) {}
                override fun onFailure(errorCode: Int, errorMessage: String?) {}
            }
        )

        // Cancel immediately
        processor!!.cancel()

        // Should not throw exception
        assertTrue("Cancel should complete without error", true)
    }

    @Test
    fun service_canShutdown() {
        bindService()
        initializeService()

        processor!!.shutdown()

        // Should not throw exception
        assertTrue("Shutdown should complete without error", true)
    }

    private fun bindService() {
        val latch = CountDownLatch(1)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                processor = IOcrService.Stub.asInterface(service)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                processor = null
            }
        }

        serviceConnection = connection

        val intent = Intent(context, MockOcrPluginService::class.java).apply {
            setPackage(context.packageName)
        }
        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        assertTrue("bindService should return true", bound)
        assertTrue("Service should bind", latch.await(5, TimeUnit.SECONDS))
        assertNotNull("Processor should be bound", processor)
    }

    private fun initializeService() {
        val latch = CountDownLatch(1)

        processor!!.initialize(Bundle(), object : IInitCallback.Stub() {
            override fun onSuccess(result: InitResult) {
                latch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                latch.countDown()
            }
        })

        assertTrue("Initialization should complete", latch.await(5, TimeUnit.SECONDS))
    }
}
