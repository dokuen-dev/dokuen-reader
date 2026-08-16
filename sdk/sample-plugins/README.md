# Dokuen Reader Plugin Samples

This directory contains sample plugin implementations demonstrating various plugin capabilities for
Dokuen Reader.

## OCR Plugin Samples

### ocr_sample_mlkit

Demonstrates on-device OCR using Google ML Kit.

**Features:**

- On-device text recognition
- No internet required
- Fast processing
- Supports Japanese and Chinese

### ocr_sample_gcp_byok

Demonstrates cloud-based OCR using Google Cloud Vision API with "Bring Your Own Key".

**Features:**

- Cloud-based text recognition
- User-provided API key
- Automatic text direction detection
- High accuracy for complex documents
- Configurable detection modes

## Dictionary Plugin Samples

### dictionary_sample_simple

Demonstrates basic dictionary lookup functionality.

**Features:**

- Simple word-to-definition mapping
- Styled text with bold formatting
- Ruby annotations for pronunciation
- No segmentation (HANDLES_SEGMENTATION = false)
- Requires dictionary form (REQUIRES_DICTIONARY_FORM = true)
- Hardcoded data (no external dependencies)

**Use case:** Basic dictionary plugins with pre-segmented, deinflected input

### dictionary_sample_google_translate

Demonstrates cloud-based translation using Google Cloud Translation API.

**Features:**

- Cloud-based translation with Google Translate NMT API
- BYOK (Bring Your Own Key) model
- HANDLES_SEGMENTATION = true
- 30+ supported target languages (expandable to 100+)
- Comprehensive error handling
- API key configuration and validation

**Use case:** Cloud-based translation plugins with network connectivity

### dictionary_sample_llm

Demonstrates context-aware AI dictionary lookup using LLMs.

**Features:**

- Multi-provider support (Gemini, Claude, ChatGPT)
- BYOK (Bring Your Own Key) model
- Configurable model ID per provider, with an optional user override so a retired
  or renamed model (which providers do on their own schedule) can be fixed from
  the plugin's settings instead of requiring a new build
- HANDLES_SEGMENTATION = true (receives full context sentence)
- Structured Outputs for consistent JSON responses
- Accurate furigana via bidirectional kana-stripping
- Theme-aware styled entries and error cards
- 11 target languages

**Use case:** Context-aware LLM-powered dictionary plugins with network connectivity

## Building the Samples

Each sample is a standalone Android project. To build:

```bash
cd <sample-directory>
./gradlew assembleDebug
```

## Installing the Samples

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Using the Samples

1. Install the sample plugin APK
2. Open Dokuen Reader
3. Go to Settings
4. For OCR plugins: Settings → Scan Mode → Custom → Enable plugin
5. For Dictionary plugins: Settings → Dictionary → Dictionary Source → Select plugin

## Plugin SDK Dependencies

All samples depend on the Plugin SDK modules:

- `plugin_sdk:core` - Core plugin infrastructure
- `plugin_sdk:ocr` - OCR-specific interfaces (for OCR plugins)
- `plugin_sdk:dictionary` - Dictionary-specific interfaces (for Dictionary plugins)

## Documentation

For detailed documentation on creating plugins, see:

- [Plugin SDK README](../README.md)
- [OCR SDK README](../ocr/README.md)
- [Dictionary SDK README](../dictionary/README.md)

## License

All samples are licensed under Apache 2.0. See individual LICENSE files in each sample directory.
