package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult;
import io.github.dokuendev.dokuenreader.dictionary.IImageRequestCallback;
import io.github.dokuendev.dokuenreader.dictionary.IReadingsRequestCallback;
import io.github.dokuendev.dokuenreader.dictionary.ITextRequestCallback;
import io.github.dokuendev.dokuenreader.dictionary.ImageRequestOptions;

/**
 * Callback interface for custom action execution on dictionary plugins.
 *
 * Besides reporting the action's outcome, the plugin can ask the host for things while the
 * action runs (requestImage, requestClipboardText, requestReadings) and tell the user what it is
 * doing (onProgressMessage). Answers to requests have no time limit; an interactive request takes
 * as long as the user needs.
 */
interface ICustomActionCallback {
    oneway void onSuccessMessage(String message);
    oneway void onUpdateResult(in DictionaryResult result);
    oneway void onFailure(String errorMessage);

    /**
     * Reports success, like onUpdateResult, and also shows [message] in the host UI, like
     * onSuccessMessage.
     *
     * @since 0.7.0
     */
    oneway void onUpdateResultWithMessage(in DictionaryResult result, String message);

    /**
     * Asks the host for an image. Valid only while this action has not reported its outcome,
     * and only one request of any kind may be outstanding at a time, otherwise the host
     * answers onNoImage().
     *
     * @since 0.7.0
     */
    oneway void requestImage(in ImageRequestOptions options, IImageRequestCallback callback);

    /**
     * Asks the host for the clipboard's text. Same validity rules as requestImage, except that
     * the host answers an invalid request with onNoText().
     *
     * @since 0.7.0
     */
    oneway void requestClipboardText(ITextRequestCallback callback);

    /**
     * Asks the host for the readings of [text]. Same validity rules as requestClipboardText,
     * except that the host answers an invalid request with onNoReadings(). Text longer than
     * CustomActionContext.MAX_READINGS_TEXT_LENGTH (500) UTF-16 code units is refused.
     *
     * @since 0.8.0
     */
    oneway void requestReadings(String text, IReadingsRequestCallback callback);

    /**
     * Shows [message] in the host UI while the action runs, in place of the host's default
     * progress text. Each call replaces the previous message, and null (or an empty or blank
     * message) restores the default. Valid only while this action has not reported its outcome;
     * the host ignores it afterwards. The host cuts messages longer than
     * CustomActionContext.MAX_PROGRESS_MESSAGE_LENGTH (100) UTF-16 code units, and may shorten a
     * message further when it does not fit in the host UI.
     *
     * @since 0.9.0
     */
    oneway void onProgressMessage(String message);
}
