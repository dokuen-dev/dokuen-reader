package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.WordReading;

/**
 * The host's answer to one readings request. Exactly one method is called, once.
 *
 * @since 0.8.0
 */
interface IReadingsRequestCallback {
    /** The words of the requested text, with the ruby of each word. */
    oneway void onReadings(in WordReading[] words);

    /**
     * Called when the host gives no readings, because the text is empty or too long, the session
     * language has no reading format, the request was refused, or the host does not provide
     * readings.
     */
    oneway void onNoReadings();
}
