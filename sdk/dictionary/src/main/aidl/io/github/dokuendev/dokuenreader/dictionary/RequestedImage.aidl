package io.github.dokuendev.dokuenreader.dictionary;

import android.os.SharedMemory;

/**
 * An image the host returns for a plugin's image request.
 *
 * The pixels are RGBA_8888 with premultiplied alpha (the in-memory layout of an Android
 * ARGB_8888 Bitmap), row by row with no padding; sharedMemory is exactly
 * width * height * 4 bytes and read-only. The receiver owns its copy of the SharedMemory
 * and must close() it when done.
 *
 * The host may scale images down, so do not assume the image has the size of the screen or
 * of the original file.
 *
 * @since 0.7.0
 */
parcelable RequestedImage {
    SharedMemory sharedMemory;
    int width;
    int height;
}
