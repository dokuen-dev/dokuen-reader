package io.github.dokuendev.dokuenreader.ocr

/**
 * Exception thrown by OCR plugins to report domain-specific errors.
 *
 * When thrown from [OcrPluginService.onProcessImage] or [OcrPluginService.onInitialize],
 * the SDK catches this exception and forwards the [errorCode] and [message] to the
 * host app, which displays the message to the user.
 *
 * Use error codes from [OcrErrorCode] to categorize the failure:
 *
 * ```kotlin
 * // Network failure
 * throw OcrException(OcrErrorCode.NETWORK_ERROR, "Could not reach OCR server")
 *
 * // Invalid API key
 * throw OcrException(OcrErrorCode.AUTHENTICATION_ERROR, "Invalid API key. Check Settings.")
 *
 * // Model failed to load
 * throw OcrException(OcrErrorCode.MODEL_UNAVAILABLE, "OCR model not found")
 * ```
 *
 * For unexpected or internal errors, you can let unhandled exceptions propagate
 * naturally. The SDK will catch them and report [OcrErrorCode.INTERNAL_ERROR]
 * automatically.
 *
 * @property errorCode An error code from [OcrErrorCode].
 * @param message A user-friendly, actionable error message. Localize this based on
 *   [PluginHostConfigKeys.USER_LOCALE][io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys.USER_LOCALE]
 *   if possible.
 */
class OcrException(
    val errorCode: Int,
    message: String
) : Exception(message)
