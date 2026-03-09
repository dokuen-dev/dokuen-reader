# ML Kit OCR Plugin Sample

A simple sample OCR plugin for Dokuen Reader that demonstrates on-device text recognition using Google ML Kit.

**Note:** This sample provides identical functionality to Dokuen's built-in "On-Device" Scan Mode and is not intended for production use. It serves as a straightforward example for plugin developers learning the SDK.

## Features

This sample demonstrates:

- **On-device OCR** - Shows how to use a local ML model without network requests
- **Simple configuration** - Demonstrates a single boolean config option (half resolution)
- **Preprocessing requirements** - Shows how to request image preprocessing from the host app
- **Vertical text conversion** - Demonstrates converting horizontal OCR output to vertical reading order when the user specifies vertical direction
- **Symbol-level bounds** - Shows how to extract and align character-level bounding boxes
- **ML Kit integration** - Example of integrating Google's ML Kit text recognition library

## Requirements

- Android SDK 29+ (Android 10+)
- Kotlin 1.8+
- Google ML Kit Text Recognition Japanese library

## Installation

### For Plugin Developers

1. Clone this repository
2. Open the project in Android Studio
3. Build and install the APK:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### For End Users

1. Download the APK from the releases page
2. Install it on your Android device
3. Open Dokuen Reader
4. Go to Settings → Scan Mode → Custom
5. Enable "ML Kit" plugin
6. Configure settings if desired (half resolution option)

## How It Works

### Plugin Metadata

The plugin declares required metadata in `AndroidManifest.xml`:

```xml
<meta-data android:name="plugin_name" android:value="ML Kit" />
<meta-data android:name="plugin_version" android:value="1.0.0" />
<meta-data android:name="plugin_author" android:value="Dokuen Reader" />
<meta-data android:name="plugin_description" android:value="Google ML Kit on-device text recognition (sample)" />
<meta-data android:name="plugin_license" android:value="Apache 2.0" />
```

This information is displayed to users in the Dokuen plugin manager before they enable the plugin.

### Capabilities

The plugin reports its capabilities to Dokuen:

- **Supports vertical text**: Yes (converts horizontal output to vertical when user requests it)
- **Requires internet**: No (fully on-device)
- **Supported languages**: Japanese (`ja`)
- **Has custom config**: Yes (half resolution setting)

### Configuration

Users can configure:

- **Half resolution** (boolean) - Downsample images to half resolution before processing. This improves speed and often accuracy for large text, but should be disabled for small text like web pages.

### Processing Flow

1. **Initialization** (`onInitialize`)
   - Creates ML Kit `TextRecognizer` with Japanese options
   - Reads user's half-resolution preference
   - Returns preprocessing requirements to host app

2. **Image Processing** (`onProcessImage`)
   - Receives bitmap from host app (already preprocessed per requirements)
   - Runs ML Kit text recognition
   - Extracts text blocks with symbol-level bounding boxes
   - Converts to vertical reading order if requested
   - Returns list of `OcrBlock` objects

3. **Shutdown** (`onShutdown`)
   - Closes ML Kit recognizer
   - Releases resources

### Vertical Text Conversion

ML Kit doesn't natively support vertical text recognition. When the user explicitly requests vertical text direction, this plugin converts ML Kit's horizontal output into vertical reading order:

1. **Extract symbols** - Flatten all text blocks into individual characters with bounding boxes
2. **Group into columns** - Cluster symbols that overlap horizontally into vertical columns
3. **Sort for reading** - Order columns right-to-left, symbols within columns top-to-bottom
4. **Regroup into blocks** - Merge nearby symbols into coherent text blocks

This demonstrates how to post-process OCR results to support different text layouts when the underlying OCR engine doesn't natively support them.

**Note:** This conversion only happens when the user explicitly selects "vertical" text direction. The plugin does not automatically detect text direction.

## Code Structure

```
app/src/main/java/.../mlkit/
└── MlkitOcrPluginService.kt    # Main plugin implementation

Key methods:
- onInitialize()                 # Setup ML Kit recognizer
- onProcessImage()               # Process image and return OCR results
- onShutdown()                   # Cleanup resources
- convertToVerticalReading()     # Convert horizontal output to vertical
```

## Testing

The sample includes comprehensive tests:

### Unit Tests (`src/test/`)

- `MlkitOcrPluginServiceTest.kt` - Tests plugin lifecycle, configuration, and vertical text conversion logic using Robolectric

Run unit tests:
```bash
./gradlew test
```

### Instrumented Tests (`src/androidTest/`)

- `MlkitOcrPluginServiceIntegrationTest.kt` - Tests actual ML Kit integration on device

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Performance

Typical performance on mid-range devices:

- **Initialization**: < 1 second
- **Processing**: 1-2 seconds for 1920x1080 screen capture
- **Memory usage**: ~50 MB during processing

## Limitations

- **Japanese only** - This sample uses ML Kit's Japanese recognizer. For other languages, use different ML Kit recognizer options.
- **Vertical text conversion** - The vertical text conversion is heuristic-based and may not be perfect for all layouts.
- **No auto text direction** - Plugin doesn't automatically detect text direction; user must explicitly specify horizontal or vertical.

## License

Apache 2.0 - See LICENSE file for details
