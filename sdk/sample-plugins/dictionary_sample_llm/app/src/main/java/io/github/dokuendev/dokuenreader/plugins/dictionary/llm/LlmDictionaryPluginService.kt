package io.github.dokuendev.dokuenreader.plugins.dictionary.llm

import android.os.Bundle
import android.util.Log
import io.github.dokuendev.dokuenreader.dictionary.DictionaryEntry
import io.github.dokuendev.dokuenreader.dictionary.DictionaryErrorCode
import io.github.dokuendev.dokuenreader.dictionary.DictionaryException
import io.github.dokuendev.dokuenreader.dictionary.DictionaryPluginService
import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult
import io.github.dokuendev.dokuenreader.dictionary.InlineStyle
import io.github.dokuendev.dokuenreader.dictionary.RubySpan
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale

/**
 * LLM Dictionary Plugin
 *
 * This plugin leverages modern LLMs (Gemini, Claude, or ChatGPT) to provide
 * context-aware definitions. It supports Structured Outputs to ensure responses
 * are delivered in a consistent JSON format, which are then rendered into
 * beautiful, styled dictionary entries with accurate Japanese RubySpan annotations.
 */
class LlmDictionaryPluginService : DictionaryPluginService() {

    companion object {
        private const val TAG = "LlmDictionaryPlugin"
        private const val TIMEOUT_MS = 30000 // 30 seconds

        // LLM Providers
        const val PROVIDER_GEMINI = "Gemini"
        const val PROVIDER_CLAUDE = "Claude"
        const val PROVIDER_CHATGPT = "ChatGPT"
    }

    private object ConfigKey {
        const val PROVIDER = "provider"
        const val API_KEY = "api_key"
    }

    private var provider: String = PROVIDER_GEMINI
    private var apiKey: String? = null
    private var sourceLanguage: String = "ja"
    private var targetLanguage: String = "en"
    private var isDarkTheme: Boolean = false

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, true)
        putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, false)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, true)
        putStringArray(
            PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES,
            arrayOf("ja")
        )
        putStringArray(
            PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES,
            arrayOf(
                "en", "es", "fr", "de", "it", "pt", "ru", "zh-CN", "zh-TW", "ko", "ja"
            )
        )
    }

    override val configSchema = listOf(
        ConfigField(
            key = ConfigKey.PROVIDER,
            displayName = "LLM Provider",
            description = "Select the LLM provider to use for looking up definitions.",
            type = ConfigFieldType.ENUM,
            defaultValue = PROVIDER_GEMINI,
            isRequired = true,
            enumValues = listOf(PROVIDER_GEMINI, PROVIDER_CLAUDE, PROVIDER_CHATGPT)
        ),
        ConfigField(
            key = ConfigKey.API_KEY,
            displayName = "API Key",
            description = "API key for the selected LLM provider.",
            type = ConfigFieldType.STRING,
            defaultValue = null,
            isRequired = true
        )
    )

    override suspend fun onInitialize(config: Bundle?): InitResult {
        super.onInitialize(config)

        if (config == null) {
            return InitResultFactory.failure("Configuration is missing")
        }

        provider = config.getString(ConfigKey.PROVIDER) ?: PROVIDER_GEMINI
        apiKey = config.getString(ConfigKey.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            return InitResultFactory.failure("API key is required")
        }

        // Read host configuration
        sourceLanguage = config.getString(PluginHostConfigKeys.LANGUAGE) ?: "ja"
        targetLanguage = config.getString(PluginHostConfigKeys.TARGET_LANGUAGE) ?: "en"

        val themeString = config.getString(PluginHostConfigKeys.UI_THEME) ?: "light"
        isDarkTheme = (themeString == "dark")

        Log.d(TAG, "Initialized: provider=$provider, source=$sourceLanguage, target=$targetLanguage, dark=$isDarkTheme")
        return InitResultFactory.success()
    }

    /**
     * Look up a word's definition by querying the selected LLM API.
     *
     * IMPORTANT/UNORTHODOX ERROR HANDLING BEHAVIOR:
     * This sample catches all exceptions (authentication errors, rate limits, quota issues,
     * network timeouts, JSON parsing errors) and formats them directly into a successful
     * DictionaryResult containing an error card. This guarantees that detailed, formatted error
     * information is immediately displayed to the user inside the host's dictionary dialog box.
     *
     * WARNING FOR THIRD-PARTY PLUGIN AUTHORS:
     * If a production plugin catches lookup exceptions and returns them as a successful
     * DictionaryResult, the host application will interpret this as a successful definition lookup.
     * Consequently, the host will NOT fall back to try subsequent active dictionary or translation
     * engines. Real plugins should propagate errors by throwing a standard DictionaryException.
     * This allows the host app's fallback (e.g. Google Translate) to execute seamlessly when this
     * plugin fails.
     */
    override suspend fun onLookup(
        contextText: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int
    ): DictionaryResult {
        val queryWord = contextText.substring(cursorStartIndex, cursorEndIndex).trim()
        if (queryWord.isEmpty()) {
            throw DictionaryException(DictionaryErrorCode.INVALID_QUERY, "Query word is empty")
        }

        return try {
            val key = apiKey ?: throw DictionaryException(
                DictionaryErrorCode.AUTHENTICATION_ERROR,
                "API key is not configured"
            )

            Log.d(TAG, "Looking up '$queryWord' using $provider (source=$sourceLanguage, target=$targetLanguage)")

            val jsonResponse = try {
                when (provider) {
                    PROVIDER_GEMINI -> callGeminiApi(queryWord, contextText, cursorStartIndex, cursorEndIndex, key)
                    PROVIDER_CLAUDE -> callClaudeApi(queryWord, contextText, cursorStartIndex, cursorEndIndex, key)
                    PROVIDER_CHATGPT -> callChatGPTApi(queryWord, contextText, cursorStartIndex, cursorEndIndex, key)
                    else -> throw DictionaryException(
                        DictionaryErrorCode.INVALID_ARGUMENT,
                        "Unsupported provider: $provider"
                    )
                }
            } catch (e: SocketTimeoutException) {
                throw DictionaryException(DictionaryErrorCode.NETWORK_ERROR, "Request timed out")
            } catch (e: IOException) {
                throw DictionaryException(DictionaryErrorCode.NETWORK_ERROR, "Network error: ${e.message}")
            }

            val entries = parseLlmResponse(queryWord, jsonResponse)
            if (entries.isEmpty()) {
                throw DictionaryException(
                    DictionaryErrorCode.WORD_NOT_FOUND,
                    "No definitions found for '$queryWord'"
                )
            }

            DictionaryResult(entries = entries.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up word '$queryWord'", e)
            val errorEntry = createErrorEntry(queryWord, e)
            DictionaryResult(entries = arrayOf(errorEntry))
        }
    }

    /**
     * Formats a lookup exception into a beautiful, theme-aware error card DictionaryEntry.
     *
     * This acts as an educational showcase on how to build custom styled text and inline spans
     * dynamically matching light/dark theme contrast.
     */
    private fun createErrorEntry(query: String, error: Exception): DictionaryEntry {
        val bodyBuilder = java.lang.StringBuilder()
        val styledSpans = mutableListOf<StyledSpan>()

        // Theme-optimized color palette matching premium aesthetics
        val errorColor = if (isDarkTheme) 0xFFE57373.toInt() else 0xFFD32F2F.toInt()
        val mutedColor = if (isDarkTheme) 0xFFB0BEC5.toInt() else 0xFF757575.toInt()

        // 1. Title Block - Header
        val titleText = "[Error] LLM Lookup Failed\n\n"
        val titleStart = bodyBuilder.length
        bodyBuilder.append(titleText)
        val titleEnd = bodyBuilder.length

        styledSpans.add(
            StyledSpan(
                startIndex = titleStart,
                endIndex = titleEnd,
                style = InlineStyle(
                    bold = true,
                    fontSize = 1.1f,
                    foregroundColor = errorColor
                )
            )
        )

        // 2. Body Block - Exact Exception Message
        val errorMessage = error.message ?: "An unknown error occurred during lookup."
        val msgStart = bodyBuilder.length
        bodyBuilder.append(errorMessage).append("\n\n")
        val msgEnd = bodyBuilder.length

        styledSpans.add(
            StyledSpan(
                startIndex = msgStart,
                endIndex = msgEnd,
                style = InlineStyle(
                    italic = true,
                    fontSize = 0.95f
                )
            )
        )

        // 3. Recommendation Block - Actionable recommendations based on error type
        val suggestion = when {
            error is DictionaryException && error.errorCode == DictionaryErrorCode.AUTHENTICATION_ERROR -> {
                "Please verify your API key in the plugin configuration settings and ensure it has correct permissions."
            }

            error is DictionaryException && error.errorCode == DictionaryErrorCode.QUOTA_EXCEEDED -> {
                "Your LLM API quota or rate limit has been exceeded. Please check your billing status or try again later."
            }

            error is DictionaryException && error.errorCode == DictionaryErrorCode.NETWORK_ERROR -> {
                "A network error occurred. Please check your internet connection or verify the LLM API service status."
            }

            error is SocketTimeoutException || (error is DictionaryException && error.message?.contains(
                "time out",
                ignoreCase = true
            ) == true) -> {
                "The request timed out. The LLM API might be slow or unreachable. Please try again."
            }

            errorMessage.contains("rate limit", ignoreCase = true) || errorMessage.contains("429") -> {
                "API rate limit or quota exceeded. Please check your billing status or try again later."
            }

            errorMessage.contains("key", ignoreCase = true) || errorMessage.contains(
                "auth",
                ignoreCase = true
            ) || errorMessage.contains("401") || errorMessage.contains("403") -> {
                "Authentication failed. Please verify your API key in the plugin configuration."
            }

            else -> {
                "Please check your API key, internet connection, and the plugin configuration."
            }
        }

        val suggStart = bodyBuilder.length
        bodyBuilder.append("Recommendation:\n").append("  • ").append(suggestion).append("\n\n")
        val suggEnd = bodyBuilder.length

        styledSpans.add(
            StyledSpan(
                startIndex = suggStart,
                endIndex = suggEnd,
                style = InlineStyle(
                    fontSize = 0.85f,
                    foregroundColor = mutedColor
                )
            )
        )

        // 4. Metadata Block - Standard Disclaimer & Diagnostic Context
        val metaStart = bodyBuilder.length
        val srcName = getLanguageName(sourceLanguage)
        val tgtName = getLanguageName(targetLanguage)
        val metaText = "Model: $provider • Source: $srcName → Target: $tgtName • Error Report"
        bodyBuilder.append(metaText)
        val metaEnd = bodyBuilder.length

        styledSpans.add(
            StyledSpan(
                startIndex = metaStart,
                endIndex = metaEnd,
                style = InlineStyle(
                    italic = true,
                    fontSize = 0.65f,
                    foregroundColor = mutedColor
                )
            )
        )

        return DictionaryEntry(
            headword = query,
            pronunciation = null,
            body = StyledText(
                text = bodyBuilder.toString(),
                styledSpans = styledSpans.toTypedArray(),
                rubySpans = emptyArray()
            )
        )
    }

    /**
     * Build unified definitions JSON schema for Structured Outputs
     */
    private fun buildDefinitionsSchema(useUppercaseTypes: Boolean, includeAdditionalProperties: Boolean): JSONObject {
        val typeObj = if (useUppercaseTypes) "OBJECT" else "object"
        val typeArr = if (useUppercaseTypes) "ARRAY" else "array"
        val typeStr = if (useUppercaseTypes) "STRING" else "string"

        return JSONObject().apply {
            put("type", typeObj)
            put("properties", JSONObject().apply {
                put("definitions", JSONObject().apply {
                    put("type", typeArr)
                    put("items", JSONObject().apply {
                        put("type", typeObj)
                        put("properties", JSONObject().apply {
                            put("partOfSpeech", JSONObject().apply { put("type", typeStr) })
                            put("definition", JSONObject().apply { put("type", typeStr) })
                            put("pronunciation", JSONObject().apply { put("type", typeStr) })
                            put("examples", JSONObject().apply {
                                put("type", typeArr)
                                put("items", JSONObject().apply { put("type", typeStr) })
                            })
                        })
                        put("required", JSONArray().apply {
                            put("partOfSpeech")
                            put("definition")
                            put("pronunciation")
                            put("examples")
                        })
                        if (includeAdditionalProperties) {
                            put("additionalProperties", false)
                        }
                    })
                })
            })
            put("required", JSONArray().apply { put("definitions") })
            if (includeAdditionalProperties) {
                put("additionalProperties", false)
            }
        }
    }

    /**
     * Get the standardized system prompt for the AI assistants
     */
    private fun getSystemPrompt(providerSuffix: String = ""): String {
        val targetLanguageName = getLanguageName(targetLanguage)
        val base =
            "You are a Japanese-$targetLanguageName dictionary. Given a word and its context sentence, define it accurately. The pronunciation field MUST be the hiragana reading of the word."
        return if (providerSuffix.isNotEmpty()) "$base $providerSuffix" else base
    }

    /**
     * Get the standardized user prompt
     */
    private fun getUserPrompt(query: String, context: String, cursorStartIndex: Int, cursorEndIndex: Int): String {
        return "Define the word '$query' as used at positions [$cursorStartIndex, $cursorEndIndex) in this context sentence:\n\"$context\""
    }

    /**
     * Helper to map language code to its real English display name
     */
    private fun getLanguageName(code: String): String {
        return when (code.lowercase(Locale.US)) {
            "ja" -> "Japanese"
            "zh" -> "Chinese"
            "zh-cn" -> "Chinese (Simplified)"
            "zh-tw" -> "Chinese (Traditional)"
            "en" -> "English"
            "ko" -> "Korean"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            else -> Locale.forLanguageTag(code).getDisplayLanguage(Locale.US).takeIf { it.isNotEmpty() } ?: code
        }
    }

    /**
     * Call Gemini API using responseSchema for Structured Outputs
     */
    private suspend fun callGeminiApi(
        query: String,
        context: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int,
        key: String
    ): String {
        val url =
            URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")

        val systemInstructionText = getSystemPrompt("Respond ONLY in the requested JSON schema.")
        val userPrompt = getUserPrompt(query, context, cursorStartIndex, cursorEndIndex)

        val requestBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstructionText) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put(
                    "responseSchema",
                    buildDefinitionsSchema(useUppercaseTypes = true, includeAdditionalProperties = false)
                )
            })
        }

        val response = executePostRequest(url, requestBody.toString(), mapOf("Content-Type" to "application/json"))

        val responseJson = JSONObject(response)
        val candidates = responseJson.optJSONArray("candidates")
            ?: throw DictionaryException(
                DictionaryErrorCode.INTERNAL_ERROR,
                "Invalid Gemini API response format: $response"
            )
        if (candidates.length() == 0) {
            throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "No definition generated by Gemini")
        }
        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
            ?: throw DictionaryException(DictionaryErrorCode.INTERNAL_ERROR, "Gemini generated no content")
        val parts = content.optJSONArray("parts")
            ?: throw DictionaryException(DictionaryErrorCode.INTERNAL_ERROR, "Gemini response is empty")
        if (parts.length() == 0) {
            throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "Gemini response has no parts")
        }
        return parts.getJSONObject(0).getString("text")
    }

    /**
     * Call ChatGPT API using JSON schema response_format for Structured Outputs
     */
    private suspend fun callChatGPTApi(
        query: String,
        context: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int,
        key: String
    ): String {
        val url = URL("https://api.openai.com/v1/chat/completions")

        val systemPrompt = getSystemPrompt()
        val userPrompt = getUserPrompt(query, context, cursorStartIndex, cursorEndIndex)

        val requestBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("response_format", JSONObject().apply {
                put("type", "json_schema")
                put("json_schema", JSONObject().apply {
                    put("name", "dictionary_definition")
                    put("strict", true)
                    put("schema", buildDefinitionsSchema(useUppercaseTypes = false, includeAdditionalProperties = true))
                })
            })
        }

        val response = executePostRequest(
            url,
            requestBody.toString(),
            mapOf(
                "Authorization" to "Bearer $key",
                "Content-Type" to "application/json"
            )
        )

        val responseJson = JSONObject(response)
        val choices = responseJson.optJSONArray("choices")
            ?: throw DictionaryException(
                DictionaryErrorCode.INTERNAL_ERROR,
                "Invalid ChatGPT response format: $response"
            )
        if (choices.length() == 0) {
            throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "No choices generated by ChatGPT")
        }
        val choice = choices.getJSONObject(0)
        val message = choice.optJSONObject("message")
            ?: throw DictionaryException(DictionaryErrorCode.INTERNAL_ERROR, "ChatGPT generated no message")
        return message.getString("content")
    }

    /**
     * Call Claude API using forced tool choice for Structured Outputs
     */
    private suspend fun callClaudeApi(
        query: String,
        context: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int,
        key: String
    ): String {
        val url = URL("https://api.anthropic.com/v1/messages")

        val systemPrompt = getSystemPrompt("You must call the `define_word` tool.")
        val userPrompt = getUserPrompt(query, context, cursorStartIndex, cursorEndIndex)

        val requestBody = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 2048)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "define_word")
                    put("description", "Provide a structured definition for the dictionary entry.")
                    put(
                        "input_schema",
                        buildDefinitionsSchema(useUppercaseTypes = false, includeAdditionalProperties = false)
                    )
                })
            })
            put("tool_choice", JSONObject().apply {
                put("type", "tool")
                put("name", "define_word")
            })
        }

        val response = executePostRequest(
            url,
            requestBody.toString(),
            mapOf(
                "x-api-key" to key,
                "anthropic-version" to "2023-06-01",
                "Content-Type" to "application/json"
            )
        )

        val responseJson = JSONObject(response)
        val contentArray = responseJson.optJSONArray("content")
            ?: throw DictionaryException(
                DictionaryErrorCode.INTERNAL_ERROR,
                "Invalid Claude response format: $response"
            )
        if (contentArray.length() == 0) {
            throw DictionaryException(DictionaryErrorCode.WORD_NOT_FOUND, "No content generated by Claude")
        }

        // Find the tool_use block
        for (i in 0 until contentArray.length()) {
            val contentObj = contentArray.getJSONObject(i)
            if (contentObj.optString("type") == "tool_use") {
                val inputObj = contentObj.optJSONObject("input")
                if (inputObj != null) {
                    return inputObj.toString()
                }
            }
        }

        throw DictionaryException(DictionaryErrorCode.INTERNAL_ERROR, "Claude did not call the define_word tool")
    }

    /**
     * Execute HttpURLConnection POST request with cancellation check
     */
    private suspend fun executePostRequest(
        url: URL,
        postData: String,
        headers: Map<String, String>
    ): String {
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            currentCoroutineContext().ensureActive()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                return InputStreamReader(connection.inputStream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
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
                            "Invalid API request argument"
                        )
                    }

                    429 -> {
                        DictionaryException(
                            DictionaryErrorCode.INTERNAL_ERROR,
                            "API rate limit or quota exceeded"
                        )
                    }

                    else -> {
                        val userMessage = try {
                            JSONObject(errorBody)
                                .optJSONObject("error")
                                ?.optString("message")
                                ?.takeIf { it.isNotEmpty() }
                        } catch (_: Exception) {
                            null
                        } ?: "HTTP error $responseCode"

                        DictionaryException(
                            DictionaryErrorCode.INTERNAL_ERROR,
                            userMessage
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class LlmDefinition(
        val partOfSpeech: String,
        val definition: String,
        val pronunciation: String,
        val examples: List<String>
    )

    /**
     * Unify the JSON responses to map to entries
     */
    private fun parseLlmResponse(query: String, jsonStr: String): List<DictionaryEntry> {
        val entries = mutableListOf<DictionaryEntry>()
        try {
            val root = JSONObject(jsonStr)
            val definitionsArray = root.optJSONArray("definitions") ?: return emptyList()

            for (i in 0 until definitionsArray.length()) {
                val defObj = definitionsArray.getJSONObject(i)
                val partOfSpeech = defObj.optString("partOfSpeech", "").trim()
                val definition = defObj.optString("definition", "").trim()
                val pronunciation = defObj.optString("pronunciation", "").trim()

                val examplesList = mutableListOf<String>()
                val examplesArray = defObj.optJSONArray("examples")
                if (examplesArray != null) {
                    for (j in 0 until examplesArray.length()) {
                        val ex = examplesArray.optString(j, "").trim()
                        if (ex.isNotEmpty()) {
                            examplesList.add(ex)
                        }
                    }
                }

                if (definition.isNotEmpty()) {
                    val llmDef = LlmDefinition(partOfSpeech, definition, pronunciation, examplesList)
                    val entry = createEntryFromLlmDefinition(query, llmDef, i == definitionsArray.length() - 1)
                    entries.add(entry)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON definition response", e)
            throw DictionaryException(
                DictionaryErrorCode.INTERNAL_ERROR,
                "Failed to parse structured response: ${e.message}"
            )
        }
        return entries
    }

    /**
     * Create premium formatted, theme-aware DictionaryEntry with accurate pronunciation annotations
     */
    private fun createEntryFromLlmDefinition(
        query: String,
        llmDef: LlmDefinition,
        isLast: Boolean
    ): DictionaryEntry {
        val prons = if (sourceLanguage.lowercase(Locale.US) == "ja" && llmDef.pronunciation.isNotEmpty()) {
            buildRubySpans(query, llmDef.pronunciation).takeIf { it.isNotEmpty() }?.toTypedArray()
        } else {
            null
        }

        val bodyBuilder = java.lang.StringBuilder()
        val styledSpans = mutableListOf<StyledSpan>()

        // Harmonies of color depending on UI theme
        val posColor = if (isDarkTheme) 0xFF4FC3F7.toInt() else 0xFF0288D1.toInt()
        val mutedColor = if (isDarkTheme) 0xFFB0BEC5.toInt() else 0xFF757575.toInt()

        // 1. Part of speech tag
        val posText = if (llmDef.partOfSpeech.isNotEmpty()) {
            "[${llmDef.partOfSpeech}] "
        } else {
            ""
        }

        if (posText.isNotEmpty()) {
            val start = bodyBuilder.length
            bodyBuilder.append(posText)
            val end = bodyBuilder.length
            styledSpans.add(
                StyledSpan(
                    startIndex = start,
                    endIndex = end,
                    style = InlineStyle(
                        bold = true,
                        italic = true,
                        fontSize = 0.9f,
                        foregroundColor = posColor
                    )
                )
            )
        }

        // 2. Main definition text
        val defStart = bodyBuilder.length
        bodyBuilder.append(llmDef.definition)
        val defEnd = bodyBuilder.length
        styledSpans.add(
            StyledSpan(
                startIndex = defStart,
                endIndex = defEnd,
                style = InlineStyle(
                    fontSize = 1.0f
                )
            )
        )

        // 3. Example sentences styled list items
        if (llmDef.examples.isNotEmpty()) {
            bodyBuilder.append("\n\n")
            val exTitleStart = bodyBuilder.length
            bodyBuilder.append("Examples:")
            val exTitleEnd = bodyBuilder.length
            styledSpans.add(
                StyledSpan(
                    startIndex = exTitleStart,
                    endIndex = exTitleEnd,
                    style = InlineStyle(
                        bold = true,
                        fontSize = 0.85f,
                        foregroundColor = mutedColor
                    )
                )
            )

            llmDef.examples.forEach { example ->
                bodyBuilder.append("\n")
                val itemStart = bodyBuilder.length
                bodyBuilder.append("  • ").append(example)
                val itemEnd = bodyBuilder.length

                styledSpans.add(
                    StyledSpan(
                        startIndex = itemStart,
                        endIndex = itemEnd,
                        style = InlineStyle(
                            italic = true,
                            fontSize = 0.85f,
                            foregroundColor = mutedColor
                        )
                    )
                )
            }
        }

        // 4. Source & Model Metadata Disclaimer
        if (isLast) {
            bodyBuilder.append("\n\n")
            val metaStart = bodyBuilder.length
            val srcName = getLanguageName(sourceLanguage)
            val tgtName = getLanguageName(targetLanguage)
            val metaText = "Model: $provider • Source: $srcName → Target: $tgtName • Context-aware definition"
            bodyBuilder.append(metaText)
            val noteStr = "\n\nThis is a sample Dokuen dictionary plugin. " +
                    "To customize the LLM prompt, tweak the output formatting, or remove this message, " +
                    "just fork the repo and modify the code. Visit the Dokuen Plugin SDK page for more info."
            bodyBuilder.append(noteStr)
            val metaEnd = bodyBuilder.length

            styledSpans.add(
                StyledSpan(
                    startIndex = metaStart,
                    endIndex = metaEnd,
                    style = InlineStyle(
                        italic = true,
                        fontSize = 0.65f,
                        foregroundColor = mutedColor
                    )
                )
            )
        }

        return DictionaryEntry(
            headword = query,
            pronunciation = prons,
            body = StyledText(
                text = bodyBuilder.toString(),
                styledSpans = styledSpans.toTypedArray(),
                rubySpans = emptyArray()
            )
        )
    }

    /**
     * Highly accurate bi-directional alignment algorithm to create perfect RubySpan furigana readings.
     * Extracts common prefix kana, then common suffix kana, and maps reading to the remaining middle kanji.
     */
    private fun buildRubySpans(headword: String, reading: String): List<RubySpan> {
        val cleanHw = headword.trim()
        val cleanRd = reading.trim()

        if (cleanHw == cleanRd || cleanHw.isEmpty() || cleanRd.isEmpty()) return emptyList()

        val hwLen = cleanHw.length
        val rdLen = cleanRd.length

        // 1. Strip common kana prefix (e.g. お茶 -> お is prefix)
        var prefixLen = 0
        while (prefixLen < hwLen && prefixLen < rdLen) {
            val hwChar = cleanHw[prefixLen]
            val rdChar = cleanRd[prefixLen]
            if (hwChar.isKana() && hwChar == rdChar) {
                prefixLen++
            } else {
                break
            }
        }

        // 2. Strip common kana suffix (e.g. 食べる -> べる is suffix)
        var suffixLen = 0
        while (suffixLen < (hwLen - prefixLen) && suffixLen < (rdLen - prefixLen)) {
            val hwChar = cleanHw[hwLen - 1 - suffixLen]
            val rdChar = cleanRd[rdLen - 1 - suffixLen]
            if (hwChar.isKana() && hwChar == rdChar) {
                suffixLen++
            } else {
                break
            }
        }

        val kanjiStart = prefixLen
        val kanjiEnd = hwLen - suffixLen
        val readingEnd = rdLen - suffixLen

        if (kanjiStart >= kanjiEnd) {
            return emptyList()
        }

        return listOf(
            RubySpan(
                startIndex = kanjiStart,
                endIndex = kanjiEnd,
                rubyText = cleanRd.substring(kanjiStart, readingEnd)
            )
        )
    }

    private fun Char.isKana(): Boolean = this in '\u3040'..'\u30FF'

    override fun onShutdown() {
        super.onShutdown()
        apiKey = null
        Log.d(TAG, "Shutdown complete")
    }
}
