# Google Cloud Vision BYOK OCR Plugin Sample

A fully functional sample OCR plugin for Dokuen Reader that demonstrates cloud-based text recognition using Google Cloud Vision API with "Bring Your Own Key" (BYOK).

## Features

This sample demonstrates:

- **Network-based OCR** - Shows how to integrate a cloud API instead of on-device models
- **Required configuration** - Demonstrates user-provided API key validation
- **Multiple config fields** - Shows both required (API key) and optional (detection mode) configuration
- **Error mapping** - Maps HTTP and API error codes to appropriate `OcrErrorCode` values, including network errors and timeouts
- **JSON parsing** - Parses GCP Vision API's nested block/paragraph/word/symbol structure
- **Text direction interpretation** - Infers whether GCP detected text as horizontal or vertical by analyzing returned symbol geometry
- **Language hints** - Passes language preferences to the API for better accuracy

## Requirements

- Android SDK 29+ (Android 10+)
- Kotlin 1.8+
- Internet connection
- Google Cloud Platform account with Cloud Vision API enabled
- GCP API key

## Setup

### 1. Get a GCP API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Cloud Vision API
4. Create an API key (Credentials → Create Credentials → API Key)
5. Restrict the key to Cloud Vision API for security

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
4. Go to Settings → Scan Mode → Custom
5. Enable "Google Vision BYOK" plugin
6. Enter your GCP API key in the configuration
7. Choose detection mode (DOCUMENT_TEXT_DETECTION recommended)

## How It Works

### Plugin Metadata

The plugin declares required metadata in `AndroidManifest.xml`:

```xml
<meta-data android:name="plugin_name" android:value="Google Vision BYOK" />
<meta-data android:name="plugin_version" android:value="1.0.0" />
<meta-data android:name="plugin_author" android:value="Dokuen Reader" />
<meta-data android:name="plugin_description" android:value="Google Cloud Vision OCR with 'Bring Your Own Key'" />
<meta-data android:name="plugin_license" android:value="Apache 2.0" />
```

### Capabilities

The plugin reports its capabilities to Dokuen:

- **Supports vertical text**: Yes
- **Supports auto text direction**: Yes (infers from symbol geometry)
- **Requires internet**: Yes (cloud API)
- **Supported languages**: Japanese (`ja`), Chinese (`zh`)
- **Has custom config**: Yes (API key and detection mode)

### Configuration

Users must configure:

- **API Key** (required) - Your GCP API key with Cloud Vision API enabled
- **Text Detection Mode** (optional) - Choose between:
  - `TEXT_DETECTION` - For sparse text (signs, labels, UI elements)
  - `DOCUMENT_TEXT_DETECTION` - For dense text (books, documents, handwriting) - **Recommended**

### Processing Flow

1. **Initialization** (`onInitialize`)
   - Validates API key is provided
   - Reads language preference from host app
   - Reads detection mode from user config
   - Returns preprocessing requirements (grayscale conversion)

2. **Image Processing** (`onProcessImage`)
   - Converts bitmap to JPEG and base64 encodes it
   - Sends request to GCP Vision API with language hints
   - Parses JSON response into blocks/paragraphs/words/symbols
   - Automatically detects text direction per block (if "auto" requested)
   - Returns list of `OcrBlock` objects with normalized bounding boxes

3. **Shutdown** (`onShutdown`)
   - Clears API key and configuration
   - Releases resources

### Automatic Text Direction Detection

When the user selects "auto" text direction, the plugin interprets the text direction that the Vision API detected.

**How it works:**
- GCP Vision API detects text orientation and returns symbols in reading order
- For horizontal text: symbols are ordered left-to-right
- For vertical text: symbols are ordered top-to-bottom, right-to-left
- The plugin infers which orientation GCP detected by analyzing the geometry of returned symbols

**Inference algorithm:**
1. **Geometric analysis** - Compares horizontal vs vertical span of symbol centers
2. **Orientation classification**:
   - If neighboring symbols are left and right of each other → GCP detected HORIZONTAL text
   - If neighboring symbols are above and below each other → GCP detected VERTICAL text
   - Otherwise → INCONCLUSIVE (ambiguous, e.g., single character)
3. **Neighbor consensus** - Inconclusive blocks adopt their neighbors' orientation when both agree
4. **Fallback** - Remaining inconclusive blocks default to horizontal

This produces accurate mixed-orientation results for documents with both horizontal and vertical text.

## Code Structure

```
app/src/main/java/.../gcp/
└── GcpOcrPluginService.kt      # Main plugin implementation

Key methods:
- onInitialize()                 # Validate API key and config
- onProcessImage()               # Call GCP API and parse results
- onShutdown()                   # Cleanup
- callGcpVisionApi()             # Network request to GCP
- parseApiResponse()             # Parse JSON response
- resolveTextDirections()        # Auto-detect text orientation
- detectOrientation()            # Geometric orientation analysis
```

## Error Handling

The plugin handles various error conditions:

- **Authentication errors** (401/403) - Invalid or missing API key
- **Quota exceeded** (429) - API rate limit or usage quota exceeded
- **Network errors** - Connection failures, timeouts
- **Invalid arguments** (400) - Malformed request
- **Service unavailable** (503) - GCP service temporarily down
- **Internal errors** (500+) - GCP server errors

All errors are mapped to appropriate `OcrErrorCode` values and displayed to users with helpful messages.

## Testing

The sample includes comprehensive tests:

### Unit Tests (`src/test/`)

- `GcpOcrPluginServiceTest.kt` - Tests plugin lifecycle, configuration, API request building, response parsing, and text direction detection using Robolectric

Run unit tests:
```bash
./gradlew test
```

### Instrumented Tests (`src/androidTest/`)

- `GcpOcrPluginServiceIntegrationTest.kt` - Tests actual GCP API integration on device (requires valid API key)

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Performance

Typical performance with good network connection:

- **Initialization**: < 1 second
- **Processing**: 2-5 seconds including network latency
- **Memory usage**: ~30 MB during processing

## Privacy & Security

- **BYOK model** - Users provide their own API key, maintaining full control
- **No data retention** - Images are processed by GCP and not stored (per GCP's data processing terms)
- **API key security** - Keys are stored locally on device, never shared
- **Recommendation** - Restrict your API key to Cloud Vision API only in GCP Console

## Limitations

- **Requires internet** - Cannot work offline
- **Network latency** - Slower than on-device processing
- **API costs** - Users pay for API usage (though minimal for typical use)
- **Rotated text** - Rotated symbols are filtered out as they're often OCR errors

## License

Apache 2.0 - See LICENSE file for details

## Disclaimer

This is a sample plugin for demonstration purposes. Users are responsible for their own GCP API usage and costs.
