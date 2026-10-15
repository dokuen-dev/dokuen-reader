package io.github.dokuendev.dokuenreader.dictionary;

/**
 * The host's answer to one clipboard text request. Exactly one method is called, once.
 *
 * @since 0.7.0
 */
interface ITextRequestCallback {
    /**
     * The clipboard's text, as plain text. The host cuts longer text to
     * CustomActionContext.MAX_CLIPBOARD_TEXT_LENGTH (10,000) UTF-16 code units.
     */
    oneway void onText(String text);

    /**
     * Called when there is no text, because the clipboard has none, the request was refused, or
     * the host does not provide it.
     */
    oneway void onNoText();
}
