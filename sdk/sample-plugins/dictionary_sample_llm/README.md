# LLM Dictionary Plugin Sample

A context-aware AI dictionary plugin for Dokuen Reader that generates definitions using modern
Large Language Models (Gemini, Claude, or ChatGPT) with Structured Outputs.

## Features

This sample demonstrates:

- **Multi-provider LLM support** - Choose between Gemini, Claude, or ChatGPT
- **Context-aware definitions** - Full surrounding text is sent to the LLM for disambiguation
- **Structured Outputs** - Each provider uses its native structured-output mechanism (Gemini
  `responseSchema`, ChatGPT `json_schema`, Claude tool-use) to guarantee consistent JSON responses
- **Handles segmentation** - Plugin receives full OCR block text with cursor indices
  (HANDLES_SEGMENTATION = true)
- **Multi-language support** - Japanese source language and 11 target languages
- **BYOK model** - Users provide their own API key for their chosen provider
- **Accurate furigana** - Bidirectional kana-stripping algorithm to produce correct `RubySpan`
  annotations for Japanese headwords
- **Theme-aware styling** - Colors adapt to dark and light mode via `UI_THEME`
- **Formatted error cards** - Lookup failures are rendered as styled, actionable error entries
  inside the dictionary dialog (see [Error Handling](#error-handling))

## Requirements

- Android SDK 29+ (Android 10+)
- Kotlin 1.8+
- Internet connection
- An API key for at least one of the supported providers

## Setup

### 1. Get an API Key

Choose a provider and obtain a key:

| Provider | Where to get a key                                              | Default model          |
|----------|-----------------------------------------------------------------|------------------------|
| Gemini   | [Google AI Studio](https://aistudio.google.com/apikey)          | `gemini-flash-latest`  |
| Claude   | [Anthropic Console](https://console.anthropic.com/)             | `claude-sonnet-4-6`    |
| ChatGPT  | [OpenAI Platform](https://platform.openai.com/account/api-keys) | `gpt-5.1-mini`         |

Providers retire model IDs on their own schedule, independent of this plugin's release cycle. If a
default above stops working, set the `model` config field (see [Configuration](#configuration)) to the
provider's current model ID instead.

### 2. Install the Plugin

#### For Plugin Developers

1. Clone this repository
2. Open the project in Android Studio
3. Build and install the APK:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

#### For End Users

1. Download the APK from the releases page
2. Install it on your Android device
3. Open Dokuen Reader
4. Go to Settings → Dictionary → Dictionary Source
5. Select "AI Dictionary (sample)"
6. Choose your LLM provider from the dropdown
7. Enter your API key

## How It Works

### Plugin Metadata

The plugin declares required metadata in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
    <category android:name="io.github.dokuendev.dokuenreader.category.DICTIONARY" />
</intent-filter>

<meta-data android:name="plugin_name" android:value="AI Dictionary (sample)" />
<meta-data android:name="plugin_version" android:value="1.0.0" />
<meta-data android:name="plugin_author" android:value="Dokuen Reader" />
<meta-data android:name="plugin_description"
    android:value="AI dictionary. Generates definitions using Gemini, Claude, or ChatGPT." />
<meta-data android:name="plugin_license" android:value="Apache 2.0" />
```

### Capabilities

The plugin reports its capabilities to Dokuen:

- **HANDLES_SEGMENTATION**: true - Plugin receives the full OCR block text with cursor indices,
  enabling context-aware lookups
- **REQUIRES_DICTIONARY_FORM**: false - Plugin handles raw, inflected text directly
- **REQUIRES_INTERNET**: true - Needs network connection to call LLM APIs
- **SUPPORTED_SOURCE_LANGUAGES**: `ja`
- **SUPPORTED_TARGET_LANGUAGES**: `en`, `es`, `fr`, `de`, `it`, `pt`, `ru`, `zh-CN`, `zh-TW`, `ko`,
  `ja`

### Configuration

The plugin declares three user-facing configuration fields via `configSchema`:

| Field      | Type | Required | Description                                                                                           |
|------------|------|----------|-------------------------------------------------------------------------------------------------------|
| `provider` | ENUM | Yes      | LLM provider: Gemini, Claude, or ChatGPT                                                              |
| `api_key`  | TEXT | Yes      | API key for the selected provider                                                                     |
| `model`    | TEXT | No       | Optional override of the model ID for the selected provider. Leave blank to use the built-in default. |

These appear as interactive controls in the Dokuen plugin settings UI. `model` exists specifically
so that a provider retiring a model ID doesn't break the plugin for existing users. They can just
update this field instead of waiting for a new release.

### Processing Flow

1. **Initialization** (`onInitialize`)
    - Validates that an API key is provided
    - Reads user-selected provider from config
    - Resolves the model ID: uses the user's `model` override if set, otherwise falls back to the
      per-provider default in `DEFAULT_MODELS`
    - Reads host config: source language, target language, UI theme
    - Stores configuration for subsequent lookups

2. **Lookup** (`onLookup`)
    - Receives full OCR block text and cursor range (because `HANDLES_SEGMENTATION = true`)
    - Extracts the query word from `contextText[cursorStartIndex, cursorEndIndex)`
    - Constructs a system prompt and user prompt including the full context sentence
    - Calls the selected LLM API with Structured Outputs to guarantee a consistent JSON schema
    - Parses the JSON response into `LlmDefinition` objects
    - Renders each definition as a styled `DictionaryEntry` with part-of-speech tags, examples,
      and furigana

3. **Shutdown** (`onShutdown`)
    - Clears the API key from memory
    - Releases resources

### Structured Outputs

All three providers are configured to return a unified JSON schema:

```json
{
  "definitions": [
    {
      "partOfSpeech": "verb",
      "definition": "to eat; to have a meal",
      "pronunciation": "たべる",
      "examples": [
        "毎朝パンを食べる。",
        "昨日レストランで食べた。"
      ]
    }
  ]
}
```

Each provider enforces this schema through its native mechanism:

| Provider | Mechanism                        | Details                                                                 |
|----------|----------------------------------|-------------------------------------------------------------------------|
| Gemini   | `responseSchema` in config       | Uses uppercase type names (`OBJECT`, `STRING`, `ARRAY`)                 |
| ChatGPT  | `response_format.json_schema`    | Uses lowercase types with `strict: true`, `additionalProperties: false` |
| Claude   | Forced tool call (`tool_choice`) | Defines a `define_word` tool whose `input_schema` is the schema         |

### Provider API Details

#### Gemini

- **Endpoint**:
  `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- **Auth**: API key as query parameter
- **Model**: User-configurable via the `model` config field; defaults to `gemini-flash-latest`
- **Structured Output**: `generationConfig.responseMimeType = "application/json"` +
  `generationConfig.responseSchema`

#### ChatGPT

- **Endpoint**: `https://api.openai.com/v1/chat/completions`
- **Auth**: `Authorization: Bearer {key}`
- **Model**: User-configurable via the `model` config field; defaults to `gpt-5.1-mini`
- **Structured Output**: `response_format.type = "json_schema"` with `strict: true`

#### Claude

- **Endpoint**: `https://api.anthropic.com/v1/messages`
- **Auth**: `x-api-key: {key}` + `anthropic-version: 2023-06-01`
- **Model**: User-configurable via the `model` config field; defaults to `claude-sonnet-4-6`
- **Structured Output**: Forced tool call. Defines a `define_word` tool and sets
  `tool_choice = { type: "tool", name: "define_word" }`; the LLM's structured `input` JSON is
  extracted from the `tool_use` content block

### Furigana Generation

For Japanese source text, the plugin generates `RubySpan` annotations using a bidirectional
kana-stripping algorithm:

1. Strip matching kana from the prefix of headword and reading (e.g., `お` in `お茶` / `おちゃ`)
2. Strip matching kana from the suffix (e.g., `べる` in `食べる` / `たべる`)
3. Map the remaining reading to the remaining kanji range

This produces accurate per-kanji furigana without requiring a morphological analyzer.

## Code Structure

```
app/src/main/
├── AndroidManifest.xml
└── java/.../llm/
    └── LlmDictionaryPluginService.kt     # Main plugin implementation

Key methods and sections:
- capabilities                             # Declare plugin capabilities
- configSchema                             # Define provider + API key fields
- onInitialize()                           # Validate config and read host settings
- onLookup()                               # Orchestrate LLM call and parse result
- callGeminiApi()                          # Gemini API with responseSchema
- callChatGPTApi()                         # ChatGPT API with json_schema
- callClaudeApi()                          # Claude API with forced tool call
- executePostRequest()                     # Shared HTTP POST with coroutine cancellation check
- buildDefinitionsSchema()                 # Unified JSON schema builder
- parseLlmResponse()                       # Parse JSON into LlmDefinition list
- createEntryFromLlmDefinition()           # Render styled DictionaryEntry
- buildRubySpans()                         # Bidirectional kana-stripping furigana
- createErrorEntry()                       # Theme-aware styled error card
- onShutdown()                             # Clear API key and release resources
```

## Error Handling

> **Important:** This sample uses an _unorthodox_ error-handling strategy for educational purposes.
> All lookup exceptions — authentication errors, rate limits, network timeouts, JSON parsing
> failures — are caught and formatted into a themed `DictionaryEntry` error card that is returned
> as a successful result. This guarantees that detailed, actionable error information is displayed
> directly inside the host's dictionary dialog.
>
> **Third-party plugin authors should NOT replicate this pattern.** If a production plugin catches
> exceptions and wraps them in a successful `DictionaryResult`, the host will not fall back to
> subsequent dictionary or translator engines. Real plugins should propagate errors by throwing
> `DictionaryException` with the appropriate `DictionaryErrorCode`.

### HTTP error mapping

The `executePostRequest()` method maps HTTP status codes to `DictionaryErrorCode` values:

| HTTP Status | Error Code             | Meaning                                             |
|-------------|------------------------|-----------------------------------------------------|
| 401 / 403   | `AUTHENTICATION_ERROR` | Invalid API key or insufficient permission          |
| 400         | `INVALID_ARGUMENT`     | Malformed API request                               |
| 404         | `INVALID_ARGUMENT`     | Model ID not found. Update the `model` config field |
| 429         | `INTERNAL_ERROR`       | Rate limit or quota exceeded                        |
| Other       | `INTERNAL_ERROR`       | Unexpected server error                             |

### Error card rendering

The `createErrorEntry()` method builds a styled error card with four sections:

1. **Title** — Bold, accent-colored header (`[Error] LLM Lookup Failed`)
2. **Message** — The exception's message text
3. **Recommendation** — Context-specific suggestion based on error type
4. **Metadata** — Provider name, language pair, and diagnostic context

All colors are theme-aware, using lighter shades on dark backgrounds and darker shades on light
backgrounds per the SDK color accessibility guidelines.

## Performance

Typical performance with good network connection:

| Metric         | Value                               |
|----------------|-------------------------------------|
| Initialization | < 1 ms (no heavy resources to load) |
| Lookup latency | 1–5 seconds (depends on provider)   |
| Timeout        | 30 seconds (matches host limit)     |
| Memory usage   | Minimal (~5 MB during HTTP call)    |

## Privacy & Security

- **BYOK model** — Users provide their own API key, maintaining full control
- **No data retention** — Text is sent to the selected LLM provider per their data processing terms
- **API key in memory only** — Cleared on `onShutdown()`; never persisted by the plugin
- **Recommendation** — Users should restrict API keys to the minimum necessary scope in their
  provider's dashboard

## Costs

Cost depends on the selected provider. Typical usage (10–20 lookups per day, ~200 tokens per
lookup):

| Provider | Approximate cost per lookup | Monthly estimate (20/day) |
|----------|-----------------------------|---------------------------|
| Gemini   | Free tier available         | Free – minimal            |
| ChatGPT  | ~$0.001                     | ~$0.60                    |
| Claude   | ~$0.002                     | ~$1.20                    |

See each provider's pricing page for current rates.

## Limitations

- **Requires internet** — Cannot work offline
- **Network latency** — Slower than on-device dictionaries or translation APIs
- **API costs** — Users pay for their own API usage
- **LLM variability** — Definitions may vary between calls; quality depends on the model
- **Furigana accuracy** — The kana-stripping algorithm handles most common patterns but may produce
  imperfect results for irregular readings or ateji

## Extending This Sample

To build a production LLM dictionary plugin from this sample:

1. **Auto-validate the model** — The `model` config field already covers the common case of a
   provider retiring an ID (the user updates a setting, no rebuild needed). For a more automated
   fix, query the provider's model-list endpoint (`GET /v1/models` for Claude and ChatGPT,
   `GET /v1beta/models` for Gemini) in `onInitialize()` to confirm the configured model still
   exists, and warn the user proactively instead of waiting for the next failed lookup
2. **Customize the prompt** — Modify `getSystemPrompt()` and `getUserPrompt()` to adjust tone,
   detail level, or output structure
3. **Add caching** — Cache responses for repeated lookups using an LRU cache or local database
4. **Propagate errors properly** — Remove the catch-all in `onLookup()` and let
   `DictionaryException` propagate so the host can fall back to other engines
5. **Add streaming** — Use SSE or streaming endpoints for faster perceived response times
6. **Support more providers** — Add support for additional LLM APIs (Mistral, Llama, etc.)
7. **Enhance furigana** — Use a morphological analyzer (MeCab/Kuromoji) for more accurate readings
8. **Add pronunciation for non-Japanese** — Extend `RubySpan` usage for Chinese pinyin or Korean
   romanization
9. **Remove the disclaimer** — Delete the sample footer text appended to the last entry in
   `createEntryFromLlmDefinition()`

## License

Apache 2.0 — See LICENSE file for details

## Disclaimer

This is a sample plugin for demonstration purposes. Users are responsible for their own API usage
and costs. LLM-generated definitions may contain inaccuracies.
