# Google Translate Dictionary Plugin Sample

A cloud-based translation plugin for Dokuen Reader using Google Cloud Translation API (NMT) with
"Bring Your Own Key" (BYOK).

## Features

This sample demonstrates:

- **Cloud-based translation** - Uses Google Cloud Translation API (Neural Machine Translation)
- **BYOK model** - Users provide their own GCP API key
- **Multiple target languages** - Supports 30+ common languages (expandable to 100+)
- **Network-based lookup** - REQUIRES_INTERNET = true
- **Handles inflected forms** - No deinflection needed (REQUIRES_DICTIONARY_FORM = false)
- **Error handling** - Comprehensive network and API error handling
- **Configuration validation** - API key format validation

## Requirements

- Android SDK 29+ (Android 10+)
- Kotlin 1.8+
- Internet connection
- Google Cloud Platform account with Cloud Translation API enabled
- GCP API key

## Setup

### 1. Get a GCP API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Cloud Translation API**
4. Create an API key (Credentials → Create Credentials → API Key)
5. Restrict the key to Cloud Translation API for security

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
4. Go to Settings → Dictionary → Translator Source
5. Select "Google Translate BYOK"
6. Enter your GCP API key in the configuration
7. Select your target language

## How It Works

### Plugin Metadata

The plugin declares required metadata and intent categories in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
    <category android:name="io.github.dokuendev.dokuenreader.category.TRANSLATOR" />
</intent-filter>

<meta-data android:name="plugin_name" android:value="Google Translate (sample)" />
<meta-data android:name="plugin_version" android:value="1.0.0" />
<meta-data android:name="plugin_author" android:value="Dokuen Reader" />
<meta-data android:name="plugin_description" android:value="Sample translator plugin using the Google Cloud Translation API" />
<meta-data android:name="plugin_license" android:value="Apache 2.0" />
```

### Capabilities

The plugin reports its capabilities to Dokuen:

- **HANDLES_SEGMENTATION**: false - Uses host segmentation
- **REQUIRES_DICTIONARY_FORM**: false - Can translate inflected forms
- **REQUIRES_INTERNET**: true - Needs network connection
- **SUPPORTED_SOURCE_LANGUAGES**: Japanese (`ja`)
- **SUPPORTED_TARGET_LANGUAGES**: 30+ languages (English, Spanish, French, German, Chinese, Korean,
  etc.)

### Processing Flow

1. **Initialization** (`onInitialize`)
    - Validates API key is provided
    - Reads target language from host config
    - Stores configuration for lookup requests

2. **Translation** (`onLookup`)
    - Receives word or phrase from host app
    - Calls Google Cloud Translation API with source and target languages
    - Parses JSON response
    - Returns formatted dictionary entry with translation

3. **Shutdown** (`onShutdown`)
    - Clears API key and configuration
    - Releases resources

### API Used: Cloud Translation - Basic (NMT)

This plugin uses the Cloud Translation - Basic API with Neural Machine Translation:

- **Endpoint**: `https://translation.googleapis.com/language/translate/v2`
- **Method**: POST
- **Cost**: $20 per 1M characters (first 500K free per month)
- **Quality**: High-quality neural translations
- **Speed**: Fast (typically < 1 second)
- **Authentication**: API key only

**Request format:**

```json
{
  "q": "食べる",
  "source": "ja",
  "target": "en",
  "format": "text"
}
```

**Response format:**

```json
{
  "data": {
    "translations": [
      {
        "translatedText": "to eat",
        "detectedSourceLanguage": "ja"
      }
    ]
  }
}
```

## Alternative: Cloud Translation - Advanced (Translation LLM)

For higher quality translations, Google offers Cloud Translation - Advanced which includes a
Translation LLM (TLLM) model in addition to the standard NMT model.

### What is Cloud Translation - Advanced?

Cloud Translation - Advanced provides access to:

- **Translation LLM (TLLM)** - Large language model optimized specifically for translation
- **Adaptive Translation** - Customization using limited example translation pairs
- **Glossary support** - Custom terminology for domain-specific translations
- **Batch translation** - Translate entire documents
- **Model selection** - Choose between NMT and TLLM models
- **Document context** - Better handling of document-level context

### Key Differences: Basic vs Advanced

| Feature                  | Basic (NMT)    | Advanced (NMT)               | Advanced (TLLM)              |
|--------------------------|----------------|------------------------------|------------------------------|
| **Quality**              | High           | High                         | Highest (LLM-based)          |
| **Cost**                 | $20/1M chars   | $20/1M chars                 | $80/1M chars                 |
| **Authentication**       | API key        | OAuth 2.0 or service account | OAuth 2.0 or service account |
| **Setup complexity**     | Simple         | More complex                 | More complex                 |
| **Speed**                | < 1 second     | < 1 second                   | 1-2 seconds                  |
| **Context awareness**    | Sentence-level | Sentence-level               | Document-level               |
| **Glossaries**           | No             | Yes                          | Yes                          |
| **Adaptive Translation** | No             | No                           | Yes                          |

### Example Advanced API Implementation

```kotlin
// Endpoint: https://translation.googleapis.com/v3/projects/{projectId}/locations/global:translateText

val requestBody = JSONObject().apply {
    put("contents", JSONArray().apply { put(text) })
    put("sourceLanguageCode", sourceLang)
    put("targetLanguageCode", targetLang)
    put("mimeType", "text/plain")
    // Optional: specify TLLM model for highest quality
    put("model", "projects/{projectId}/locations/global/models/general/tllm")
}

// Note: Requires OAuth 2.0 authentication, not just API key
// Authorization: Bearer {access_token}
```

### When to Use Advanced API

Use Advanced API when you need:

- Highest translation quality (TLLM model)
- Domain-specific terminology (with glossaries)
- Adaptive Translation with custom examples
- Document-level context
- Batch translation of documents

Use Basic API (this sample) when you need:

- Simple API key authentication
- Lower cost
- Faster response times
- Good quality for general translations

### Migration Path

To upgrade this sample to Advanced API:

1. **Change authentication** - Use OAuth 2.0 or service account instead of API key
2. **Update endpoint** - Use v3 endpoint with project ID and location
3. **Modify request format** - Use v3 request structure
4. **Handle response** - Parse v3 response format
5. **Select model** - Choose between NMT or TLLM model
6. **Add glossary support** (optional) - Define custom terminology

See the [Cloud Translation API documentation](https://cloud.google.com/translate/docs/overview) for
details.

## Note: Using Gemini Directly (Not Recommended)

You could also use Gemini API directly for translation tasks, but this is **NOT recommended**:

- ❌ Requires complex prompt engineering
- ❌ Inconsistent output format requiring parsing
- ❌ Higher latency
- ❌ More expensive
- ❌ Not optimized for translation

**Use Cloud Translation - Advanced with TLLM instead** - it provides LLM-quality translations
through a proper translation API without the drawbacks of using a general-purpose LLM.

## Supported Target Languages

The plugin supports 30+ common languages (expandable to 100+):

- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Italian (it)
- Portuguese (pt)
- Russian (ru)
- Chinese Simplified (zh-CN)
- Chinese Traditional (zh-TW)
- Korean (ko)
- Arabic (ar)
- Hindi (hi)
- Thai (th)
- Vietnamese (vi)
- Indonesian (id)
- Dutch (nl)
- Polish (pl)
- Turkish (tr)
- Swedish (sv)
- Norwegian (no)
- Danish (da)
- Finnish (fi)
- Czech (cs)
- Romanian (ro)
- Hungarian (hu)
- Greek (el)
- Hebrew (he)
- Ukrainian (uk)
- Malay (ms)
- Persian (fa)

**Note**: This list can be expanded to include any of the 100+ languages supported by Google
Translate. See the [full list](https://cloud.google.com/translate/docs/languages).

## Code Structure

```
app/src/main/java/.../googletranslate/
└── GoogleTranslateDictionaryPluginService.kt

Key methods:
- capabilities                          # Declare plugin capabilities
- configSchema                          # Define API key configuration
- onInitialize()                       # Validate API key and read config
- onLookup()                           # Translate word via API
- callTranslateApi()                   # HTTP request to GCP
- parseTranslationResponse()           # Parse JSON response
- createTranslationEntry()             # Format as DictionaryEntry
```

## Error Handling

The plugin handles various error conditions:

- **Authentication errors** (401/403) - Invalid or missing API key
- **Quota exceeded** (429) - API rate limit or usage quota exceeded
- **Network errors** - Connection failures, timeouts
- **Invalid arguments** (400) - Malformed request
- **Service unavailable** (503) - GCP service temporarily down
- **Internal errors** (500+) - GCP server errors

All errors are mapped to appropriate `DictionaryErrorCode` values and displayed to users with
helpful messages.

## Performance

Typical performance with good network connection:

- **Initialization**: < 1 second
- **Translation**: 0.5-2 seconds including network latency
- **Memory usage**: ~10 MB during processing

## Privacy & Security

- **BYOK model** - Users provide their own API key, maintaining full control
- **No data retention** - Translations are processed by GCP and not stored (per GCP's data
  processing terms)
- **API key security** - Keys are stored locally on device, never shared
- **Recommendation** - Restrict your API key to Cloud Translation API only in GCP Console

## Costs

Google Cloud Translation API pricing (as of 2026):

- **NMT**: $20 per 1 million characters
- **First 500,000 characters per month**: Free

For typical usage (looking up 10-20 words per day):

- ~500 characters per day
- ~15,000 characters per month
- **Cost**: Free (within free tier)

See [GCP Translation Pricing](https://cloud.google.com/translate/pricing) for current rates.

## Limitations

- **Requires internet** - Cannot work offline
- **Network latency** - Slower than on-device dictionaries
- **API costs** - Users pay for API usage (though minimal for typical use)
- **Translation quality** - May not match specialized dictionaries for technical terms
- **No context** - Translates individual words without surrounding context (use LLM for
  context-aware translation)

## Extending This Sample

To enhance this plugin:

1. **Add caching** - Cache frequently translated words locally
2. **Batch requests** - Translate multiple words in one API call
3. **Context support** - Send surrounding text for better translations
4. **Offline fallback** - Use local dictionary when network unavailable
5. **LLM integration** - Switch to Gemini API for context-aware translations
6. **Usage tracking** - Show users their API usage and costs
7. **Language detection** - Auto-detect source language

## License

Apache 2.0 - See LICENSE file for details

## Disclaimer

This is a sample plugin for demonstration purposes. Users are responsible for their own GCP API
usage and costs.
