package io.github.dokuendev.dokuenreader.dictionary

import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The [CustomActionContext] of one custom action, which sends requests to the host over the
 * action's [callback]. When the host is too old to answer them ([hostSupportsRequests] false),
 * or after [close] has been called, nothing is sent and every request gets its "nothing" answer.
 */
internal class HostActionContext(
    private val callback: ICustomActionCallback,
    private val hostSupportsRequests: Boolean
) : CustomActionContext {

    // Set while the host may be holding a request, which is either from sending it until the host
    // answers it or dies, or until sending fails. It stays set even if the caller stops waiting
    // earlier, so the SDK never has more requests outstanding than the host allows.
    private val requestInFlight = AtomicBoolean(false)

    private val closed = AtomicBoolean(false)

    /** Ends the action. Requests made after this get "nothing" without asking the host. */
    fun close() {
        closed.set(true)
    }

    override suspend fun requestImage(source: ImageRequestSource, interactive: Boolean): ImageRequestResult {
        val options = ImageRequestOptions().apply {
            this.source = source.code
            this.interactive = interactive
        }
        return request(
            nothing = ImageRequestResult.NoImage,
            onHostGone = ImageRequestResult.Cancelled,
            discard = { (it as? ImageRequestResult.Image)?.bitmap?.recycle() }
        ) { answer ->
            callback.requestImage(options, object : IImageRequestCallback.Stub() {
                override fun onImage(image: RequestedImage?) {
                    val used = answer {
                        image?.let { RequestedImages.toBitmap(it) }?.let { ImageRequestResult.Image(it) }
                            ?: ImageRequestResult.NoImage
                    }
                    if (!used) image?.sharedMemory?.close()
                }

                override fun onNoImage() {
                    answer { ImageRequestResult.NoImage }
                }

                override fun onCancelled() {
                    answer { ImageRequestResult.Cancelled }
                }

                override fun onDenied() {
                    answer { ImageRequestResult.Denied }
                }
            })
        }
    }

    override suspend fun requestClipboardText(): String? =
        request(nothing = null, onHostGone = null, discard = {}) { answer ->
            callback.requestClipboardText(object : ITextRequestCallback.Stub() {
                override fun onText(text: String?) {
                    answer { text }
                }

                override fun onNoText() {
                    answer { null }
                }
            })
        }

    /**
     * Sends one request with [send] and suspends until the host answers it through `answer`.
     * Returns [nothing] without sending when requests are not possible: the host is too old,
     * the action has ended, or another request is outstanding. The host dying, or the request
     * failing to send, resolves to [onHostGone]. Any other exception from [send] means the
     * request was never sent (it failed while being written), so the request ends without
     * waiting for the host, and the exception is rethrown to the caller.
     *
     * `answer` builds the value with its argument only when a caller is still waiting for it,
     * and returns whether it did, so the binder callback can release what it would otherwise
     * have converted. A value that is built but cannot be delivered because the caller was
     * cancelled at that moment is passed to [discard].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @VisibleForTesting
    internal suspend fun <T> request(
        nothing: T,
        onHostGone: T,
        discard: (T) -> Unit,
        send: (answer: (() -> T) -> Boolean) -> Unit
    ): T {
        if (!hostSupportsRequests || closed.get() || !requestInFlight.compareAndSet(false, true)) return nothing
        return suspendCancellableCoroutine { continuation ->
            val answered = AtomicBoolean(false)
            val binder = callback.asBinder()
            lateinit var deathRecipient: IBinder.DeathRecipient
            // Ends the request on the SDK side. Returns false if it had already ended.
            val finish: () -> Boolean = {
                answered.compareAndSet(false, true).also { first ->
                    if (first) {
                        runCatching { binder.unlinkToDeath(deathRecipient, 0) }
                        requestInFlight.set(false)
                    }
                }
            }
            val answer: (() -> T) -> Boolean = answer@{ produce ->
                if (!finish() || !continuation.isActive) return@answer false
                val value = produce()
                continuation.resume(value) { discard(value) }
                true
            }
            deathRecipient = IBinder.DeathRecipient { answer { onHostGone } }
            try {
                binder.linkToDeath(deathRecipient, 0)
                send(answer)
            } catch (_: RemoteException) {
                answer { onHostGone }
            } catch (t: Throwable) {
                // Requests are oneway, so the host's own exceptions never come back here. This
                // one was thrown before the transaction, so the host has nothing to answer.
                finish()
                throw t
            }
        }
    }
}
