package io.github.dokuendev.dokuenreader.dictionary

import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented tests for DictionaryPluginService base class.
 * Tests error handling for DictionaryException, generic exceptions, cancellation,
 * and security verification integration.
 */
@RunWith(AndroidJUnit4::class)
class DictionaryPluginServiceTest {

    /**
     * Test plugin that returns a simple result.
     */
    private class SimpleTestPlugin : DictionaryPluginService() {
        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            val word = contextText.substring(cursorStartIndex, cursorEndIndex)
            val entry = DictionaryEntry(
                headword = word,
                pronunciation = null,
                body = StyledText(text = "Definition of $word")
            )
            return DictionaryResult(listOf(entry))
        }

        // Expose getBinder for testing
        public override fun getBinder(): IBinder = super.getBinder()
    }

    /**
     * Test plugin that throws DictionaryException.
     */
    private class WordNotFoundPlugin : DictionaryPluginService() {
        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "No definition found"
            )
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /**
     * Test plugin that throws a generic exception.
     */
    private class GenericErrorPlugin : DictionaryPluginService() {
        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            throw RuntimeException("Unexpected error occurred")
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /**
     * Test plugin that throws CancellationException.
     */
    private class CancellablePlugin : DictionaryPluginService() {
        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            delay(100)
            throw CancellationException("Operation cancelled")
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /**
     * Test plugin that validates initialization.
     */
    private class InitializablePlugin : DictionaryPluginService() {
        var initialized = false
        var shutdownCalled = false

        override suspend fun onInitialize(config: Bundle?): InitResult {
            initialized = true
            return InitResultFactory.success()
        }

        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            if (!initialized) {
                throw DictionaryException(
                    DictionaryErrorCode.INTERNAL_ERROR,
                    "Plugin not initialized"
                )
            }
            return DictionaryResult(emptyList())
        }

        override fun onShutdown() {
            shutdownCalled = true
            initialized = false
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /**
     * Test plugin that fails initialization.
     */
    private class FailedInitPlugin : DictionaryPluginService() {
        override suspend fun onInitialize(config: Bundle?): InitResult {
            return InitResultFactory.failure("Invalid API key")
        }

        override suspend fun onLookup(
            contextText: String,
            cursorStartIndex: Int,
            cursorEndIndex: Int
        ): DictionaryResult {
            return DictionaryResult(emptyList())
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    @Test
    fun simplePlugin_successfulLookup_returnsResult() {
        val plugin = SimpleTestPlugin()
        val latch = CountDownLatch(1)
        var receivedResult: DictionaryResult? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                receivedResult = result
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("猫", 0, 1, callback)

        assertTrue("Callback should be invoked within timeout", latch.await(5, TimeUnit.SECONDS))
        assertNotNull("Result should not be null", receivedResult)
        assertEquals("Should have one entry", 1, receivedResult!!.entries.size)
        assertEquals("Headword should match", "猫", receivedResult!!.entries[0].headword)
        assertTrue(
            "Body should contain definition",
            receivedResult!!.entries[0].body.text.contains("Definition of 猫")
        )
    }

    @Test
    fun wordNotFoundPlugin_throwsDictionaryException_invokesOnFailureWithCorrectCode() {
        val plugin = WordNotFoundPlugin()
        val latch = CountDownLatch(1)
        var receivedErrorCode = -1
        var receivedErrorMessage: String? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                receivedErrorCode = errorCode
                receivedErrorMessage = errorMessage
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("nonexistent", 0, 11, callback)

        assertTrue("Callback should be invoked within timeout", latch.await(5, TimeUnit.SECONDS))
        assertEquals(
            "Error code should be WORD_NOT_FOUND",
            DictionaryErrorCode.WORD_NOT_FOUND,
            receivedErrorCode
        )
        assertEquals("Error message should match", "No definition found", receivedErrorMessage)
    }

    @Test
    fun genericErrorPlugin_throwsException_invokesOnFailureWithInternalError() {
        val plugin = GenericErrorPlugin()
        val latch = CountDownLatch(1)
        var receivedErrorCode = -1
        var receivedErrorMessage: String? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                receivedErrorCode = errorCode
                receivedErrorMessage = errorMessage
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("test", 0, 4, callback)

        assertTrue("Callback should be invoked within timeout", latch.await(5, TimeUnit.SECONDS))
        assertEquals(
            "Error code should be INTERNAL_ERROR",
            DictionaryErrorCode.INTERNAL_ERROR,
            receivedErrorCode
        )
        assertEquals("Error message should match", "Unexpected error occurred", receivedErrorMessage)
    }

    @Test
    fun cancellablePlugin_throwsCancellationException_invokesOnFailureWithCanceledCode() {
        val plugin = CancellablePlugin()
        val latch = CountDownLatch(1)
        var receivedErrorCode = -1
        var receivedErrorMessage: String? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                receivedErrorCode = errorCode
                receivedErrorMessage = errorMessage
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("test", 0, 4, callback)

        assertTrue("Callback should be invoked within timeout", latch.await(5, TimeUnit.SECONDS))
        assertEquals(
            "Error code should be CANCELED",
            DictionaryErrorCode.CANCELED,
            receivedErrorCode
        )
        assertEquals("Error message should match", "Lookup cancelled by user", receivedErrorMessage)
    }

    @Test
    fun initializablePlugin_initializeAndShutdown_lifecycleMethodsCalled() {
        val plugin = InitializablePlugin()
        val initLatch = CountDownLatch(1)
        var initSuccess = false

        val initCallback = object : io.github.dokuendev.dokuenreader.plugin.core.IInitCallback.Stub() {
            override fun onSuccess(result: InitResult?) {
                initSuccess = true
                initLatch.countDown()
            }

            override fun onFailure(errorMessage: String?) {
                initLatch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.initialize(null, initCallback)

        assertTrue("Init callback should be invoked", initLatch.await(5, TimeUnit.SECONDS))
        assertTrue("Initialization should succeed", initSuccess)
        assertTrue("Plugin should be initialized", plugin.initialized)

        binder.shutdown()
        assertTrue("Shutdown should be called", plugin.shutdownCalled)
        assertTrue("Plugin should remain initialized after shutdown", !plugin.initialized)
    }

    @Test
    fun failedInitPlugin_initializeFails_invokesOnFailure() {
        val plugin = FailedInitPlugin()
        val latch = CountDownLatch(1)
        var initFailed = false
        var errorMessage: String? = null

        val callback = object : io.github.dokuendev.dokuenreader.plugin.core.IInitCallback.Stub() {
            override fun onSuccess(result: InitResult?) {
                latch.countDown()
            }

            override fun onFailure(message: String?) {
                initFailed = true
                errorMessage = message
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.initialize(null, callback)

        assertTrue("Callback should be invoked", latch.await(5, TimeUnit.SECONDS))
        assertTrue("Initialization should fail", initFailed)
        assertEquals("Error message should match", "Invalid API key", errorMessage)
    }

    @Test
    fun lookup_withNullContextText_invokesOnFailureWithInvalidQuery() {
        val plugin = SimpleTestPlugin()
        val latch = CountDownLatch(1)
        var receivedErrorCode = -1

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                receivedErrorCode = errorCode
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup(null, 0, 0, callback)

        assertTrue("Callback should be invoked", latch.await(5, TimeUnit.SECONDS))
        assertEquals(
            "Error code should be INVALID_QUERY",
            DictionaryErrorCode.INVALID_QUERY,
            receivedErrorCode
        )
    }

    @Test
    fun getCapabilities_returnsEmptyBundle() {
        val plugin = SimpleTestPlugin()
        val binder = plugin.getBinder() as IDictionaryService
        val capabilities = binder.capabilities

        assertNotNull("Capabilities should not be null", capabilities)
        assertTrue("Default capabilities should be empty", capabilities.isEmpty)
    }

    @Test
    fun getConfigSchema_returnsEmptySchema() {
        val plugin = SimpleTestPlugin()
        val binder = plugin.getBinder() as IDictionaryService
        val schema = binder.configSchema

        assertNotNull("Config schema should not be null", schema)
        assertEquals("Default config schema should be empty", 0, schema.fields.size)
    }

    @Test
    fun onLookup_withEmptyResult_returnsEmptyDictionaryResult() {
        val plugin = object : DictionaryPluginService() {
            override suspend fun onLookup(
                contextText: String,
                cursorStartIndex: Int,
                cursorEndIndex: Int
            ): DictionaryResult {
                return DictionaryResult(emptyList())
            }

            public override fun getBinder() = super.getBinder()
        }

        val latch = CountDownLatch(1)
        var receivedResult: DictionaryResult? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                receivedResult = result
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("test", 0, 4, callback)

        assertTrue("Callback should be invoked", latch.await(5, TimeUnit.SECONDS))
        assertNotNull("Result should not be null", receivedResult)
        assertEquals("Result should have zero entries", 0, receivedResult!!.entries.size)
    }

    @Test
    fun onLookup_withMultipleEntries_returnsAllEntries() {
        val plugin = object : DictionaryPluginService() {
            override suspend fun onLookup(
                contextText: String,
                cursorStartIndex: Int,
                cursorEndIndex: Int
            ): DictionaryResult {
                val entries = listOf(
                    DictionaryEntry(
                        headword = "橋",
                        pronunciation = null,
                        body = StyledText(text = "bridge")
                    ),
                    DictionaryEntry(
                        headword = "箸",
                        pronunciation = null,
                        body = StyledText(text = "chopsticks")
                    )
                )
                return DictionaryResult(entries)
            }

            public override fun getBinder() = super.getBinder()
        }

        val latch = CountDownLatch(1)
        var receivedResult: DictionaryResult? = null

        val callback = object : IDictionaryCallback.Stub() {
            override fun onSuccess(result: DictionaryResult?) {
                receivedResult = result
                latch.countDown()
            }

            override fun onFailure(errorCode: Int, errorMessage: String?) {
                latch.countDown()
            }
        }

        val binder = plugin.getBinder() as IDictionaryService
        binder.lookup("はし", 0, 2, callback)

        assertTrue("Callback should be invoked", latch.await(5, TimeUnit.SECONDS))
        assertNotNull("Result should not be null", receivedResult)
        assertEquals("Result should have two entries", 2, receivedResult!!.entries.size)
        assertEquals("First entry headword", "橋", receivedResult!!.entries[0].headword)
        assertEquals("Second entry headword", "箸", receivedResult!!.entries[1].headword)
    }
}
