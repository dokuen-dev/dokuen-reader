package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Base64
import android.util.Log
import io.github.dokuendev.dokuenreader.ocr.OcrBlock
import io.github.dokuendev.dokuenreader.ocr.OcrErrorCode
import io.github.dokuendev.dokuenreader.ocr.OcrException
import io.github.dokuendev.dokuenreader.ocr.OcrPluginService
import io.github.dokuendev.dokuenreader.ocr.OcrRequirementKeys
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class GcpOcrPluginService : OcrPluginService() {

    private var apiKey: String? = null
    private var language: String? = null
    private var useDocumentTextDetection: Boolean = true

    /**
     * Intermediate representation of a parsed OCR block, before text direction is resolved.
     *
     * @param text The concatenated text content of the block.
     * @param symbolBounds Normalized bounding rectangles for each symbol in the block.
     */
    private data class ParsedBlock(
        val text: String,
        val symbolBounds: List<RectF>,
    )

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
        putBoolean(PluginCapabilityKeys.SUPPORTS_AUTO_TEXT_DIRECTION, true)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, true)
        putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja", "zh"))
    }

    override val configSchema = listOf(
        ConfigField(
            key = ConfigKey.API_KEY,
            displayName = "API Key",
            description = "GCP API Key with Cloud Vision API enabled",
            type = ConfigFieldType.STRING,
            defaultValue = null,
            isRequired = true,
            regexPattern = "^AIza[a-zA-Z0-9_-]{35}$"
        ),
        ConfigField(
            key = ConfigKey.DETECTION_MODE,
            displayName = "Text Detection Mode",
            description = "TEXT_DETECTION for sparse text (signs, labels). DOCUMENT_TEXT_DETECTION for dense text (books, documents, handwriting).",
            type = ConfigFieldType.ENUM,
            defaultValue = "DOCUMENT_TEXT_DETECTION",
            isRequired = false,
            enumValues = listOf("TEXT_DETECTION", "DOCUMENT_TEXT_DETECTION")
        )
    )

    override suspend fun onInitialize(config: Bundle?): InitResult {
        if (config == null) {
            throw OcrException(OcrErrorCode.INVALID_ARGUMENT, "Configuration is missing")
        }

        apiKey = config.getString(ConfigKey.API_KEY)
        if (apiKey.isNullOrEmpty()) {
            throw OcrException(OcrErrorCode.AUTHENTICATION_ERROR, "API key is required")
        }

        // Read host-provided language; fall back to "ja" if absent.
        language = config.getString(PluginHostConfigKeys.LANGUAGE) ?: "ja"

        // Read user config; default to DOCUMENT_TEXT_DETECTION if the user didn't set it.
        val detectionMode = config.getString(ConfigKey.DETECTION_MODE) ?: "DOCUMENT_TEXT_DETECTION"
        useDocumentTextDetection = detectionMode == "DOCUMENT_TEXT_DETECTION"

        val requirements = Bundle().apply {
            putBoolean(OcrRequirementKeys.CONVERT_TO_GRAYSCALE, true)
        }
        return InitResultFactory.success(requirements)
    }

    override suspend fun onProcessImage(
        bitmap: Bitmap,
        textDirection: String?
    ): List<OcrBlock> {
        val currentApiKey = apiKey
        if (currentApiKey.isNullOrEmpty()) {
            throw OcrException(OcrErrorCode.AUTHENTICATION_ERROR, "API key is missing")
        }

        try {
            // Check for cancellation
            currentCoroutineContext().ensureActive()

            // Encode to base64 JPEG
            val base64Image = bitmapToBase64(bitmap)
            currentCoroutineContext().ensureActive()

            // Call GCP Vision API
            val responseJson = callGcpVisionApi(
                base64Image,
                currentApiKey,
                language ?: "ja",
                useDocumentTextDetection
            )
            currentCoroutineContext().ensureActive()

            // Parse and return result
            return parseApiResponse(responseJson, bitmap.width, bitmap.height, textDirection)

        } catch (e: GcpApiException) {
            Log.e(TAG, "GCP API error (${e.errorCode}): ${e.message}")
            throw OcrException(e.errorCode, e.message ?: "GCP API error")
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            throw OcrException(OcrErrorCode.TIMEOUT, "Request timed out")
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            throw OcrException(OcrErrorCode.NETWORK_ERROR, "Network error: ${e.message}")
        }
    }

    override fun onShutdown() {
        apiKey = null
        language = null
        useDocumentTextDetection = true
    }

    // -------------------------------------------------------------------------
    // Image helpers
    // -------------------------------------------------------------------------

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // -------------------------------------------------------------------------
    // Network
    // -------------------------------------------------------------------------

    /**
     * Sends the image to the GCP Vision API and returns the parsed JSON response.
     *
     * @throws GcpApiException for API-level or HTTP-level errors.
     * @throws IOException for network failures.
     */
    private fun callGcpVisionApi(
        base64Image: String,
        apiKey: String,
        language: String,
        useDocumentTextDetection: Boolean,
    ): JSONObject {
        val featureType = if (useDocumentTextDetection) "DOCUMENT_TEXT_DETECTION" else "TEXT_DETECTION"
        val url = URL("https://vision.googleapis.com/v1/images:annotate?key=$apiKey")
        val requestBody = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", JSONObject().apply { put("content", base64Image) })
                    put("features", JSONArray().apply {
                        put(JSONObject().apply { put("type", featureType) })
                    })
                    put("imageContext", JSONObject().apply {
                        put("languageHints", JSONArray(listOf(language)))
                    })
                })
            })
        }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10_000
            readTimeout = 25_000
            doOutput = true
        }

        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

        val responseCode = connection.responseCode
        val responseBody = try {
            InputStreamReader(
                if (responseCode == HttpURLConnection.HTTP_OK) connection.inputStream
                else connection.errorStream
            ).readText()
        } catch (e: IOException) {
            throw GcpApiException(OcrErrorCode.NETWORK_ERROR, "Failed to read response: ${e.message}")
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorMessage = try {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    ?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            } ?: "HTTP $responseCode: $responseBody"
            throw GcpApiException(mapHttpError(responseCode, responseBody), errorMessage)
        }

        return JSONObject(responseBody)
    }

    private fun mapHttpError(httpCode: Int, body: String): Int {
        // Prefer the structured error code from the response body when available
        val apiCode = try {
            JSONObject(body).optJSONObject("error")?.optInt("code", 0) ?: 0
        } catch (_: Exception) {
            0
        }
        return when {
            httpCode == 400 -> OcrErrorCode.INVALID_ARGUMENT
            httpCode == 401 || httpCode == 403 -> OcrErrorCode.AUTHENTICATION_ERROR
            httpCode == 408 -> OcrErrorCode.TIMEOUT
            httpCode == 429 || apiCode == 429 -> OcrErrorCode.QUOTA_EXCEEDED
            httpCode == 503 -> OcrErrorCode.SERVICE_DISABLED
            httpCode >= 500 -> OcrErrorCode.INTERNAL_ERROR
            else -> OcrErrorCode.NETWORK_ERROR
        }
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    /**
     * Parses the GCP Vision API JSON response into a list of [OcrBlock].
     *
     * Walks the response → fullTextAnnotation → pages → blocks → paragraphs → words → symbols
     * hierarchy, mirroring the structure expected by [resolveTextDirections].
     */
    private fun parseApiResponse(
        responseJson: JSONObject,
        fallbackWidth: Int,
        fallbackHeight: Int,
        textDirection: String?,
    ): List<OcrBlock> {
        val response0 = responseJson.optJSONArray("responses")?.optJSONObject(0)
            ?: return emptyList()

        if (response0.has("error")) {
            val errMsg = response0.getJSONObject("error").optString("message", "Unknown API error")
            throw GcpApiException(OcrErrorCode.INTERNAL_ERROR, errMsg)
        }

        val page = response0
            .optJSONObject("fullTextAnnotation")
            ?.optJSONArray("pages")
            ?.optJSONObject(0)
            ?: return emptyList()

        val pageWidth = page.optInt("width", fallbackWidth).takeIf { it > 0 } ?: return emptyList()
        val pageHeight = page.optInt("height", fallbackHeight).takeIf { it > 0 } ?: return emptyList()

        val normX = 1.0f / pageWidth
        val normY = 1.0f / pageHeight

        val parsedBlocks = parseBlocks(page.optJSONArray("blocks"), normX, normY)
        return resolveTextDirections(parsedBlocks, textDirection)
    }

    /**
     * Flattens the GCP Vision API block/paragraph/word/symbol hierarchy into a list of
     * [ParsedBlock] values.
     *
     * Each block in the response maps to one [ParsedBlock]. Text is built by concatenating
     * word-level characters; words with no surviving symbols (e.g. those filtered out due to
     * invalid symbol bounds) are omitted entirely, and blocks that produce empty text after
     * trimming are dropped.
     *
     * @param blocksArray The `blocks` JSON array from a single GCP Vision page object, or null.
     * @param normX Multiplier to convert absolute x pixel coordinates to [0, 1] space.
     * @param normY Multiplier to convert absolute y pixel coordinates to [0, 1] space.
     */
    private fun parseBlocks(blocksArray: JSONArray?, normX: Float, normY: Float): List<ParsedBlock> {
        blocksArray ?: return emptyList()
        val parsedBlocks = mutableListOf<ParsedBlock>()

        for (i in 0 until blocksArray.length()) {
            val block = blocksArray.optJSONObject(i) ?: continue
            val blockText = StringBuilder()
            val symbolBounds = mutableListOf<RectF>()

            val paragraphs = block.optJSONArray("paragraphs") ?: continue
            for (j in 0 until paragraphs.length()) {
                val paragraph = paragraphs.optJSONObject(j) ?: continue
                val words = paragraph.optJSONArray("words") ?: continue

                for (k in 0 until words.length()) {
                    val word = words.optJSONObject(k) ?: continue
                    val wordText = StringBuilder()
                    val symbols = word.optJSONArray("symbols") ?: continue

                    for (l in 0 until symbols.length()) {
                        val symbol = symbols.optJSONObject(l) ?: continue
                        val text = symbol.optString("text")
                        if (text.isEmpty()) continue
                        val rect = boundingBoxToRectF(symbol.optJSONObject("boundingBox"), normX, normY)
                        if (!rect.isEmpty) {
                            wordText.append(text)
                            symbolBounds.add(rect)
                        }
                    }

                    if (wordText.isNotEmpty()) blockText.append(wordText)
                }
            }

            val finalText = blockText.toString().trim()
            if (finalText.isNotEmpty()) {
                parsedBlocks.add(ParsedBlock(finalText, symbolBounds))
            }
        }
        return parsedBlocks
    }

    /**
     * Converts a GCP Vision API boundingBox JSON object to a normalized [RectF].
     *
     * The vertices are in the order of top-left, top-right, bottom-right, bottom-left,
     * but relative to the character's orientation. In other words it may be:
     * 0----1      1----2      2----3      3----0
     * |    |  or  |    |  or  |    |  or  |    |
     * 3----2      0----3      1----0      2----1
     * We throw away all rotated symbols. Because we usually capture right-side-up text,
     * the occasional rotated symbol is very likely to be an OCR error (like recognizing
     * "ω" as a rotated "3").
     */
    private fun boundingBoxToRectF(boundingBox: JSONObject?, normX: Float, normY: Float): RectF {
        boundingBox ?: return RectF()
        return try {
            val vertices = boundingBox.optJSONArray("vertices") ?: return RectF()
            if (vertices.length() < 4) return RectF()

            val v0 = vertices.getJSONObject(0) // top-left
            val v1 = vertices.getJSONObject(1) // top-right
            val v2 = vertices.getJSONObject(2) // bottom-right

            val left = v0.optDouble("x", 0.0).toFloat() * normX
            val top = v0.optDouble("y", 0.0).toFloat() * normY
            val right = v1.optDouble("x", 0.0).toFloat() * normX
            val bottom = v2.optDouble("y", 0.0).toFloat() * normY

            // Note: For rotated characters (cases 2-4 above), the constructed RectF will have
            // left >= right or top >= bottom, causing isEmpty() to return true.
            // Therefore the caller's isEmpty() check filters both malformed data AND rotated symbols.

            RectF(left, top, right, bottom)
        } catch (_: Exception) {
            RectF()
        }
    }

    // -------------------------------------------------------------------------
    // Text direction resolution
    // -------------------------------------------------------------------------

    /**
     * The orientation of a single parsed block, as determined geometrically or assigned
     * from the caller's explicit request.
     */
    private enum class BlockOrientation { HORIZONTAL, VERTICAL, INCONCLUSIVE }

    /**
     * Assigns a text direction to each [ParsedBlock] and returns the final list of [OcrBlock].
     *
     * When a fixed direction is requested ("horizontal" or "vertical"), it is applied uniformly
     * to all blocks with no detection or resolution step.
     *
     * If textDirection is "auto", we determine the orientation of each block based on the geometric
     * order of the symbols as returned by the Vision API. I.e. if the symbols are ordered
     * left-to-right then the block is horizontal, and if the symbols are ordered top-to-bottom then
     * the block is vertical.
     *
     * However, some blocks will be ambiguous, such as blocks with only one character. For those
     * cases we run a second pass to heuristically determine the orientation based on broader context.
     *
     * - **First pass** - each block gets an initial orientation via [detectOrientation]; ambiguous
     *   blocks yield [BlockOrientation.INCONCLUSIVE].
     * - **Second pass** - INCONCLUSIVE blocks are resolved by adopting their neighbors' orientation
     *   when both agree, falling back to horizontal otherwise.
     */
    private fun resolveTextDirections(parsedBlocks: List<ParsedBlock>, textDirection: String?): List<OcrBlock> {
        val fixedDirection = when (textDirection?.lowercase()) {
            "horizontal" -> BlockOrientation.HORIZONTAL
            "vertical" -> BlockOrientation.VERTICAL
            else -> null // AUTO
        }

        if (fixedDirection == BlockOrientation.HORIZONTAL || fixedDirection == BlockOrientation.VERTICAL) {
            return parsedBlocks.map { block ->
                OcrBlock(block.text, block.symbolBounds, isVertical = fixedDirection == BlockOrientation.VERTICAL)
            }
        }

        // Auto path: detect orientation per block, then resolve inconclusive blocks.

        // First pass: detect an initial orientation for every block.
        val initialOrientations: List<BlockOrientation> = parsedBlocks.map { block ->
            detectOrientation(block.symbolBounds)
        }

        // Second pass: resolve INCONCLUSIVE blocks via neighbor consensus.
        val ocrBlocks = initialOrientations.mapIndexed { i, orientation ->
            val resolvedOrientation = if (orientation != BlockOrientation.INCONCLUSIVE) {
                orientation
            } else {
                val prev = if (i > 0) initialOrientations[i - 1] else BlockOrientation.INCONCLUSIVE
                val next =
                    if (i < initialOrientations.size - 1) initialOrientations[i + 1] else BlockOrientation.INCONCLUSIVE
                val resolved = when {
                    prev == next && prev != BlockOrientation.INCONCLUSIVE -> prev
                    else -> BlockOrientation.HORIZONTAL // fallback
                }
                Log.d(TAG, "Block $i was INCONCLUSIVE. Resolved to $resolved (neighbors: $prev, $next)")
                resolved
            }
            OcrBlock(
                parsedBlocks[i].text,
                parsedBlocks[i].symbolBounds,
                isVertical = resolvedOrientation == BlockOrientation.VERTICAL
            )
        }

        return ocrBlocks
    }

    /**
     * Heuristically determines the orientation of a block's symbols. This assumes the Vision API has
     * already correctly detected the orientation and returned the symbols in left-to-right order for
     * horizontal text and top-to-bottom/right-to-left order for vertical text.
     *
     * Compares the span of symbol centers along each axis. Returns [BlockOrientation.INCONCLUSIVE]
     * when the evidence is ambiguous (too few symbols, or a near-square arrangement).
     */
    private fun detectOrientation(symbolBounds: List<RectF>): BlockOrientation {
        if (symbolBounds.size < 2) return BlockOrientation.INCONCLUSIVE

        val xSpan = symbolBounds.maxOf { it.centerX() } - symbolBounds.minOf { it.centerX() }
        val ySpan = symbolBounds.maxOf { it.centerY() } - symbolBounds.minOf { it.centerY() }

        val total = xSpan + ySpan
        if (total < 0.01f) return BlockOrientation.INCONCLUSIVE // symbols overlapping, indeterminate

        return when {
            ySpan / total > 0.65f -> BlockOrientation.VERTICAL
            xSpan / total > 0.65f -> BlockOrientation.HORIZONTAL
            else -> BlockOrientation.INCONCLUSIVE
        }
    }

    // -------------------------------------------------------------------------
    // Internal types
    // -------------------------------------------------------------------------

    /**
     * User-facing configuration keys defined in [configSchema].
     */
    private object ConfigKey {
        const val API_KEY = "api_key"
        const val DETECTION_MODE = "detection_mode"
    }

    /** Wraps a GCP API error with a mapped [OcrErrorCode] for structured propagation. */
    private class GcpApiException(val errorCode: Int, message: String?) : Exception(message)

    companion object {
        private const val TAG = "GcpOcrPluginService"
    }
}
