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
     * There is no image, because the user chose to continue without one, the clipboard holds no
     * image, the host could not answer the request (for example because another request is
     * waiting), or the host is too old to answer requests.
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
 * Things a plugin can ask the host for, or tell it, while one of its custom actions runs. A
 * context is passed to [DictionaryPluginService.onExecuteCustomAction] and is valid only until
 * that call returns, after which every request returns the "nothing" answer immediately and
 * progress messages are dropped.
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
     * [ImageRequestSource.SCREENSHOT] only. If it is true, the user can adjust the image (for
     * example crop it or choose a different one), decline, or cancel the action. If it is false,
     * the full screenshot is returned without user interaction. Suspends until the host
     * answers, with no time limit. The image may be smaller than the screen or the original
     * file.
     */
    suspend fun requestImage(source: ImageRequestSource, interactive: Boolean = false): ImageRequestResult

    /**
     * Asks the host for the clipboard's text, as plain text without styling. Text longer than
     * [MAX_CLIPBOARD_TEXT_LENGTH] is cut to that length. Returns null if there is none, the
     * request was refused or cancelled, or the host does not provide clipboard text; not every
     * Dokuen version does, so null is always a possible answer.
     */
    suspend fun requestClipboardText(): String?

    /**
     * Asks the host for the readings of [text]. The host splits [text] into words and gives each
     * word the ruby of its parts that have a reading, in the reading format of the session
     * language (see [io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys.LANGUAGE]
     * and the format table in the SDK documentation). Returns null if the host provides no
     * readings, which happens when [text] is empty or longer than [MAX_READINGS_TEXT_LENGTH], the
     * session language has no reading format, the request was refused or cancelled, or the host is
     * too old. Null is always a possible answer.
     *
     * Words are sorted and do not overlap, every ruby span lies inside its word and has a
     * non-empty reading, and a word's spans are sorted and do not overlap. Offsets are UTF-16
     * indices into [text].
     *
     * @since 0.8.0
     */
    suspend fun requestReadings(text: String): List<WordReading>? = null

    /**
     * Shows [message] to the user while the action runs, in place of the host's default progress
     * text, for example "Adding note..." while an "Add to Anki" action works.
     * Call it once, at the start of the action. It says what the action is doing, not how far
     * along it is, and is not meant for reporting stages or percentages.
     *
     * A later call replaces the previous message, and null (or an empty or blank message)
     * restores the default. The message disappears when the action reports its outcome.
     *
     * The host cuts messages longer than [MAX_PROGRESS_MESSAGE_LENGTH], and may shorten a message
     * further when it does not fit on screen, so keep messages short.
     *
     * Returns immediately without waiting for the host. Does nothing after the action has ended,
     * or on hosts older than 0.9.0, which show their default text instead.
     *
     * @since 0.9.0
     */
    fun setProgressMessage(message: String?) {}

    companion object {
        /**
         * The most UTF-16 code units of clipboard text the host returns. Longer text is cut to
         * this length, one code unit shorter when that would split a surrogate pair.
         *
         * @since 0.7.0
         */
        const val MAX_CLIPBOARD_TEXT_LENGTH = 10_000

        /**
         * The longest text, in UTF-16 code units, a readings request may carry. Longer text is not
         * cut; the request is answered with null.
         *
         * @since 0.8.0
         */
        const val MAX_READINGS_TEXT_LENGTH = 500

        /**
         * The most UTF-16 code units of a progress message the host shows. Longer messages are cut
         * to this length, one code unit shorter when that would split a surrogate pair. A message
         * within this length may still be shortened on screen (see [setProgressMessage]).
         *
         * @since 0.9.0
         */
        const val MAX_PROGRESS_MESSAGE_LENGTH = 100
    }
}
