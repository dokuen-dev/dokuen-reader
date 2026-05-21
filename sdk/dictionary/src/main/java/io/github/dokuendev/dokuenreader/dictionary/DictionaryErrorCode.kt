package io.github.dokuendev.dokuenreader.dictionary

import io.github.dokuendev.dokuenreader.plugin.core.PluginErrorCode

/**
 * Error codes for Dictionary plugins.
 * 
 * This object extends [PluginErrorCode] with dictionary-specific error codes.
 * Dictionary plugins should use these codes when reporting errors via
 * [DictionaryException] or [IDictionaryCallback.onFailure].
 * 
 * ## Standard Error Codes (from PluginErrorCode)
 * 
 * - [SUCCESS] (0): Operation completed successfully
 * - [NETWORK_ERROR] (1): Network connectivity issue
 * - [SERVICE_DISABLED] (2): Service is disabled or unavailable
 * - [PERMISSION_DENIED] (3): Required permission not granted
 * - [AUTHENTICATION_ERROR] (4): Authentication failed (e.g., invalid API key)
 * - [INVALID_ARGUMENT] (5): Invalid input parameter
 * - [UNSUPPORTED] (6): Operation not supported
 * - [QUOTA_EXCEEDED] (7): Rate limit or quota exceeded
 * - [CANCELED] (8): Operation was canceled
 * - [TIMEOUT] (9): Operation timed out
 * - [INTERNAL_ERROR] (10): Internal plugin error
 * - [UNKNOWN_ERROR] (11): Unknown error
 * 
 * ## Dictionary-Specific Error Codes (200+)
 * 
 * - [WORD_NOT_FOUND] (200): No dictionary entry found for the query
 * - [INVALID_QUERY] (201): Query format is invalid or unsupported
 * 
 * ## Usage Example
 * 
 * ```kotlin
 * // Throw a DictionaryException with a specific error code
 * throw DictionaryException(
 *     DictionaryErrorCode.WORD_NOT_FOUND,
 *     "No definition found for \"猫\""
 * )
 * 
 * // Or report via callback
 * callback.onFailure(
 *     DictionaryErrorCode.NETWORK_ERROR,
 *     "Unable to connect to dictionary server"
 * )
 * ```
 */
object DictionaryErrorCode {
    // Standard error codes from PluginErrorCode
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

    // Dictionary-specific error codes (200+)

    /**
     * No dictionary entry found for the queried word.
     * 
     * Use this code when the lookup completes successfully but no matching
     * entries exist in the dictionary. This is a normal condition, not an error.
     */
    const val WORD_NOT_FOUND = 200

    /**
     * The query format is invalid or unsupported.
     * 
     * Use this code when the query text is malformed, contains unsupported
     * characters, or violates plugin-specific constraints.
     */
    const val INVALID_QUERY = 201
}
