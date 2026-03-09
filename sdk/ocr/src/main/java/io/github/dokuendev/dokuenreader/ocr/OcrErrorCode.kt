package io.github.dokuendev.dokuenreader.ocr

import io.github.dokuendev.dokuenreader.plugin.core.PluginErrorCode

/**
 * OCR-specific error codes extending the base PluginErrorCode.
 */
object OcrErrorCode {
    const val SUCCESS = PluginErrorCode.SUCCESS
    const val NETWORK_ERROR = PluginErrorCode.NETWORK_ERROR
    const val SERVICE_DISABLED = PluginErrorCode.SERVICE_DISABLED
    const val PERMISSION_DENIED = PluginErrorCode.PERMISSION_DENIED
    const val AUTHENTICATION_ERROR = PluginErrorCode.AUTHENTICATION_ERROR
    const val INVALID_ARGUMENT = PluginErrorCode.INVALID_ARGUMENT
    const val UNSUPPORTED = PluginErrorCode.UNSUPPORTED
    const val QUOTA_EXCEEDED = PluginErrorCode.QUOTA_EXCEEDED
    const val CANCELED = PluginErrorCode.CANCELED
    const val TIMEOUT = PluginErrorCode.TIMEOUT
    const val INTERNAL_ERROR = PluginErrorCode.INTERNAL_ERROR
    const val UNKNOWN_ERROR = PluginErrorCode.UNKNOWN_ERROR

    // OCR-specific error codes start at 100
    const val RESULT_PARSING_ERROR = 100
    const val MODEL_UNAVAILABLE = 101
}
