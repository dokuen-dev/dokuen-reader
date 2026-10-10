package io.github.dokuendev.dokuenreader.dictionary

/**
 * Where a requested image comes from. [code] is the value sent in [ImageRequestOptions.source].
 *
 * @since 0.7.0
 */
enum class ImageRequestSource(val code: Int) {
    /** The screenshot that was OCR'ed for the current lookup. */
    SCREENSHOT(0),

    /** The image on the clipboard, if any. */
    CLIPBOARD(1);

    companion object {
        fun fromCode(code: Int): ImageRequestSource? = entries.firstOrNull { it.code == code }
    }
}

/**
 * The host's answer to [CustomActionContext.requestImage].
 *
 * @since 0.7.0
 */
sealed class ImageRequestResult {
    /** The image, ARGB_8888. The plugin owns it and should recycle it when done. */
    data class Image(val bitmap: android.graphics.Bitmap) : ImageRequestResult()

    /**
     * No image: the user chose to continue without one, the clipboard holds no image, the host
     * could not answer the request (for example because another request is waiting), or the
     * host is too old to answer requests.
     */
    data object NoImage : ImageRequestResult()

    /**
     * The user cancelled the whole action. Stop without side effects. The host has already
     * ended the action, so whatever the action returns afterwards is not shown.
     */
    data object Cancelled : ImageRequestResult()

    /**
     * The host denied the request, for example because the user lacks the necessary entitlement
     * for this request, the user has not allowed this plugin to receive images, or a security
     * check failed.
     */
    data object Denied : ImageRequestResult()
}

/**
 * Things a plugin can ask the host for while one of its custom actions runs. Passed to
 * [DictionaryPluginService.onExecuteCustomAction]; valid only until that call returns, after
 * which every request returns the "nothing" answer immediately.
 *
 * Requests are answered one at a time. Calling a method while another request of the same
 * action is still waiting returns the "nothing" answer immediately. A request whose caller was
 * cancelled (for example by a timeout) keeps waiting until the host answers it.
 *
 * @since 0.7.0
 */
interface CustomActionContext {
    /**
     * Asks the host for an image from [source]. [interactive] applies to
     * [ImageRequestSource.SCREENSHOT] only: true lets the user adjust the image (for example
     * crop it or choose a different one), decline, or cancel the action; false returns the
     * full screenshot without user interaction. Suspends until the host answers, with no time
     * limit. The image may be smaller than the screen or the original file.
     */
    suspend fun requestImage(source: ImageRequestSource, interactive: Boolean = false): ImageRequestResult

    /**
     * Asks the host for the clipboard's text, as plain text without styling. Text longer than
     * [MAX_CLIPBOARD_TEXT_LENGTH] is cut to that length. Returns null if there is none, the
     * request was refused or cancelled, or the host does not provide clipboard text; not every
     * Dokuen version does, so null is always a possible answer.
     */
    suspend fun requestClipboardText(): String?

    companion object {
        /**
         * The most UTF-16 code units of clipboard text the host returns. Longer text is cut to
         * this length, one code unit shorter when that would split a surrogate pair.
         *
         * @since 0.7.0
         */
        const val MAX_CLIPBOARD_TEXT_LENGTH = 10_000
    }
}
