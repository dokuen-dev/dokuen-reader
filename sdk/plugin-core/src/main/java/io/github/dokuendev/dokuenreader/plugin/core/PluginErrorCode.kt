package io.github.dokuendev.dokuenreader.plugin.core

/**
 * Shared error codes used across all plugin types.
 * Domain-specific plugins may define additional error codes.
 */
object PluginErrorCode {
    const val SUCCESS = 0
    const val NETWORK_ERROR = 1
    const val SERVICE_DISABLED = 2
    const val PERMISSION_DENIED = 3
    const val AUTHENTICATION_ERROR = 4
    const val INVALID_ARGUMENT = 5
    const val UNSUPPORTED = 6
    const val QUOTA_EXCEEDED = 7
    const val CANCELED = 8
    const val TIMEOUT = 9
    const val INTERNAL_ERROR = 10
    const val UNKNOWN_ERROR = 11
}
