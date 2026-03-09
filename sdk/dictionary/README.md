# Dokuen Dictionary Plugin SDK

Build dictionary lookup plugins for Dokuen Reader. This SDK provides a Kotlin-first
development experience with native coroutine support and automatic lifecycle
management.

### **Note:** This API is a PLACEHOLDER. The final version of `onLookup` won't look anything like this. The real version will support structured dictionary entries and two-way communication for things like adding flashcards, "starring" entries in your dictionary app, etc. This early placeholder is only for working out the basic plumbing first.

## Quick Start Checklist

1. [ ] Create a new Android project in Android Studio ("No Activity" template).
2. [ ] Set `minSdk` >= 29 in your `build.gradle.kts`.
3. [ ] Add the JitPack repository to your `settings.gradle.kts` (see [Installation](#installation)).
4. [ ] Add the SDK dependency:
   `implementation("com.github.dokuen-dev.dokuen-reader:dictionary:main-SNAPSHOT")`
5. [ ] Create your service class extending `DictionaryPluginService`.
6. [ ] Add all required metadata to your `AndroidManifest.xml` (
   see [AndroidManifest.xml](#androidmanifestxml)).
7. [ ] Implement `onLookup()` with your dictionary logic.
8. [ ] Customize `capabilities` and `configSchema` for your plugin.

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

Then add the SDK dependency:

```kotlin
dependencies {
    implementation("com.github.dokuen-dev.dokuen-reader:dictionary:main-SNAPSHOT")
}
```

**Note:** During the beta period, use `main-SNAPSHOT` to get the latest development
version. When the SDK is officially released, versioned releases (e.g., `1.0.0`) will
be available.

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

Extend `DictionaryPluginService` and implement `onLookup`:

```kotlin
import io.github.dokuendev.dokuenreader.dictionary.DictionaryPluginService

class MyDictionaryPlugin : DictionaryPluginService() {

    private lateinit var db: DictionaryDatabase

    override val capabilities = Bundle().apply {
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
        putStringArray(PluginCapabilityKeys.SUPPORTED_LANGUAGES, arrayOf("ja"))
        putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, true)
    }

    override suspend fun onInitialize(config: Bundle?): InitResult {
        db = DictionaryDatabase.open(applicationContext)
        return InitResultFactory.success()
    }

    override suspend fun onLookup(query: String): String {
        val entry = db.findDefinition(query)
            ?: throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "No entry found for \"$query\""
            )
        return entry.definition
    }

    override fun onShutdown() {
        db.close()
    }
}
```

### 2. AndroidManifest.xml

Register your service with the required intent filter and metadata. **All five
required metadata fields must be present.**

```xml
<service android:name=".MyDictionaryPlugin" android:exported="true">
    <intent-filter>
        <action android:name="io.github.dokuendev.dokuenreader.dictionary.BIND_DICTIONARY_SERVICE" />
    </intent-filter>

    <!-- REQUIRED: All five fields must be present. -->
    <meta-data android:name="plugin_name" android:value="My Dictionary" />
    <meta-data android:name="plugin_version" android:value="1.0.0" />
    <meta-data android:name="plugin_author" android:value="Your Name" />
    <meta-data android:name="plugin_description" android:value="Offline J-E dictionary" />
    <meta-data android:name="plugin_license" android:value="MIT" />
</service>
```

| Field                  | Required | Description                               |
|------------------------|----------|-------------------------------------------|
| `plugin_name`          | Yes      | Display name shown in the plugin manager  |
| `plugin_version`       | Yes      | Version string (e.g., "1.0.0")            |
| `plugin_author`        | Yes      | Your name or organization                 |
| `plugin_description`   | Yes      | Short description of what the plugin does |
| `plugin_license`       | Yes      | License type (e.g., "MIT", "Apache 2.0")  |
| `plugin_website`       | No       | URL for more information                  |
| `plugin_support_email` | No       | Contact email for support                 |

## API Reference

### `DictionaryPluginService`

The base class you extend. Override points:

#### `capabilities: Bundle`

A Bundle of key-value pairs describing plugin capabilities. Relevant standard keys:

| Key                        | Type         | Description                              |
|----------------------------|--------------|------------------------------------------|
| `REQUIRES_INTERNET`        | Boolean      | Whether the plugin needs network access  |
| `SUPPORTED_LANGUAGES`      | String Array | BCP-47 codes (e.g., `["ja"]`)            |
| `HANDLES_SEGMENTATION`     | Boolean      | Plugin receives full sentence + index    |
| `REQUIRES_DICTIONARY_FORM` | Boolean      | Dokuen deinflects the word before lookup |

**Segmentation behavior:**

- If `HANDLES_SEGMENTATION` is `false` (default), Dokuen segments the sentence and
  passes the isolated word to `onLookup`.
- If `HANDLES_SEGMENTATION` is `true`, Dokuen passes the full sentence context. The
  plugin is responsible for finding and segmenting the target word.
- If `REQUIRES_DICTIONARY_FORM` is `true`, Dokuen deinflects the word to its base
  form before passing it.
- `HANDLES_SEGMENTATION = true` implies the plugin also handles deinflection;
  `REQUIRES_DICTIONARY_FORM` has no effect in that case.

#### `configSchema: List<ConfigField>`

Configuration fields presented to the user. See the
[OCR SDK documentation](../ocr/README.md#configschema-listconfigfield) for details
on `ConfigField` properties.

**Field types:**

- `ConfigFieldType.STRING` - Free-form text input
- `ConfigFieldType.BOOLEAN` - Checkbox or switch
- `ConfigFieldType.INT` - Numeric input
- `ConfigFieldType.ENUM` - Dropdown selection from predefined values (requires `enumValues`)

**Example:**

```kotlin
override val configSchema = listOf(
    ConfigField(
        key = "dictionary_source",
        displayName = "Dictionary Source",
        description = "Choose which dictionary to use",
        type = ConfigFieldType.ENUM,
        defaultValue = "jmdict",
        isRequired = false,
        enumValues = listOf("jmdict", "jmnedict", "kanjidic")
    )
)
```

#### `onInitialize(config: Bundle?): InitResult`

Called once when the session starts. The `config` Bundle contains user settings and
host values:

- `PluginHostConfigKeys.LANGUAGE`: The active reading language.
- `PluginHostConfigKeys.USER_LOCALE`: The user's preferred locale.

Return `InitResultFactory.success()` or `InitResultFactory.failure(errorMessage)`.

#### `onLookup(query: String): String`

Called each time the user taps a word. Return the definition text.

> This is a placeholder API. The return type will become a structured data class
> in future versions.

Throw `DictionaryException` for domain-specific errors. Unhandled exceptions
are reported as `INTERNAL_ERROR`.

#### `onShutdown()`

Called when the session ends. Release all resources.

## Error Codes

Use `DictionaryErrorCode` constants when throwing `DictionaryException`:

| Code                   | When to use                                        |
|------------------------|----------------------------------------------------|
| `WORD_NOT_FOUND`       | No dictionary entry found for the query            |
| `INVALID_QUERY`        | Query string is malformed or unsupported           |
| `NETWORK_ERROR`        | Network unavailable (for cloud-based dictionaries) |
| `AUTHENTICATION_ERROR` | Invalid API key or credentials                     |
| `INTERNAL_ERROR`       | Unexpected error (catch-all)                       |

## Testing Your Plugin

### Unit Testing

```kotlin
class DictionaryTest {
    @Test
    fun testLookup() {
        val db = TestDictionaryDatabase()
        val definition = db.findDefinition("食べる")

        assertEquals("to eat", definition)
    }
}
```

### Manual Testing with Dokuen

1. Build and install your plugin APK.
2. Open Dokuen and go to **Settings → Dictionary → Custom**.
3. Enable your plugin.
4. Start a reading session, tap a word, and verify the definition appears.
