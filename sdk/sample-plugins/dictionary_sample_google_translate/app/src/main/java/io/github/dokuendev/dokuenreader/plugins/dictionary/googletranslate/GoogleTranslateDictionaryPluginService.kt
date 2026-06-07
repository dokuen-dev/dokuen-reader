package io.github.dokuendev.dokuenreader.plugins.dictionary.googletranslate

import android.os.Bundle
import android.util.Log
import io.github.dokuendev.dokuenreader.dictionary.DictionaryEntry
import io.github.dokuendev.dokuenreader.dictionary.DictionaryErrorCode
import io.github.dokuendev.dokuenreader.dictionary.DictionaryException
import io.github.dokuendev.dokuenreader.dictionary.DictionaryPluginService
import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult
import io.github.dokuendev.dokuenreader.dictionary.InlineStyle
import io.github.dokuendev.dokuenreader.dictionary.StyledSpan
import io.github.dokuendev.dokuenreader.dictionary.StyledText
import io.github.dokuendev.dokuenreader.plugin.core.ConfigField
import io.github.dokuendev.dokuenreader.plugin.core.ConfigFieldType
import io.github.dokuendev.dokuenreader.plugin.core.InitResult
import io.github.dokuendev.dokuenreader.plugin.core.InitResultFactory
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Google Translate Dictionary Plugin
 *
 * This sample demonstrates:
 * - Cloud-based translation using Google Cloud Translation API (Basic)
 * - BYOK (Bring Your Own Key) model for user-provided API keys
 * - Network-based dictionary lookup with REQUIRES_INTERNET = true
 * - Multiple target language support
 * - Error handling for network and API failures
 * - Configuration validation
 *
 * ## API Used: Cloud Translation - Basic (NMT)
 *
 * This plugin uses the Cloud Translation - Basic API with Neural Machine Translation:
 * - Endpoint: https://translation.googleapis.com/language/translate/v2
 * - Cost: $20 per 1M characters (first 500K free per month)
 * - Quality: High-quality neural translations
 * - Speed: Fast (typically < 1 second)
 * - Authentication: API key only
 *
 * ## Alternative: Cloud Translation - Advanced (Translation LLM)
 *
 * For higher quality translations, Google offers Cloud Translation - Advanced which
 * includes a Translation LLM (TLLM) model in addition to the standard NMT model.
 *
 * **Key features of Advanced API:**
 * - **Translation LLM (TLLM)** - Large language model optimized for translation
 * - **Better quality** - More natural, context-aware translations
 * - **Adaptive Translation** - Customization with limited example pairs
 * - **Glossary support** - Custom terminology for domain-specific translations
 * - **Batch translation** - Translate entire documents
 * - **Model selection** - Choose between NMT and TLLM models
 * - **Document context** - Better handling of document-level context
 *
 * **Endpoint:** `https://translation.googleapis.com/v3/projects/{project}/locations/{location}:translateText`
 *
 * **Example Advanced API request:**
 * ```kotlin
 * val requestBody = JSONObject().apply {
 *     put("contents", JSONArray().apply { put(text) })
 *     put("sourceLanguageCode", sourceLang)
 *     put("targetLanguageCode", targetLang)
 *     put("mimeType", "text/plain")
 *     // Optional: specify TLLM model for higher quality
 *     put("model", "projects/{projectId}/locations/global/models/general/tllm")
 * }
 * ```
 *
 * **Authentication for Advanced API:**
 * - Requires OAuth 2.0 or service account credentials (not just API key)
 * - More complex setup than Basic API
 *
 * **Cost comparison:**
 * - Basic (NMT): $20 per 1M characters
 * - Advanced (NMT): $20 per 1M characters
 * - Advanced (TLLM): $80 per 1M characters
 *
 * **Why this sample uses Basic API:**
 * - Simpler authentication (API key only)
 * - Lower cost for typical use cases
 * - Faster response times
 * - Sufficient quality for most dictionary lookups
 *
 * For production plugins requiring highest quality, consider upgrading to Advanced API with TLLM.
 * See: https://cloud.google.com/translate/docs/overview
 *
 * ## Note: Using Gemini Directly (Not Recommended)
 *
 * You could also use Gemini API directly for translation, but this is NOT recommended:
 * - Requires complex prompt engineering
 * - Inconsistent output format requiring parsing
 * - Higher latency
 * - More expensive
 * - Not optimized for translation tasks
 *
 * Use Cloud Translation - Advanced with TLLM instead for LLM-quality translations.
 */
class GoogleTranslateDictionaryPluginService : DictionaryPluginService() {

    companion object {
        private const val TAG = "GoogleTranslatePlugin"
        private const val TRANSLATE_API_URL = "https://translation.googleapis.com/language/translate/v2"
        private const val TIMEOUT_MS = 30000 // 30 seconds
    }

    private object ConfigKey {
        const val API_KEY = "api_key"
    }

    private var apiKey: String? = null
    private var sourceLanguage: String = "ja"
    private var targetLanguage: String = "en"
    private var isDarkTheme: Boolean = false

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, true)
        putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, false)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, true)
        putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, arrayOf("ja", "zh"))

        // Common target languages - this list can be expanded arbitrarily
        // Google Translate supports 100+ languages, but we list the most common ones here
        // Full list: https://cloud.google.com/translate/docs/languages
        putStringArray(
            PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES,
            arrayOf(
                "en", // English
                "es", // Spanish
                "fr", // French
                "de", // German
                "it", // Italian
                "pt", // Portuguese
                "ru", // Russian
                "zh-CN", // Chinese (Simplified)
                "zh-TW", // Chinese (Traditional)
                "ko", // Korean
                "ar", // Arabic
                "hi", // Hindi
                "th", // Thai
                "vi", // Vietnamese
                "id", // Indonesian
                "nl", // Dutch
                "pl", // Polish
                "tr", // Turkish
                "sv", // Swedish
                "no", // Norwegian
                "da", // Danish
                "fi", // Finnish
                "cs", // Czech
                "ro", // Romanian
                "hu", // Hungarian
                "el", // Greek
                "he", // Hebrew
                "uk", // Ukrainian
                "ms", // Malay
                "fa"  // Persian
                // NOTE: This list can be expanded to include any of the 100+ languages
                // supported by Google Translate. See the full list at:
                // https://cloud.google.com/translate/docs/languages
            )
        )
    }

    override val configSchema = listOf(
        ConfigField(
            key = ConfigKey.API_KEY,
            displayName = "API Key",
            description = "Google Cloud API Key with Cloud Translation API enabled",
            type = ConfigFieldType.STRING,
            defaultValue = null,
            isRequired = true,
            regexPattern = "^AIza[a-zA-Z0-9_-]{35}$"
        )
    )

    override suspend fun onInitialize(config: Bundle?): InitResult {
        super.onInitialize(config)

        if (config == null) {
            return InitResultFactory.failure("Configuration is missing")
        }

        apiKey = config.getString(ConfigKey.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            return InitResultFactory.failure("API key is required")
        }

        // Read host-provided configuration
        sourceLanguage = config.getString(PluginHostConfigKeys.LANGUAGE) ?: "ja"
        targetLanguage = config.getString(PluginHostConfigKeys.TARGET_LANGUAGE) ?: "en"

        // Determine the UI Theme
        val themeString = config.getString(PluginHostConfigKeys.UI_THEME) ?: "light"
        isDarkTheme = (themeString == "dark")

        Log.d(
            TAG,
            "Initialized with source language: $sourceLanguage, target language: $targetLanguage, dark theme: $isDarkTheme"
        )
        return InitResultFactory.success()
    }

    /**
     * Perform translation lookup using Google Cloud Translation API.
     *
     * Since HANDLES_SEGMENTATION = true, this method will receive the raw text
     * as selected by the user, rather than segmented or deinflected forms.
     *
     * @param contextText The full text context
     * @param cursorStartIndex Start index of the query word
     * @param cursorEndIndex End index of the query word
     * @return DictionaryResult containing translation
     */
    override suspend fun onLookup(
        contextText: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int
    ): DictionaryResult {
        // Extract the query word
        val queryWord = contextText.substring(cursorStartIndex, cursorEndIndex)

        if (queryWord.isBlank()) {
            throw DictionaryException(DictionaryErrorCode.INVALID_QUERY, "Query word is empty")
        }

        Log.d(TAG, "Translating '$queryWord' from $sourceLanguage to $targetLanguage")

        // Call Google Translate API
        val translation = try {
            callTranslateApi(queryWord, sourceLanguage, targetLanguage)
        } catch (e: SocketTimeoutException) {
            throw DictionaryException(DictionaryErrorCode.NETWORK_ERROR, "Request timed out")
        } catch (e: IOException) {
            throw DictionaryException(DictionaryErrorCode.NETWORK_ERROR, "Network error: ${e.message}")
        }

        // Create dictionary entry from translation
        val entry = createTranslationEntry(queryWord, translation, sourceLanguage, targetLanguage)

        return DictionaryResult(entries = arrayOf(entry))
    }

    /**
     * Call Google Cloud Translation API (NMT).
     *
     * API Documentation: https://cloud.google.com/translate/docs/reference/rest/v2/translate
     *
     * @param text Text to translate
     * @param sourceLang Source language code
     * @param targetLang Target language code
     * @return Translated text
     */
    private suspend fun callTranslateApi(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val url = URL("$TRANSLATE_API_URL?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            // Build request body
            val requestBody = JSONObject().apply {
                put("q", text)
                put("source", sourceLang)
                put("target", targetLang)
                put("format", "text")
            }

            // Send request
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            // Check for cancellation
            currentCoroutineContext().ensureActive()

            // Handle response
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = InputStreamReader(connection.inputStream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
                return parseTranslationResponse(response)
            } else {
                val errorBody = try {
                    InputStreamReader(connection.errorStream, Charsets.UTF_8).use { it.readText() }
                } catch (e: Exception) {
                    "Unable to read error response"
                }

                throw when (responseCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> {
                        DictionaryException(
                            DictionaryErrorCode.AUTHENTICATION_ERROR,
                            "Invalid API key or insufficient permissions"
                        )
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        DictionaryException(
                            DictionaryErrorCode.INVALID_ARGUMENT,
                            "Invalid request: $errorBody"
                        )
                    }

                    429 -> { // Too Many Requests
                        DictionaryException(
                            DictionaryErrorCode.INTERNAL_ERROR,
                            "API quota exceeded or rate limit reached"
                        )
                    }

                    HttpURLConnection.HTTP_UNAVAILABLE -> {
                        DictionaryException(
                            DictionaryErrorCode.NETWORK_ERROR,
                            "Translation API temporarily unavailable"
                        )
                    }

                    else -> {
                        DictionaryException(
                            DictionaryErrorCode.INTERNAL_ERROR,
                            "API error (HTTP $responseCode): $errorBody"
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse the JSON response from Google Translate API.
     *
     * Response format:
     * ```json
     * {
     *   "data": {
     *     "translations": [
     *       {
     *         "translatedText": "to eat",
     *         "detectedSourceLanguage": "ja"
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    private fun parseTranslationResponse(response: String): String {
        val json = JSONObject(response)
        val data = json.getJSONObject("data")
        val translations = data.getJSONArray("translations")

        if (translations.length() == 0) {
            throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "No translation returned from API"
            )
        }

        val firstTranslation = translations.getJSONObject(0)
        return firstTranslation.getString("translatedText")
    }

    /**
     * Create a dictionary entry from the translation result.
     */
    private fun createTranslationEntry(
        originalText: String,
        translation: String,
        sourceLang: String,
        targetLang: String
    ): DictionaryEntry {

        val metadataStr = "Translated via Google Translate (NMT)\nSource: $sourceLang → Target: $targetLang"
        val noteStr = "This is a sample Dokuen translator plugin. " +
                "To customize the translation model, tweak the output formatting, or remove this message, " +
                "just fork the repo and modify the code. Visit the Dokuen Plugin SDK page for more info."

        val bodyText = "$translation\n\n$metadataStr\n$noteStr"

        val metaStart = translation.length + 2
        val metaEnd = metaStart + metadataStr.length
        val noteStart = metaEnd + 1
        val noteEnd = bodyText.length

        // Select an appropriate muted color based on the current theme (ARGB format)
        // Dark theme gets a lighter gray to remain visible, light theme gets a darker gray
        val mutedColor = if (isDarkTheme) 0xFFAAAAAA.toInt() else 0xFF757575.toInt()

        return DictionaryEntry(
            headword = originalText,
            pronunciation = null, // Null tells Dokuen to use the default pronunciation from the overlay
            body = StyledText(
                text = bodyText,
                styledSpans = arrayOf(
                    // Style 1: Metadata (Italic, slightly smaller, muted foreground color)
                    StyledSpan(
                        startIndex = metaStart,
                        endIndex = metaEnd,
                        style = InlineStyle(
                            italic = true,
                            fontSize = 0.75f,
                            foregroundColor = mutedColor
                        )
                    ),
                    // Style 2: Disclaimer Note (Normal, much smaller, muted foreground color)
                    StyledSpan(
                        startIndex = noteStart,
                        endIndex = noteEnd,
                        style = InlineStyle(
                            italic = false,
                            fontSize = 0.65f,
                            foregroundColor = mutedColor
                        )
                    )
                ),
                rubySpans = emptyArray()
            )
        )
    }

    override fun onShutdown() {
        super.onShutdown()
        apiKey = null
        Log.d(TAG, "Plugin shut down")
    }
}
