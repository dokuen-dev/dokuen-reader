# Dokuen OCR Plugin SDK

[![](https://jitpack.io/v/dokuen-dev/dokuen-reader.svg)](https://jitpack.io/#dokuen-dev/dokuen-reader)

Build text recognition plugins for Dokuen Reader. This SDK provides a Kotlin-first
development experience with native coroutine support and automatic lifecycle management.

## Quick Start Checklist

1. [ ] Create a new Android project in Android Studio ("No Activity" template, since this runs as a
   background service).
2. [ ] Set `minSdk` >= 29 in your `build.gradle.kts`.
3. [ ] Add the JitPack repository to your `settings.gradle.kts` (see [Installation](#installation)).
4. [ ] Add the SDK dependency:
   `implementation("com.github.dokuen-dev.dokuen-reader:ocr:VERSION")`
5. [ ] Create your service class extending `OcrPluginService`.
6. [ ] Add all required metadata to your `AndroidManifest.xml` (
   see [AndroidManifest.xml](#androidmanifestxml)).
7. [ ] Implement `onProcessImage()` with your OCR engine logic.
8. [ ] Customize `capabilities` and `configSchema` for your plugin.
9. [ ] Test your plugin (see [Testing](#testing-your-plugin)).

## Installation

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the SDK dependency in your module's `build.gradle.kts`:

```kotlin
dependencies {
    // Replace VERSION with the latest release version (e.g. 0.5.0) shown in the badge above
    implementation("com.github.dokuen-dev.dokuen-reader:ocr:VERSION")
}
```

The SDK follows [semantic versioning](https://semver.org/):

- **Major version** (1.x.x): Breaking changes that require code updates
- **Minor version** (x.1.x): New features, backwards compatible
- **Patch version** (x.x.1): Bug fixes, backwards compatible

Your plugin's compatibility is determined by which SDK version you compile against.
The host app maintains backwards compatibility with older major versions, so plugins
built with SDK 1.x will continue to work when the host app updates to SDK 2.x.

**System Requirements:**

- `minSdk` >= 29 (Android 10)
- Kotlin 1.8+ recommended for coroutine support

## Implementation Guide

### 1. Create the Service

Extend `OcrPluginService` and implement `onProcessImage`:

```kotlin
import io.github.dokuendev.dokuenreader.ocr.OcrPluginService

class MyOcrPlugin : OcrPluginService() {

    private lateinit var recognizer: TextRecognizer

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.SUPPORTS_VERTICAL_TEXT, true)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
        putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja"))
    }

    override val configSchema = listOf(
        ConfigField(
            key = "model_quality",
            displayName = "Model Quality",
            description = "Higher quality is slower but more accurate",
            type = ConfigFieldType.STRING,
            defaultValue = "standard",
            isRequired = false
        )
    )

    override suspend fun onInitialize(config: Bundle?): InitResult {
        val quality = config?.getString("model_quality") ?: "standard"
        recognizer = loadModel(quality)
        return InitResultFactory.success()
    }

    override suspend fun onProcessImage(
        bitmap: Bitmap,
        textDirection: String?
    ): List<OcrBlock> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).await()

        return visionText.textBlocks.map { block ->
            OcrBlock(
                text = block.text,
                symbolBounds = extractSymbolBounds(block),
                isVertical = (textDirection == "vertical")
            )
        }
    }

    override fun onShutdown() {
        recognizer.close()
    }
}
```

### 2. AndroidManifest.xml

Register your service with the required intent filter and metadata. **All five required
metadata fields must be present**, or Dokuen will not recognize the plugin.

```xml
<service android:name=".MyOcrPlugin" android:exported="true">
    <intent-filter>
        <action android:name="io.github.dokuendev.dokuenreader.ocr.BIND_OCR_SERVICE" />
    </intent-filter>

    <!-- REQUIRED: All five fields must be present. -->
    <meta-data android:name="plugin_name" android:value="My OCR Plugin" />
    <meta-data android:name="plugin_version" android:value="1.0.0" />
    <meta-data android:name="plugin_author" android:value="Your Name" />
    <meta-data android:name="plugin_description" android:value="High-accuracy Japanese OCR" />
    <meta-data android:name="plugin_license" android:value="Apache 2.0" />

    <!-- Optional but recommended -->
    <meta-data android:name="plugin_website" android:value="https://github.com/yourname/plugin" />
    <meta-data android:name="plugin_support_email" android:value="support@example.com" />
</service>
```

| Field                  | Required | Description                               |
|------------------------|----------|-------------------------------------------|
| `plugin_name`          | Yes      | Display name shown in the plugin manager  |
| `plugin_version`       | Yes      | Version string (e.g., "1.0.0")            |
| `plugin_author`        | Yes      | Your name or organization                 |
| `plugin_description`   | Yes      | Short description of what the plugin does |
| `plugin_license`       | Yes      | License type (e.g., "Apache 2.0", "MIT")  |
| `plugin_website`       | No       | URL for more information                  |
| `plugin_support_email` | No       | Contact email for support                 |

The metadata is displayed to users in the plugin manager UI before they enable your
plugin, allowing them to make an informed decision about whether to trust and use it.

## API Reference

### `OcrPluginService`

The base class you extend. It manages the plugin lifecycle and provides these
override points:

#### `capabilities: Bundle`

A Bundle of key-value pairs describing what your plugin supports. Dokuen reads these
to adjust its UI. Use the standard keys from `PluginCapabilityKeys`:

| Key                            | Type         | Description                              |
|--------------------------------|--------------|------------------------------------------|
| `SUPPORTS_VERTICAL_TEXT`       | Boolean      | Enables "vertical" text direction option |
| `SUPPORTS_AUTO_TEXT_DIRECTION` | Boolean      | Enables "auto" text direction option     |
| `REQUIRES_INTERNET`            | Boolean      | Whether the plugin needs network access  |
| `SUPPORTED_LANGUAGES`          | String Array | BCP-47 language codes (e.g., `["ja"]`)   |

You may also add custom keys beyond these standard ones.

#### `configSchema: List<ConfigField>`

Configuration fields that Dokuen presents to the user in the plugin settings screen.
The user's entries are delivered to `onInitialize()` in the `config` Bundle, keyed
by `ConfigField.key`.

This property is optional. If your plugin does not require any user configuration,
simply do not override this property (it defaults to an empty list).

User-defined config keys **must not** start with an underscore (`_`). Keys beginning
with `_` are reserved for host-provided values (see below).

Each `ConfigField` has the following properties:

| Property       | Type          | Description                                                                                             |
|----------------|---------------|---------------------------------------------------------------------------------------------------------|
| `key`          | String        | Unique key for this field (alphanumeric, dots, hyphens, underscores only)                               |
| `displayName`  | String        | Label shown to the user                                                                                 |
| `description`  | String        | Help text describing the field                                                                          |
| `type`         | Int           | One of `ConfigFieldType.STRING`, `.BOOLEAN`, `.INT`, `.ENUM`                                            |
| `defaultValue` | String?       | Default value (null if none)                                                                            |
| `isRequired`   | Bool          | Whether the user must fill in this field                                                                |
| `enumValues`   | List<String>? | For ENUM type: list of acceptable values shown in dropdown. Required for ENUM, ignored for other types. |
| `regexPattern` | String?       | For STRING type: optional regex that the user's input must match. Ignored for other types.              |

**Example with ENUM type:**

```kotlin
override val configSchema = listOf(
    ConfigField(
        key = "model_quality",
        displayName = "Model Quality",
        description = "Higher quality is slower but more accurate",
        type = ConfigFieldType.ENUM,
        defaultValue = "standard",
        isRequired = false,
        enumValues = listOf("low", "standard", "high")
    ),
    ConfigField(
        key = "api_key",
        displayName = "API Key",
        description = "Your API key for cloud processing",
        type = ConfigFieldType.STRING,
        defaultValue = null,
        isRequired = true
    )
)
```

The ENUM type displays as a dropdown menu in the Dokuen settings UI, restricting user input to the
predefined values.

**Examples with `regexPattern` (STRING types only):**

```kotlin
override val configSchema = listOf(
    ConfigField(
        key = "scale_factor",
        displayName = "Scale Factor",
        description = "A positive decimal number (e.g., 1.5)",
        type = ConfigFieldType.STRING,
        defaultValue = "1.0",
        isRequired = false,
        regexPattern = "^[0-9]*\\.?[0-9]+$"
    ),
    ConfigField(
        key = "custom_id",
        displayName = "Custom ID",
        description = "Must be 10 to 20 alphanumeric characters",
        type = ConfigFieldType.STRING,
        defaultValue = null,
        isRequired = true,
        regexPattern = "^[a-zA-Z0-9]{10,20}$"
    )
)
```

For STRING types, the `regexPattern` will enforce constraints (like length, format, or character
types) and show an error in the UI if the user's input does not match.

#### Plugin-Hosted Configuration UI (`configActivityName` & `isConfigured`)

By default, Dokuen builds a configuration UI automatically from your `configSchema`. However, if
your plugin needs a complex configuration flow (e.g., custom OAuth flows, custom UI components, or
reading a configuration file from local storage), your plugin can host its own configuration UI.

To use a plugin-hosted configuration UI, you must:

1. **Specify your configuration Activity** by overriding `configActivityName`:
   ```kotlin
   override val configActivityName = ".MyConfigActivity"
   ```
   *Use a leading dot (e.g. `".MyConfigActivity"`) for a class relative to your plugin's package, or
   a fully-qualified class name.*

   When `configActivityName` is non-null, Dokuen ignores `configSchema` for UI-building and will
   instead launch your Activity when the user taps "Configure".

2. **Declare the Activity in your plugin's `AndroidManifest.xml`**:
   ```xml
   <activity
       android:name=".MyConfigActivity"
       android:exported="true"
       android:theme="@style/Theme.Material3.DayNight">
       <!-- Ensure the activity is launchable by the host app -->
   </activity>
   ```

3. **Report configuration readiness** by overriding `isConfigured()`:
   ```kotlin
   override fun isConfigured(): Boolean {
       // Perform your validation here (e.g., checking if SharedPreferences contain valid credentials)
       return checkCredentialsValid()
   }
   ```
   *Since your plugin manages its own configuration storage (e.g., using its own `SharedPreferences`
   or database), the host cannot verify configuration completeness automatically. Thus, you **must**
   override `isConfigured()` to report whether your plugin is ready to be activated. The default
   implementation returns `true`.*

> [!IMPORTANT]
> **Mutual Exclusivity:** The plugin-hosted configuration flow (`configActivityName` +
`isConfigured()`) and the schema-based configuration flow (`configSchema`) are mutually exclusive.
> If you specify a non-null `configActivityName`, Dokuen will ignore the `configSchema` entirely for
> UI building, and you must manage configuration storage and readiness check yourself.

#### `onInitialize(config: Bundle?): InitResult`

Called once when the user starts a reading session ("Start" button). Use this to
load heavy resources such as ML models, network clients, or processing buffers.

The `config` Bundle contains:

1. **User configuration values**: keyed by your `ConfigField.key` strings.
2. **Host configuration values**: standard keys provided by Dokuen:
    - `PluginHostConfigKeys.LANGUAGE` (`"_language"`): BCP-47 code of the active
      reading language (e.g., `"ja"`, `"zh"`). Omitted if your plugin reported only
      one supported language.
    - `PluginHostConfigKeys.USER_LOCALE` (`"_user_locale"`): The user's preferred UI
      locale for localizing error messages.

**Preprocessing requirements:** You may request that Dokuen preprocess the image
before delivering it to `onProcessImage`. Pass a requirements Bundle to
`InitResultFactory.success(requirements)` using keys from `OcrRequirementKeys`:

| Key                    | Type    | Description                                       |
|------------------------|---------|---------------------------------------------------|
| `MAX_WIDTH`            | Int     | Max bitmap width; larger images are downsampled   |
| `MAX_HEIGHT`           | Int     | Max bitmap height; larger images are downsampled  |
| `DOWNSAMPLE`           | Boolean | Halve the resolution before any other resizing    |
| `CONVERT_TO_GRAYSCALE` | Boolean | Convert to grayscale before sending to the plugin |

If initialization fails, return `InitResultFactory.failure(errorMessage)`.

#### `onProcessImage(bitmap: Bitmap, textDirection: String?): List<OcrBlock>`

Called each time the user captures an image. The SDK provides a standard Android
`Bitmap` in ARGB_8888 format.

**Parameters:**

- `bitmap`: The captured image, with any requested preprocessing already applied.
- `textDirection`: A hint for the expected text orientation:
    - `"horizontal"`: Left-to-right text
    - `"vertical"`: Top-to-bottom Japanese text (only if `SUPPORTS_VERTICAL_TEXT` is
      reported)
    - `"auto"`: Plugin determines direction per block (only if
      `SUPPORTS_AUTO_TEXT_DIRECTION` is reported)

**Return value:** A list of `OcrBlock` objects. Each block contains:

- `text`: The recognized text string.
- `symbolBounds`: An array of `RectF`, one bounding box per character in `text`,
  in the bitmap's pixel coordinate space.
- `isVertical`: `true` if the block runs vertically (top-to-bottom, right-to-left
  column order), `false` for horizontal text.

Return an empty list if no text is detected.

**Cancellation:** If the user cancels, the coroutine is cancelled and a
`CancellationException` is thrown at the next suspension point. The SDK catches
this automatically -- you do not need to catch or handle it.

However, you are responsible for:

- **State consistency:** After cancellation, `onProcessImage` may be called again.
  Your plugin must remain in a usable state. If you update mutable state during
  processing, ensure it is safe when the coroutine is cancelled partway through.
- **Local resource cleanup:** Wrap locally allocated resources (temporary files,
  native handles) in `try/finally` or Kotlin's `.use {}`.
- **CPU-bound work:** Cancellation is cooperative. Long-running CPU work without
  suspension points will not be interrupted. Call `ensureActive()` periodically
  in tight loops to allow cancellation to take effect.
- **Do not catch `CancellationException`:** The SDK relies on it. If you use a
  broad `catch (e: Exception)` block, re-throw if the exception is a
  `CancellationException`.

**Error handling:** Throw `OcrException(errorCode, message)` for domain-specific
errors (see [Error Codes](#error-codes)). Unhandled exceptions are reported as
`INTERNAL_ERROR`.

#### `onShutdown()`

Called when the reading session ends. Unload ML models, close network connections,
and free buffers. This may be called even if `onInitialize()` was not called or
failed, so your implementation should be safe in any state.

## Error Codes

Use `OcrErrorCode` constants when throwing `OcrException`:

| Code                   | When to use                                               |
|------------------------|-----------------------------------------------------------|
| `NETWORK_ERROR`        | Network unavailable, timeout, or connection failed        |
| `AUTHENTICATION_ERROR` | Invalid API key or credentials                            |
| `QUOTA_EXCEEDED`       | API rate limit or usage quota exceeded                    |
| `INVALID_ARGUMENT`     | Malformed input (should not occur in normal use)          |
| `MODEL_UNAVAILABLE`    | Required ML model failed to load or is corrupted          |
| `RESULT_PARSING_ERROR` | OCR succeeded but result post-processing failed           |
| `CANCELED`             | Operation was canceled (handled automatically by the SDK) |
| `TIMEOUT`              | Operation exceeded time limit                             |
| `INTERNAL_ERROR`       | Unexpected error (catch-all for unknown issues)           |

Always provide descriptive, user-friendly error messages. Localize them based on
`PluginHostConfigKeys.USER_LOCALE` if possible.

## Testing Your Plugin

### Unit Testing

Test your OCR logic independently:

```kotlin
class OcrEngineTest {
    @Test
    fun testTextRecognition() {
        val engine = MyOcrEngine()
        val testImage = loadTestImage()
        val result = engine.process(testImage)

        assertEquals("期待される文字", result[0].text)
    }
}
```

### Manual Testing with Dokuen

1. Build and install your plugin APK on a device or emulator.
2. Open the Dokuen app.
3. Go to **Settings → Scan Mode → Custom**.
4. Enable your plugin in the list.
5. Configure any required settings (e.g., API key).
6. Start a reading session and test OCR capture.

### Debugging Tips

- Use `adb logcat` with your plugin's package name to view logs.
- If your plugin does not appear in the settings menu, verify that **all five
  required metadata fields** are present in your `AndroidManifest.xml`.
- Test on both low-end and high-end devices to verify performance.
