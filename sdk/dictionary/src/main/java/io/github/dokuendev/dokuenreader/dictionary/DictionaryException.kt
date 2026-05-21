package io.github.dokuendev.dokuenreader.dictionary

/**
 * Exception thrown by Dictionary plugins to report domain-specific errors.
 *
 * When thrown from [DictionaryPluginService.onLookup] or
 * [DictionaryPluginService.onInitialize], the SDK catches this exception and
 * forwards the [errorCode] and [message] to the host app via
 * [IDictionaryCallback.onFailure].
 *
 * ## Usage Patterns
 *
 * ### 1. Word Not Found
 * ```kotlin
 * override suspend fun onLookup(
 *     contextText: String,
 *     cursorStartIndex: Int,
 *     cursorEndIndex: Int
 * ): DictionaryResult {
 *     val word = contextText.substring(cursorStartIndex, cursorEndIndex)
 *     val entries = database.findEntries(word)
 *     
 *     if (entries.isEmpty()) {
 *         throw DictionaryException(
 *             DictionaryErrorCode.WORD_NOT_FOUND,
 *             "No definition found for \"$word\""
 *         )
 *     }
 *     
 *     return DictionaryResult(entries)
 * }
 * ```
 *
 * ### 2. Network Error
 * ```kotlin
 * try {
 *     val response = apiClient.lookup(word)
 *     return response.toDictionaryResult()
 * } catch (e: IOException) {
 *     throw DictionaryException(
 *         DictionaryErrorCode.NETWORK_ERROR,
 *         "Unable to connect to dictionary server"
 *     )
 * }
 * ```
 *
 * ### 3. Authentication Error
 * ```kotlin
 * if (!apiKey.isValid()) {
 *     throw DictionaryException(
 *         DictionaryErrorCode.AUTHENTICATION_ERROR,
 *         "Invalid API key. Please check your settings."
 *     )
 * }
 * ```
 *
 * ### 4. Invalid Query
 * ```kotlin
 * if (word.length > MAX_QUERY_LENGTH) {
 *     throw DictionaryException(
 *         DictionaryErrorCode.INVALID_QUERY,
 *         "Query exceeds maximum length of $MAX_QUERY_LENGTH characters"
 *     )
 * }
 * ```
 *
 * ## Error Message Guidelines
 *
 * Error messages should be:
 * - **User-friendly**: Avoid technical jargon and stack traces
 * - **Actionable**: Tell users what they can do to fix the problem
 * - **Localized**: Use the `USER_LOCALE` from the config Bundle when possible
 * - **Specific**: Provide enough detail for users to diagnose the issue
 *
 * Good: "Network unavailable. Please check your internet connection."
 * Bad: "java.net.SocketException: Connection refused"
 *
 * ## Error Handling Flow
 *
 * 1. Plugin throws `DictionaryException` from `onLookup()` or `onInitialize()`
 * 2. SDK catches the exception in the AIDL binder stub
 * 3. SDK calls `callback.onFailure(errorCode, message)`
 * 4. Host app receives the error and displays it to the user
 *
 * ## See Also
 *
 * - [DictionaryErrorCode] for available error codes
 * - [DictionaryPluginService.onLookup] for lookup implementation
 * - [IDictionaryCallback.onFailure] for callback error reporting
 *
 * @property errorCode An error code from [DictionaryErrorCode].
 * @param message A user-friendly error message explaining what went wrong.
 */
class DictionaryException(
    val errorCode: Int,
    message: String
) : Exception(message)
