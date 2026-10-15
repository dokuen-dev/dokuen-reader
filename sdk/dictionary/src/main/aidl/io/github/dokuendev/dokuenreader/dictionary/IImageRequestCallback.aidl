package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.RequestedImage;

/**
 * The host's answer to one image request. Exactly one method is called, once.
 *
 * @since 0.7.0
 */
interface IImageRequestCallback {
    /** The image. */
    oneway void onImage(in RequestedImage image);

    /**
     * Called when there is no image, because the user chose to continue without one, the
     * clipboard holds no image, or the host could not answer the request (for example because
     * another request is waiting).
     */
    oneway void onNoImage();

    /** The user cancelled the whole action. The plugin should stop without side effects. */
    oneway void onCancelled();

    /**
     * The host denied the request, for example because the user lacks the necessary entitlement
     * for this request, the user has not allowed this plugin to receive images, or a security
     * check failed.
     */
    oneway void onDenied();
}
