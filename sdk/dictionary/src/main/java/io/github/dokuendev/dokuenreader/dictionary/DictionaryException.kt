package io.github.dokuendev.dokuenreader.dictionary

/**
 * Exception thrown by Dictionary plugins to report domain-specific errors.
 *
 * When thrown from [DictionaryPluginService.onLookup] or
 * [DictionaryPluginService.onInitialize], the SDK catches this exception and
 * forwards the [errorCode] and [message] to the host app.
 *
 * ```kotlin
 * throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "No entry found")
 * ```
 *
 * @property errorCode An error code from [DictionaryErrorCode].
 * @param message A user-friendly error message.
 */
class DictionaryException(
    val errorCode: Int,
    message: String
) : Exception(message)
