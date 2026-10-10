package io.github.dokuendev.dokuenreader.dictionary

import android.graphics.Color
import android.os.IBinder
import android.os.RemoteException
import android.os.SharedMemory
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HostActionContextTest {

    /**
     * A host that records requests and answers them with the configured replies. It also keeps
     * the death recipient linked to it, so a test can make it "die".
     */
    private class FakeHost : ICustomActionCallback.Stub() {
        val imageRequests = mutableListOf<Pair<Int, Boolean>>()
        var textRequests = 0
        var imageReply: (IImageRequestCallback) -> Unit = { it.onNoImage() }
        var textReply: (ITextRequestCallback) -> Unit = { it.onNoText() }
        var deathRecipient: IBinder.DeathRecipient? = null

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            deathRecipient = recipient
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
            if (deathRecipient === recipient) deathRecipient = null
            return true
        }

        fun die() = deathRecipient!!.binderDied()

        override fun onSuccessMessage(message: String?) {}
        override fun onUpdateResult(result: DictionaryResult?) {}
        override fun onFailure(errorMessage: String?) {}

        override fun requestImage(options: ImageRequestOptions?, callback: IImageRequestCallback?) {
            imageRequests += options!!.source to options.interactive
            imageReply(callback!!)
        }

        override fun requestClipboardText(callback: ITextRequestCallback?) {
            textRequests++
            textReply(callback!!)
        }
    }

    /** An RGBA_8888 image of [width] x [height] whose every pixel is R=0x11 G=0x22 B=0x33 A=0xFF. */
    private fun requestedImage(width: Int, height: Int, size: Int = width * height * 4): RequestedImage {
        val memory = SharedMemory.create("test", size)
        val buffer = memory.mapReadWrite()
        while (buffer.remaining() >= 4) {
            buffer.put(0x11); buffer.put(0x22); buffer.put(0x33); buffer.put(0xFF.toByte())
        }
        SharedMemory.unmap(buffer)
        return RequestedImage().apply { sharedMemory = memory; this.width = width; this.height = height }
    }

    /**
     * Whether this platform's ARGB_8888 bitmaps keep their pixels in memory as R, G, B, A, as
     * Android does. Robolectric's native Bitmap on some hosts (Windows) stores B, G, R, A instead.
     */
    private fun platformStoresRgba(): Boolean {
        val probe = createBitmap(1, 1).apply { setPixel(0, 0, Color.argb(0xFF, 0x11, 0x22, 0x33)) }
        val bytes = ByteBuffer.allocate(4)
        probe.copyPixelsToBuffer(bytes)
        probe.recycle()
        return bytes.get(0) == 0x11.toByte()
    }

    @Test
    fun oldHostGetsNoRequestAndNoImage() = runBlocking {
        val host = FakeHost()
        val context = HostActionContext(host, hostSupportsRequests = false)

        assertEquals(ImageRequestResult.NoImage, context.requestImage(ImageRequestSource.SCREENSHOT, true))
        assertNull(context.requestClipboardText())
        assertTrue(host.imageRequests.isEmpty())
        assertEquals(0, host.textRequests)
    }

    @Test
    fun forwardsSourceAndInteractiveFlag() = runBlocking {
        val host = FakeHost()
        val context = HostActionContext(host, hostSupportsRequests = true)

        context.requestImage(ImageRequestSource.SCREENSHOT, interactive = true)
        context.requestImage(ImageRequestSource.CLIPBOARD)

        assertEquals(listOf(0 to true, 1 to false), host.imageRequests)
    }

    @Test
    fun imageReplyBecomesBitmap() = runBlocking {
        val host = FakeHost().apply { imageReply = { it.onImage(requestedImage(3, 2)) } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val result = context.requestImage(ImageRequestSource.SCREENSHOT)

        val bitmap = (result as ImageRequestResult.Image).bitmap
        assertEquals(3, bitmap.width)
        assertEquals(2, bitmap.height)
        val pixel = bitmap.getPixel(2, 1)
        assertEquals(0xFF, Color.alpha(pixel))
        assertEquals(0x22, Color.green(pixel))
    }

    @Test
    fun imageKeepsItsChannelOrder() = runBlocking {
        assumeTrue("This platform's Bitmap does not store RGBA", platformStoresRgba())
        val host = FakeHost().apply { imageReply = { it.onImage(requestedImage(3, 2)) } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val bitmap = (context.requestImage(ImageRequestSource.SCREENSHOT) as ImageRequestResult.Image).bitmap

        assertEquals(Color.argb(0xFF, 0x11, 0x22, 0x33), bitmap.getPixel(2, 1))
    }

    @Test
    fun malformedImageBecomesNoImage() = runBlocking {
        val host = FakeHost().apply { imageReply = { it.onImage(requestedImage(3, 2, size = 7)) } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        assertEquals(ImageRequestResult.NoImage, context.requestImage(ImageRequestSource.SCREENSHOT))
    }

    @Test
    fun cancelledReplyIsPassedThrough() = runBlocking {
        val host = FakeHost().apply { imageReply = { it.onCancelled() } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        assertEquals(ImageRequestResult.Cancelled, context.requestImage(ImageRequestSource.SCREENSHOT, true))
    }

    @Test
    fun deniedReplyIsPassedThrough() = runBlocking {
        val host = FakeHost().apply { imageReply = { it.onDenied() } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        assertEquals(ImageRequestResult.Denied, context.requestImage(ImageRequestSource.CLIPBOARD))
    }

    @Test
    fun secondConcurrentRequestGetsNoImageWithoutAsking() = runBlocking {
        var pending: IImageRequestCallback? = null
        val host = FakeHost().apply { imageReply = { pending = it } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val first = async { context.requestImage(ImageRequestSource.SCREENSHOT, true) }
        while (pending == null) yield()
        val second = context.requestImage(ImageRequestSource.CLIPBOARD)
        pending.onNoImage()

        assertEquals(ImageRequestResult.NoImage, second)
        assertEquals(ImageRequestResult.NoImage, first.await())
        assertEquals(1, host.imageRequests.size)
    }

    @Test
    fun clipboardTextReplies() = runBlocking {
        val host = FakeHost().apply { textReply = { it.onText("hello") } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        assertEquals("hello", context.requestClipboardText())
        host.textReply = { it.onNoText() }
        assertNull(context.requestClipboardText())
    }

    @Test
    fun closedContextAsksNothing() = runBlocking {
        val host = FakeHost()
        val context = HostActionContext(host, hostSupportsRequests = true)
        context.close()

        assertEquals(ImageRequestResult.NoImage, context.requestImage(ImageRequestSource.SCREENSHOT))
        assertNull(context.requestClipboardText())
        assertTrue(host.imageRequests.isEmpty())
        assertEquals(0, host.textRequests)
    }

    @Test
    fun cancelledRequestBlocksOthersUntilTheHostAnswers() = runBlocking {
        var pending: IImageRequestCallback? = null
        val host = FakeHost().apply { imageReply = { pending = it } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val first = async { context.requestImage(ImageRequestSource.SCREENSHOT, true) }
        while (pending == null) yield()
        first.cancelAndJoin()

        assertNull(context.requestClipboardText())
        assertEquals(0, host.textRequests)

        pending.onImage(requestedImage(3, 2))
        host.textReply = { it.onText("after") }
        assertEquals("after", context.requestClipboardText())
    }

    @Test
    fun hostDyingDuringImageRequestCancelsIt() = runBlocking {
        val host = FakeHost().apply { imageReply = {} }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val first = async { context.requestImage(ImageRequestSource.SCREENSHOT, true) }
        while (host.deathRecipient == null) yield()
        host.die()

        assertEquals(ImageRequestResult.Cancelled, first.await())
        assertNull(host.deathRecipient)
    }

    @Test
    fun hostDyingDuringTextRequestAnswersNull() = runBlocking {
        val host = FakeHost().apply { textReply = {} }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val first = async { context.requestClipboardText() }
        while (host.deathRecipient == null) yield()
        host.die()

        assertNull(first.await())
        host.textReply = { it.onText("after") }
        assertEquals("after", context.requestClipboardText())
    }

    @Test
    fun failedSendIsCancelledAndFreesTheSlot() = runBlocking {
        val host = FakeHost().apply { imageReply = { throw RemoteException() } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        assertEquals(ImageRequestResult.Cancelled, context.requestImage(ImageRequestSource.SCREENSHOT))
        assertNull(host.deathRecipient)
        host.imageReply = { it.onDenied() }
        assertEquals(ImageRequestResult.Denied, context.requestImage(ImageRequestSource.SCREENSHOT))
    }

    @Test
    fun unexpectedSendFailureIsRethrownAndFreesTheSlot() = runBlocking {
        val host = FakeHost().apply { imageReply = { throw IllegalStateException("boom") } }
        val context = HostActionContext(host, hostSupportsRequests = true)

        val failure = runCatching { context.requestImage(ImageRequestSource.SCREENSHOT) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertNull(host.deathRecipient)
        host.imageReply = { it.onDenied() }
        assertEquals(ImageRequestResult.Denied, context.requestImage(ImageRequestSource.SCREENSHOT))
    }

    @Test
    fun answerArrivingAfterCancellationIsNotBuilt() = runBlocking {
        val context = HostActionContext(FakeHost(), hostSupportsRequests = true)
        var answer: ((() -> String) -> Boolean)? = null

        val waiting = async { context.request(nothing = "nothing", onHostGone = "gone", discard = {}) { answer = it } }
        while (answer == null) yield()
        waiting.cancelAndJoin()
        var built = false

        assertFalse(answer!!.invoke { built = true; "late" })
        assertFalse(built)
    }

    @Test
    fun answerDeliveredAsTheCallerIsCancelledIsDiscarded() = runBlocking {
        val context = HostActionContext(FakeHost(), hostSupportsRequests = true)
        val discarded = mutableListOf<String>()
        var answer: ((() -> String) -> Boolean)? = null

        val waiting = async {
            context.request(nothing = "nothing", onHostGone = "gone", discard = { discarded += it }) { answer = it }
        }
        while (answer == null) yield()
        // The answer is handed over, but the cancellation lands before the caller resumes.
        assertTrue(answer!!.invoke { "value" })
        waiting.cancel()
        waiting.join()

        assertEquals(listOf("value"), discarded)
    }
}
