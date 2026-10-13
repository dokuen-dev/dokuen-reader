package io.github.dokuendev.dokuenreader.dictionary

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.IBinder
import io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ActionContextWiringTest {

    /** Requests a screenshot and reports what came back. */
    class RequestingPlugin : DictionaryPluginService() {
        override suspend fun onLookup(contextText: String, cursorStartIndex: Int, cursorEndIndex: Int) =
            DictionaryResult(emptyArray())

        override suspend fun onExecuteCustomAction(
            actionPayload: String,
            context: CustomActionContext
        ): CustomActionResult {
            val result = context.requestImage(ImageRequestSource.SCREENSHOT, interactive = true)
            return CustomActionResult.SuccessMessage(result::class.java.simpleName)
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /** Only overrides the one-argument method, as plugins built before 0.7.0 do. */
    class LegacyPlugin : DictionaryPluginService() {
        override suspend fun onLookup(contextText: String, cursorStartIndex: Int, cursorEndIndex: Int) =
            DictionaryResult(emptyArray())

        override suspend fun onExecuteCustomAction(actionPayload: String): CustomActionResult =
            CustomActionResult.SuccessMessage("legacy:$actionPayload")

        public override fun getBinder(): IBinder = super.getBinder()
    }

    /** Returns [UpdateResult][CustomActionResult.UpdateResult] with the payload as its message. */
    class UpdatingPlugin : DictionaryPluginService() {
        override suspend fun onLookup(contextText: String, cursorStartIndex: Int, cursorEndIndex: Int) =
            DictionaryResult(emptyArray())

        override suspend fun onExecuteCustomAction(actionPayload: String): CustomActionResult =
            CustomActionResult.UpdateResult(UPDATED, actionPayload.ifEmpty { null })

        public override fun getBinder(): IBinder = super.getBinder()
    }

    private class RecordingHost : ICustomActionCallback.Stub() {
        val latch = CountDownLatch(1)
        var outcome: String? = null
        var message: String? = null
        var result: DictionaryResult? = null
        var imageRequests = 0
        var textRequests = 0

        override fun onSuccessMessage(message: String?) {
            outcome = "onSuccessMessage"
            this.message = message
            latch.countDown()
        }

        override fun onUpdateResult(result: DictionaryResult?) {
            outcome = "onUpdateResult"
            this.result = result
            latch.countDown()
        }

        override fun onUpdateResultWithMessage(result: DictionaryResult?, message: String?) {
            outcome = "onUpdateResultWithMessage"
            this.result = result
            this.message = message
            latch.countDown()
        }

        override fun onFailure(errorMessage: String?) = latch.countDown()

        override fun requestImage(options: ImageRequestOptions?, callback: IImageRequestCallback?) {
            imageRequests++
            callback!!.onCancelled()
        }

        override fun requestClipboardText(callback: ITextRequestCallback?) {
            textRequests++
            callback!!.onNoText()
        }
    }

    /** Keeps its context and only uses it after the action has returned. */
    class LateRequestingPlugin : DictionaryPluginService() {
        var context: CustomActionContext? = null

        override suspend fun onLookup(contextText: String, cursorStartIndex: Int, cursorEndIndex: Int) =
            DictionaryResult(emptyArray())

        override suspend fun onExecuteCustomAction(
            actionPayload: String,
            context: CustomActionContext
        ): CustomActionResult {
            this.context = context
            return CustomActionResult.SuccessMessage("done")
        }

        public override fun getBinder(): IBinder = super.getBinder()
    }

    private fun run(service: DictionaryPluginService, hostVersion: String?, payload: String = "payload"): RecordingHost {
        // Debug builds skip the host signature check, which Robolectric's caller would fail.
        service.applicationInfo.flags = service.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        val binder = service.javaClass.getMethod("getBinder").invoke(service) as IDictionaryService
        val config = Bundle().apply { hostVersion?.let { putString(PluginHostConfigKeys.HOST_SDK_VERSION, it) } }
        binder.initialize(config, null)
        val host = RecordingHost()
        binder.executeCustomAction(payload, host)
        assertTrue(host.latch.await(5, TimeUnit.SECONDS))
        return host
    }

    @Test
    fun newHostReceivesTheRequest() {
        val host = run(Robolectric.buildService(RequestingPlugin::class.java).create().get(), "0.7.0")
        assertEquals(1, host.imageRequests)
        assertEquals("Cancelled", host.message)
    }

    @Test
    fun hostWithoutVersionIsNotAsked() {
        val host = run(Robolectric.buildService(RequestingPlugin::class.java).create().get(), null)
        assertEquals(0, host.imageRequests)
        assertEquals("NoImage", host.message)
    }

    @Test
    fun olderHostIsNotAsked() {
        val host = run(Robolectric.buildService(RequestingPlugin::class.java).create().get(), "0.6.9")
        assertEquals(0, host.imageRequests)
        assertEquals("NoImage", host.message)
    }

    @Test
    fun oneArgumentOverridesStillRun() {
        val host = run(Robolectric.buildService(LegacyPlugin::class.java).create().get(), "0.7.0")
        assertEquals("legacy:payload", host.message)
    }

    @Test
    fun contextStopsWorkingWhenTheActionReturns() = runBlocking {
        val plugin = Robolectric.buildService(LateRequestingPlugin::class.java).create().get()
        val host = run(plugin, "0.7.0")

        assertEquals(ImageRequestResult.NoImage, plugin.context!!.requestImage(ImageRequestSource.SCREENSHOT))
        assertNull(plugin.context!!.requestClipboardText())
        assertEquals(0, host.imageRequests)
        assertEquals(0, host.textRequests)
    }

    @Test
    fun updateWithMessageReachesANewHostAsBoth() {
        val host = run(Robolectric.buildService(UpdatingPlugin::class.java).create().get(), "0.7.0", "Added")
        assertEquals("onUpdateResultWithMessage", host.outcome)
        assertEquals("Added", host.message)
        assertEquals("updated", host.result!!.entries.single().headword)
    }

    @Test
    fun updateWithMessageReachesAnOlderHostAsAPlainUpdate() {
        val host = run(Robolectric.buildService(UpdatingPlugin::class.java).create().get(), "0.6.9", "Added")
        assertEquals("onUpdateResult", host.outcome)
        assertNull(host.message)
        assertEquals("updated", host.result!!.entries.single().headword)
    }

    @Test
    fun updateWithoutMessageIsAPlainUpdate() {
        val host = run(Robolectric.buildService(UpdatingPlugin::class.java).create().get(), "0.7.0", "")
        assertEquals("onUpdateResult", host.outcome)
        assertNull(host.message)
        assertEquals("updated", host.result!!.entries.single().headword)
    }

    private companion object {
        val UPDATED = DictionaryResult(arrayOf(DictionaryEntry("updated", null, null, StyledText("body"))))
    }
}
