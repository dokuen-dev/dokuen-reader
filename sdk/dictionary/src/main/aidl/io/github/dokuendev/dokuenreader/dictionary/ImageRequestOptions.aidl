package io.github.dokuendev.dokuenreader.dictionary;

/**
 * What a plugin asks for when it requests an image from the host during a custom action.
 *
 * @since 0.7.0
 */
parcelable ImageRequestOptions {
    /**
     * Where the image comes from. 0 for the screenshot that was OCR'ed or 1 for the
     * clipboard. See ImageRequestSource. A host answers an unknown value with onNoImage().
     */
    int source;

    /**
     * Applies to the screenshot only. If true, the host lets the user adjust the image before it
     * is returned (for example by cropping it or choosing a different image), and the user may
     * also decline to provide one or cancel the whole action. If false, the host returns the full
     * screenshot without user interaction. Ignored for the clipboard, which never involves user
     * interaction.
     */
    boolean interactive;
}
