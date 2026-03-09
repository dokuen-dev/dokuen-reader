package io.github.dokuendev.dokuenreader.dictionary

import io.github.dokuendev.dokuenreader.plugin.core.PluginErrorCode

/**
 * Placeholder error codes for Dictionary plugins.
 */
object DictionaryErrorCode {
    const val SUCCESS = PluginErrorCode.SUCCESS
    const val NETWORK_ERROR = PluginErrorCode.NETWORK_ERROR
    const val AUTHENTICATION_ERROR = PluginErrorCode.AUTHENTICATION_ERROR
    const val INVALID_ARGUMENT = PluginErrorCode.INVALID_ARGUMENT
    const val INTERNAL_ERROR = PluginErrorCode.INTERNAL_ERROR

    // Dictionary specific codes
    const val WORD_NOT_FOUND = 200
    const val INVALID_QUERY = 201
}
