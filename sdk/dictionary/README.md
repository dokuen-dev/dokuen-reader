# Dokuen Dictionary Plugin SDK

[![](https://jitpack.io/v/dokuen-dev/dokuen-reader.svg)](https://jitpack.io/#dokuen-dev/dokuen-reader)

Build dictionary lookup plugins for Dokuen Reader. This SDK provides a Kotlin-first
development experience with native coroutine support, structured definition data, and
automatic lifecycle management.

## Quick Start Checklist

1. [ ] Create a new Android project in Android Studio ("No Activity" template).
2. [ ] Set `minSdk` >= 29 in your `build.gradle.kts`.
3. [ ] Add the JitPack repository to your `settings.gradle.kts` (see [Installation](#installation)).
4. [ ] Add the SDK dependency:
   `implementation("com.github.dokuen-dev.dokuen-reader:dictionary:VERSION")`
5. [ ] Create your service class extending `DictionaryPluginService`.
6. [ ] Add all required metadata to your `AndroidManifest.xml`
   (see [AndroidManifest.xml](#androidmanifestxml)).
7. [ ] If your plugin is a **translator**, declare the
   `io.github.dokuendev.dokuenreader.category.TRANSLATOR` category in its intent-filter.
8. [ ] Implement `onLookup()` with your dictionary logic.
9. [ ] Declare `capabilities` with at minimum `SUPPORTED_SOURCE_LANGUAGES` and
   `SUPPORTED_TARGET_LANGUAGES`.

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
    // Replace VERSION with the latest release version (e.g. 0.4.1) shown in the badge above
    implementation("com.github.dokuen-dev.dokuen-reader:dictionary:VERSION")
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

Extend `DictionaryPluginService` and implement `onLookup`:

```kotlin
import io.github.dokuendev.dokuenreader.dictionary.DictionaryPluginService
import io.github.dokuendev.dokuenreader.plugin.core.PluginCapabilityKeys
import io.github.dokuendev.dokuenreader.plugin.core.PluginHostConfigKeys
import android.os.Bundle

class MyDictionaryPlugin : DictionaryPluginService() {

    private lateinit var db: DictionaryDatabase
    private var targetLanguage: String = "en"
    private var isDarkMode: Boolean = false

    override val capabilities = Bundle().apply {
        putStringArray(PluginCapabilityKeys.SUPPORTED_SOURCE_LANGUAGES, arrayOf("ja"))
        putStringArray(PluginCapabilityKeys.SUPPORTED_TARGET_LANGUAGES, arrayOf("en", "es"))
        putBoolean(PluginCapabilityKeys.HANDLES_SEGMENTATION, false)
        putBoolean(PluginCapabilityKeys.REQUIRES_DICTIONARY_FORM, true)
        putBoolean(PluginCapabilityKeys.REQUIRES_INTERNET, false)
    }

    override suspend fun onInitialize(config: Bundle?): InitResult {
        if (config == null) {
            return InitResultFactory.failure("Configuration is required")
        }

        // Read host-provided configuration
        val language = config.getString(PluginHostConfigKeys.LANGUAGE, "ja")
        targetLanguage = config.getString(PluginHostConfigKeys.TARGET_LANGUAGE, "en")
        isDarkMode = config.getString(PluginHostConfigKeys.UI_THEME) == "dark"

        // Load dictionary resources
        try {
            db = DictionaryDatabase.open(applicationContext, targetLanguage)
            return InitResultFactory.success()
        } catch (e: Exception) {
            return InitResultFactory.failure("Failed to load dictionary: ${e.message}")
        }
    }

    override suspend fun onLookup(
        contextText: String,
        cursorStartIndex: Int,
        cursorEndIndex: Int
    ): DictionaryResult {
        // Extract the word of interest
        val word = contextText.substring(cursorStartIndex, cursorEndIndex)

        // Query the dictionary
        val matches = db.findDefinitions(word)

        if (matches.isEmpty()) {
            // For "not found" throw WORD_NOT_FOUND exception or return empty definitions list.
            throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "No definition found for \"$word\""
            )
        }

        // Build structured entries with rich formatting
        val entries = matches.map { match ->
            DictionaryEntry(
                headword = match.headword,
                pronunciation = match.reading?.let {
                    listOf(RubySpan(0, match.headword.length, it))
                },
                body = buildStyledDefinition(match)
            )
        }

        return DictionaryResult(entries)
    }

    private fun buildStyledDefinition(match: DbEntry): StyledText {
        val text = buildString {
            append("${match.partOfSpeech}\n")
            match.senses.forEachIndexed { index, sense ->
                append("${index + 1}. $sense\n")
            }
        }

        val spans = mutableListOf<StyledSpan>()

        // Style the part-of-speech tag
        val posLength = match.partOfSpeech.length
        spans.add(StyledSpan(
            startIndex = 0,
            endIndex = posLength,
            style = InlineStyle(
                bold = true,
                fontSize = 0.9f,
                foregroundColor = if (isDarkMode) 0xFF90CAF9.toInt() else 0xFF1976D2.toInt()
            )
        ))

        // Style numbered list items
        var offset = posLength + 1
        match.senses.forEachIndexed { index, sense ->
            val itemText = "${index + 1}. $sense\n"
            spans.add(StyledSpan(
                startIndex = offset,
                endIndex = offset + itemText.length - 1,
                style = InlineStyle(
                    listItemOrdinal = index + 1,
                    listIndentLevel = 1
                )
            ))
            offset += itemText.length
        }

        return StyledText(text, spans)
    }

    override fun onShutdown() {
        if (::db.isInitialized) {
            db.close()
        }
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
        <!-- Declare classification categories: Dictionary, Translator, or both -->
        <category android:name="io.github.dokuendev.dokuenreader.category.DICTIONARY" />
        <category android:name="io.github.dokuendev.dokuenreader.category.TRANSLATOR" />
    </intent-filter>

    <!-- REQUIRED: All five fields must be present. -->
    <meta-data android:name="plugin_name" android:value="My Dictionary" />
    <meta-data android:name="plugin_version" android:value="1.0.0" />
    <meta-data android:name="plugin_author" android:value="Your Name" />
    <meta-data android:name="plugin_description" android:value="Offline J-E dictionary" />
    <meta-data android:name="plugin_license" android:value="MIT" />

    <!-- Optional but recommended -->
    <meta-data android:name="plugin_website" android:value="https://example.com" />
    <meta-data android:name="plugin_support_email" android:value="support@example.com" />
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

---

#### `capabilities: Bundle`

Declares what your plugin supports. Dokuen reads these after binding to configure the
lookup pipeline and settings UI.

**Required Capabilities:**

| Key                          | Type         | Description                                                                                         |
|------------------------------|--------------|-----------------------------------------------------------------------------------------------------|
| `SUPPORTED_SOURCE_LANGUAGES` | String Array | BCP-47 codes of languages you can look up. Must include `"ja"` to appear in Dokuen Japanese Reader. |
| `SUPPORTED_TARGET_LANGUAGES` | String Array | BCP-47 codes of languages you return definitions in (e.g. `["en", "es"]`).                          |

Plugins that omit either required capability will be treated as invalid and will not be
offered to the user in the plugin manager.

**Optional Capabilities:**

| Key                        | Type    | Default | Description                                                                            |
|----------------------------|---------|---------|----------------------------------------------------------------------------------------|
| `HANDLES_SEGMENTATION`     | Boolean | `false` | See [Segmentation behavior](#segmentation-behavior) below.                             |
| `REQUIRES_DICTIONARY_FORM` | Boolean | `false` | Dokuen deinflects the word before lookup (ignored when `HANDLES_SEGMENTATION = true`). |
| `REQUIRES_INTERNET`        | Boolean | `false` | Whether the plugin needs network access.                                               |

**Multi-Target Language Support:**

When `SUPPORTED_TARGET_LANGUAGES` contains more than one value, Dokuen automatically
adds a target-language selector to the plugin's settings UI. The selected language is
passed to `onInitialize()` via `PluginHostConfigKeys.TARGET_LANGUAGE`.

When `SUPPORTED_TARGET_LANGUAGES` contains exactly one value, no selector is shown, but
the single value is still passed via `TARGET_LANGUAGE` for consistency.

---

#### Dictionary vs. Translator Plugins

A plugin can be a **Dictionary**, a **Translator**, or both.

- **Dictionary**: Shown in the standard dictionary source list. Queried first when the user taps a
  word.
- **Translator**: Shown in the translator source list. Queried only when the dictionary source
  returns no results.

Classification is declared in the `AndroidManifest.xml` via intent `<category>` tags inside the
`<intent-filter>`:

```xml
<!-- Declare as Dictionary -->
<category android:name="io.github.dokuendev.dokuenreader.category.DICTIONARY" />

<!-- Declare as Translator -->
<category android:name="io.github.dokuendev.dokuenreader.category.TRANSLATOR" />
```

If neither category is declared, the plugin is classified as a Dictionary by default. A single
plugin can declare both categories to act in both roles.

**Translator plugins must set `HANDLES_SEGMENTATION = true`** in their capabilities to receive the
text context properly. Translators receive the query directly and must be capable of handling raw or
unsegmented text.

---

#### Segmentation behavior

The `HANDLES_SEGMENTATION` capability controls what `contextText` contains in `onLookup`.

---

**`HANDLES_SEGMENTATION = false` (default)**

`contextText` is the selected word only. `cursorStartIndex = 0`,
`cursorEndIndex = contextText.length`.

The selected text depends on the user's tap selection mode:

- **Word mode** (default): User taps anywhere in a word, entire word is selected
- **Character mode**: User taps a position, only that character is selected
- **Drag selection**: User drags across multiple words/characters, all selected text is concatenated

If `REQUIRES_DICTIONARY_FORM = true`, the word is deinflected to its dictionary form
before being passed.

```
User taps "食べて" in word mode
→ contextText      = "食べる"   (deinflected, REQUIRES_DICTIONARY_FORM = true)
  cursorStartIndex = 0
  cursorEndIndex   = 3

User taps "食" in character mode
→ contextText      = "食"
  cursorStartIndex = 0
  cursorEndIndex   = 1

User drags over "食べて" and "行く" in word mode
→ contextText      = "食べて行く"   (concatenated, then deinflected as one string)
  cursorStartIndex = 0
  cursorEndIndex   = 6

Note: Deinflection operates on the entire concatenated string by examining its suffix,
not on individual words separately.
```

**Important:** Drag selection can create **disjoint ranges** where non-contiguous text
is selected. For example, if the user selects "食" and "る" while skipping "べて", you
receive `contextText = "食る"`. Your plugin cannot distinguish whether the text came
from a contiguous selection or a disjoint one.

---

**`HANDLES_SEGMENTATION = true` — contiguous selection**

`contextText` is the full OCR block text. `cursorStartIndex` and `cursorEndIndex`
identify the selection within it. You receive surrounding context for
disambiguation and are responsible for your own segmentation and deinflection.

This applies to:

- Single taps (one word selected)
- Continuous drag selections over adjacent words within the same OCR block

```
User taps "食べて" in "今日ご飯食べてから出かけた"
→ contextText      = "今日ご飯食べてから出かけた"
  cursorStartIndex = 4
  cursorEndIndex   = 7

User drags over "食べてから" in the same sentence
→ contextText      = "今日ご飯食べてから出かけた"
  cursorStartIndex = 4
  cursorEndIndex   = 9
```

---

**`HANDLES_SEGMENTATION = true` — disjoint or multi-block selection**

When the user makes a disjoint selection (characters with a gap between them) or
selects across multiple OCR blocks, Dokuen cannot construct a single coherent block
text with honest cursor indices. In this case `contextText` is the concatenated
selected text, `cursorStartIndex = 0`, `cursorEndIndex = contextText.length` — the
same as `HANDLES_SEGMENTATION = false`.

```
User selects "食" and "る" while skipping "べて"
→ contextText      = "食る"    (concatenated selected characters)
  cursorStartIndex = 0
  cursorEndIndex   = 2
```

---

**The invariant that always holds:**

`contextText.substring(cursorStartIndex, cursorEndIndex)` is the primary text of
interest, regardless of `HANDLES_SEGMENTATION` setting or interaction type.

---

#### `configSchema: List<ConfigField>`

Configuration fields presented to the user in the plugin settings screen. See the
[OCR SDK documentation](../ocr/README.md#configschema-listconfigfield) for details
on `ConfigField` properties.

This property is optional. If your plugin does not require any user configuration,
simply do not override this property (it defaults to an empty list).

**Field types:**

- `ConfigFieldType.STRING` — Free-form text input
- `ConfigFieldType.BOOLEAN` — Checkbox or switch
- `ConfigFieldType.INT` — Numeric input
- `ConfigFieldType.ENUM` — Dropdown selection from predefined values (requires `enumValues`)

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

---

#### `onInitialize(config: Bundle?): InitResult`

Called once when the reading session starts. Load heavy resources here: dictionary
files, network clients, caches.

The `config` Bundle contains two categories of keys:

**User configuration** — values entered by the user in the plugin settings UI, keyed
by `ConfigField.key` strings from your `configSchema`.

**Host configuration** — values provided by Dokuen (defined in `PluginHostConfigKeys`):

| Key               | Type   | Description                                                                                                                                      |
|-------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `LANGUAGE`        | String | Active session source language (e.g. `"ja"`). Use this to select the appropriate internal configuration.                                         |
| `TARGET_LANGUAGE` | String | User-selected target language (e.g. `"en"`). Provided for all plugins; for multi-target plugins, this is the user's choice from the settings UI. |
| `USER_LOCALE`     | String | User's UI locale (e.g. `"en-US"`) for localizing your error messages.                                                                            |
| `UI_THEME`        | String | `"dark"` or `"light"`. **Use this when choosing text colors** to ensure legibility in both themes.                                               |

**UI_THEME and Color Accessibility:**

The `UI_THEME` key tells you whether the user's device is in dark or light mode at
initialization time. Use this when setting `foregroundColor` and `backgroundColor` in
`InlineStyle` to ensure your definitions remain legible in both themes.

Text runs without an explicit foreground color automatically receive Dokuen's default
contrasting color for the active theme, so you only need to set colors explicitly for
special styling (e.g., part-of-speech tags, example sentences).

**Example:**

```kotlin
override suspend fun onInitialize(config: Bundle?): InitResult {
    val language = config?.getString(PluginHostConfigKeys.LANGUAGE, "ja")
    val isDarkMode = config?.getString(PluginHostConfigKeys.UI_THEME) == "dark"
    val targetLang = config?.getString(PluginHostConfigKeys.TARGET_LANGUAGE, "en")

    // Choose theme-appropriate colors
    accentColor = if (isDarkMode) 0xFF90CAF9.toInt() else 0xFF1976D2.toInt()

    // Load dictionary for target language
    db = DictionaryDatabase.open(applicationContext, targetLang)
    return InitResultFactory.success()
}
```

Return `InitResultFactory.success()` on success, or
`InitResultFactory.failure(message)` with a user-facing error message on failure.
Dokuen will not start the reading session if initialization fails.

---

#### `onLookup(contextText, cursorStartIndex, cursorEndIndex): DictionaryResult`

Called each time the user selects a word. See [Segmentation behavior](#segmentation-behavior)
above for what `contextText` contains.

**Method Signature:**

```kotlin
suspend fun onLookup(
    contextText: String,
    cursorStartIndex: Int,
    cursorEndIndex: Int
): DictionaryResult
```

**Parameters:**

- `contextText`: The text context (full OCR block or selected text, depending on
  `HANDLES_SEGMENTATION`)
- `cursorStartIndex`: Inclusive start index of the selection in `contextText`
- `cursorEndIndex`: Exclusive end index of the selection in `contextText`

**Return Value:**
Return a `DictionaryResult` containing a list of `DictionaryEntry` objects.

**Error Handling:**

- **Recommended: Throw `DictionaryException` with `WORD_NOT_FOUND`** when no entries are found
- **Also acceptable: Return an empty `DictionaryResult`**. The host treats this the same as
  `WORD_NOT_FOUND`
- **DO throw `DictionaryException`** for genuine errors: network failures, invalid API keys,
  resource exhaustion, etc.
- The base class catches exceptions and converts them to `onFailure()` callbacks automatically
- Both `WORD_NOT_FOUND` exceptions and empty results trigger the same host behavior: showing "no
  definition found" and/or querying the translator

**Example:**

```kotlin
override suspend fun onLookup(
    contextText: String,
    cursorStartIndex: Int,
    cursorEndIndex: Int
): DictionaryResult {
    val word = contextText.substring(cursorStartIndex, cursorEndIndex)

    // Query your dictionary
    val matches = db.findDefinitions(word)

    // For "not found" throw WORD_NOT_FOUND exception or return empty definitions list.
    if (matches.isEmpty()) {
        throw DictionaryException(
            DictionaryErrorCode.WORD_NOT_FOUND,
            "No definition found for \"$word\""
        )
    }

    // Build structured entries
    val entries = matches.map { match ->
        DictionaryEntry(
            headword = match.headword,
            pronunciation = match.reading?.let {
                listOf(RubySpan(0, match.headword.length, it))
            },
            body = buildStyledDefinition(match)
        )
    }

    return DictionaryResult(entries)
}
```

---

#### `onShutdown()`

Called when the session ends. Release all resources allocated in `onInitialize`.

---

### Data Model

All data structures are AIDL parcelables that can be passed across process boundaries.
The SDK provides Kotlin convenience constructors that accept `List<T>` parameters for
better ergonomics.

#### `DictionaryEntry`

Represents a single headword and its full definition.

| Field           | Type          | Description                                                                                  |
|-----------------|---------------|----------------------------------------------------------------------------------------------|
| `headword`      | `String`      | The dictionary form of the word.                                                             |
| `pronunciation` | `RubySpan[]?` | Ruby annotations for the headword. Each `RubySpan` pairs a character range with its reading. |
| `body`          | `StyledText`  | The full definition content with optional formatting.                                        |

**Example:**

```kotlin
DictionaryEntry(
    headword = "食べる",
    pronunciation = listOf(
        RubySpan(0, 1, "た"),   // "食" → "た"
    ),
    body = StyledText(
        text = "to eat; to consume",
        styledSpans = listOf(
            StyledSpan(0, 7, InlineStyle(bold = true))  // bold "to eat"
        )
    )
)
```

#### `StyledText`

A text string paired with optional formatting spans and ruby annotations. Think of it
as an IPC-safe equivalent of Compose `AnnotatedString`.

| Field         | Type            | Description                                    |
|---------------|-----------------|------------------------------------------------|
| `text`        | `String`        | The plain text content.                        |
| `styledSpans` | `StyledSpan[]?` | Formatting annotations (bold, italic, color…). |
| `rubySpans`   | `RubySpan[]?`   | Ruby text annotations within the body.         |

**Example:**

```kotlin
StyledText(
    text = "noun\n1. food\n2. meal",
    styledSpans = listOf(
        // Style the part-of-speech tag
        StyledSpan(0, 4, InlineStyle(
            bold = true,
            fontSize = 0.9f,
            foregroundColor = 0xFF1976D2.toInt()
        )),
        // Style list items
        StyledSpan(5, 13, InlineStyle(listItemOrdinal = 1)),
        StyledSpan(14, 22, InlineStyle(listItemOrdinal = 2))
    )
)
```

#### `StyledSpan` and `InlineStyle`

A `StyledSpan` pairs a character range (`startIndex` inclusive, `endIndex` exclusive)
with an `InlineStyle`.

**InlineStyle Fields:**

| Field                | Type      | Description                                                                                        |
|----------------------|-----------|----------------------------------------------------------------------------------------------------|
| `bold`               | `Boolean` | Bold text.                                                                                         |
| `italic`             | `Boolean` | Italic text.                                                                                       |
| `fontSize`           | `Float`   | Relative size multiplier (1.0 = normal, 0.9 = smaller, 1.2 = larger).                              |
| `foregroundColor`    | `Int`     | ARGB color. `0` = use default text color (theme-appropriate).                                      |
| `backgroundColor`    | `Int`     | ARGB color. `0` = no background.                                                                   |
| `listItemOrdinal`    | `Int`     | `0` = not a list item. `>0` = numbered (e.g., 1 → "1."). `-1` (`LIST_ITEM_BULLET`) = bullet ("•"). |
| `listIndentLevel`    | `Int`     | Indent depth; each level adds 16dp. Default 1 for list items.                                      |
| `listMarkerOverride` | `String?` | Custom marker string (e.g. `"① "`, `"α. "`). Only has effect if `listItemOrdinal != 0`.            |
| `isBlock`            | `Boolean` | Whether this span represents an explicit block container.                                          |
| `isTable`            | `Boolean` | Whether this span represents an aligned grid table block (flat pipe grid layout).                  |
| `hoverText`          | `String?` | Clicking this text range opens a pop-up displaying this additional text.                           |
| `linkUrl`            | `String?` | Target URL/URI string to make the text range an underlined, styled hyperlink.                      |

**Span Overlap Behavior:**

Overlapping spans are merged additively:

- Multiple spans can apply bold, italic, etc. to the same text
- Where two spans set the same scalar property (color, font size), the later span in the array wins
- This matches Compose `AnnotatedString` semantics

**Example:**

```kotlin
val spans = listOf(
    StyledSpan(0, 10, InlineStyle(bold = true)),
    StyledSpan(5, 15, InlineStyle(italic = true)),
    StyledSpan(7, 12, InlineStyle(foregroundColor = 0xFFFF0000.toInt()))
)
// Result:
// [0-5):   bold
// [5-7):   bold + italic
// [7-10):  bold + italic + red
// [10-12): italic + red
// [12-15): italic
```

**List Items:**

To create numbered lists, set `listItemOrdinal` to a positive integer. Dokuen will
render the item with a prefix like "1.", "2.", etc., and apply indentation based on
`listIndentLevel`.

To create bullet lists, set `listItemOrdinal` to `LIST_ITEM_BULLET` (-1). Dokuen will
render the item with a default "•" marker:

```kotlin
import io.github.dokuendev.dokuenreader.dictionary.LIST_ITEM_BULLET

StyledSpan(0, 10, InlineStyle(
    listItemOrdinal = LIST_ITEM_BULLET,
    listIndentLevel = 1
))
```

For custom markers (roman numerals, circled numbers, etc.), use `listMarkerOverride`
on either numbered or bullet items.

> [!NOTE]
> `listMarkerOverride` is ignored if `listItemOrdinal` is 0 (i.e., the span is not a list item).

```kotlin
StyledSpan(0, 10, InlineStyle(
    listItemOrdinal = LIST_ITEM_BULLET,
    listMarkerOverride = "→ ",
    listIndentLevel = 1
))
```

**Blocks (`isBlock = true`):**

To support standalone block containers (such as example boxes, cross-reference boxes, or card-like
layouts), you can use the `isBlock` property.
When `isBlock` is set to `true` on an `InlineStyle`, the host application renders the styled
character range as a standalone block container separated from surrounding content.

* **Indentation**: Blocks can be indented relative to their surrounding layout by setting
  `listIndentLevel`.
* **Sizing**: Blocks automatically grow to fit their content vertically and occupy the available
  parent width.

*Example Block:*

```kotlin
val blockSpan = StyledSpan(
    startIndex = 0,
    endIndex = blockText.length,
    style = InlineStyle(
        isBlock = true,
        listIndentLevel = 1
    )
)
```

**Tables (`isTable = true`):**

To support grid layouts, you can declare structured table blocks.
When `isTable` is set to `true` on an `InlineStyle`, the host application interprets the text
content of the span as a flat table grid using a markdown-like **pipe character (`|`)** layout.

* **Row Structure**: Each line in the table text represents a row.
* **Cell Bounds**: Rows must start with `| ` and end with ` |`. Cells are separated by ` | `.
* **Explicit Cell Styling**: The host application does not automatically infer headers or highlight
  specific rows differently. You control formatting (like bold headers, cell backgrounds, or colored
  text) by explicitly setting nested `StyledSpan`s for individual cell contents.
* **Indentation**: Tables can be cleanly aligned/indented like list items by setting
  `listIndentLevel` on the table's `InlineStyle`.
* **Formatting inside cells**: Text, child styled spans, and ruby annotations inside cells are fully
  supported. They will be correctly preserved, styled, and positioned within their respective grid
  cells relative to cell boundaries.
* **Line breaks within cells**: You can introduce line breaks/newlines within a table cell using
  HTML-style `<br>` or `<br/>` tags (case-insensitive).

*Example Table:*

```kotlin
val tableText = """
| Expression | Reading | Meaning |
| 食べる | たべる | to eat |
| 食らう | くらう | to devour |
""".trimIndent()

val tableSpan = StyledSpan(
    startIndex = 0,
    endIndex = tableText.length,
    style = InlineStyle(
        isTable = true,
        listIndentLevel = 1
    )
)
```

**Hover Text (`hoverText`):**

Clickable ranges of text can be created to show additional descriptive text in a pop-up when clicked
by the user.
When `hoverText` is set to a non-null string on an `InlineStyle`, the host application registers a
click handler on that character range. When clicked, it displays the specified string in a mini
hover-text overlay.

*Example Hover Text:*

```kotlin
val definitionText = "Contains sodium chloride"
val hoverSpan = StyledSpan(
    startIndex = 9,
    endIndex = 24,
    style = InlineStyle(
        hoverText = "Common salt (NaCl)"
    )
)
```

**Links (`linkUrl`):**

Hyperlinks can be added to text ranges to support external references, internal dictionary
cross-linking (such as "See also" references), or custom interactive operations (like mutating
dictionary state or making custom RPC calls).
When `linkUrl` is set to a non-null string on an `InlineStyle`, the host application automatically:

1. Styles the text range with a contrasting **blue/cyan** link accent color.
2. Applies a visual **underline** (`TextDecoration.Underline`) to denote interactiveness.

**Supported Link Schemes:**

1. **External Web Links**: Use standard URL schemes starting with `http://` or `https://` (e.g.,
   `https://example.com/definition`). These must be properly URL-encoded. When clicked, they are
   opened in the system browser using Android's default URI handler.
2. **Dictionary Lookups (`lookup:`)**: Prefix the target word with `lookup:` (e.g.,
   `lookup:食べる`). The host application extracts the target word and calls the plugin's `onLookup`
   method with that target as the query string. The target string is delivered verbatim to the
   plugin without segmentation or deinflection. Unlike with external URLs, the target string need
   not be URL-encoded. (You may choose to URL-encode it, but if you do, it will not be automatically
   decoded by the host and will be passed back as-is, encoding and all.)
3. **Custom Actions (`action:`)**: Prefix your custom payload with `action:` (e.g.,
   `action:toggle_favorite?id=123`). The host application strips the prefix and passes the remaining
   payload string directly to the plugin's `onExecuteCustomAction` method. Unlike with external
   URLs, the target string need not be URL-encoded. (You may choose to URL-encode it, but if you do,
   it will not be automatically decoded by the host and will be passed back as-is, encoding and
   all.)

**Implementing Custom Actions:**

To support custom action links (`action:`), override the optional `onExecuteCustomAction` method in
your `DictionaryPluginService` subclass. Return a **`CustomActionResult`** on success, or throw an
exception on failure:

* **`CustomActionResult.SuccessMessage(message)`**: Displays a success status message in the host's
  dictionary window UI.
* **`CustomActionResult.UpdateResult(result)`**: Dynamically updates the displayed dictionary
  entries with new result data (e.g., to modify content after toggling a state).
* **Throwing an exception**: Automatically maps to a failure state in the host UI, displaying your
  exception message.

*Custom Action Service Example:*

```kotlin
class MyDictionaryPlugin : DictionaryPluginService() {

    override suspend fun onExecuteCustomAction(actionPayload: String): CustomActionResult {
        if (actionPayload.startsWith("favorite?word=")) {
            val word = actionPayload.removePrefix("favorite?word=")
            // Toggle word favorite in your database/state
            myDb.toggleFavorite(word)
            
            // Fetch updated definitions to dynamically update the host popup
            val updatedResult = onLookup(word, 0, word.length)
            return CustomActionResult.UpdateResult(updatedResult)
        } else {
            throw IllegalArgumentException("Unknown custom action: $actionPayload")
        }
    }
}
```

*Example Links in StyledText:*

```kotlin
val linkText = "See also 食べる or Action Toggle"
val spans = listOf(
    // 1. External Link
    StyledSpan(
        startIndex = 0,
        endIndex = 8,
        style = InlineStyle(linkUrl = "https://en.wikipedia.org/wiki/Taberu")
    ),
    // 2. Internal Lookup Link
    StyledSpan(
        startIndex = 9,
        endIndex = 12,
        style = InlineStyle(linkUrl = "lookup:食べる")
    ),
    // 3. Custom Action Link
    StyledSpan(
        startIndex = 16,
        endIndex = 29,
        style = InlineStyle(linkUrl = "action:favorite?word=食べる")
    )
)
```

**Color Accessibility:**

When setting `foregroundColor` or `backgroundColor`, consider the `UI_THEME` value from
`onInitialize()` to ensure legibility in both dark and light modes:

```kotlin
val isDarkMode = config?.getString(PluginHostConfigKeys.UI_THEME) == "dark"
val accentColor = if (isDarkMode) 0xFF90CAF9.toInt() else 0xFF1976D2.toInt()

StyledSpan(0, 10, InlineStyle(
    foregroundColor = accentColor
))
```

Text runs without an explicit foreground color automatically receive Dokuen's default
contrasting color for the active theme.

#### `RubySpan`

Pairs a character range in a text string with a ruby reading (furigana or bopomofo).

| Field        | Type     | Description                                         |
|--------------|----------|-----------------------------------------------------|
| `startIndex` | `Int`    | Inclusive start of the base text range.             |
| `endIndex`   | `Int`    | Exclusive end of the base text range.               |
| `rubyText`   | `String` | The ruby annotation to display above the base text. |

**Example:**

```kotlin
// For the word "乗り換え案内" (norikae annai)
val rubySpans = listOf(
    RubySpan(0, 1, "の"),       // "乗" → "の"
    RubySpan(2, 3, "か"),       // "換" → "か"
    RubySpan(4, 6, "あんない")  // "案内" → "あんない"
)
```

Ruby text is rendered above (or beside, for vertical layouts) the annotated base text
in the Dokuen UI.

#### `DictionaryResult`

The complete result of a dictionary lookup, wrapping a list of `DictionaryEntry` objects.

| Field     | Type                | Description                                        |
|-----------|---------------------|----------------------------------------------------|
| `entries` | `DictionaryEntry[]` | The list of dictionary entries matching the query. |

**Example:**

```kotlin
DictionaryResult(
    entries = listOf(
        DictionaryEntry(headword = "食べる", ...),
        DictionaryEntry(headword = "食べる", ...)  // Multiple senses
    )
)

// Or for "not found":
DictionaryResult(emptyList())
```

---

## Error Codes

Use `DictionaryErrorCode` constants when throwing `DictionaryException`:

| Code                   | When to use                                                                     |
|------------------------|---------------------------------------------------------------------------------|
| `WORD_NOT_FOUND`       | No dictionary entry found (recommended approach; empty results also acceptable) |
| `INVALID_QUERY`        | Query string is malformed or unsupported                                        |
| `NETWORK_ERROR`        | Network unavailable (for cloud-based dictionaries)                              |
| `AUTHENTICATION_ERROR` | Invalid API key or credentials                                                  |
| `QUOTA_EXCEEDED`       | API rate limit or quota reached                                                 |
| `INTERNAL_ERROR`       | Unexpected error (catch-all; also used for unhandled exceptions)                |

## Security Limits and Performance Guidelines

The host enforces these limits on every `DictionaryResult`. Responses that exceed a
limit are silently truncated rather than rejected, so your plugin continues working:

| Limit                    | Value  | Applies to                                           |
|--------------------------|--------|------------------------------------------------------|
| `MAX_DICTIONARY_ENTRIES` | 100    | Number of `DictionaryEntry` objects per result       |
| `MAX_BODY_LENGTH`        | 50,000 | Characters in a single `StyledText.text`             |
| `MAX_SPANS_PER_ENTRY`    | 500    | Combined `StyledSpan` + `RubySpan` objects per entry |

**Timeout:** Complete lookups within **30 seconds**. The host enforces a timeout and
will ignore results that arrive late. If your plugin needs network access, handle
timeouts gracefully and return an error via `DictionaryException`.

**Performance Best Practices:**

1. **Lazy Loading:** Load dictionary data in `onInitialize()`, not in the constructor
2. **Indexing:** Use proper database indices for fast lookups
3. **Async Operations:** Use coroutines for I/O operations (network, disk)
4. **Memory Management:** Release resources in `onShutdown()` to avoid memory leaks

**Network Plugins:**

If your plugin requires internet access:

- Set `REQUIRES_INTERNET = true` in capabilities
- Handle network errors gracefully with user-friendly messages
- Implement request timeouts (shorter than the 30-second host timeout)
- Test behavior when network is unavailable

---

## Color Accessibility and Theme Adaptation

Dokuen supports both dark and light themes. To ensure your definitions remain legible
in both modes, follow these guidelines:

### Using UI_THEME

The `UI_THEME` host config key tells you the current theme at initialization time:

```kotlin
override suspend fun onInitialize(config: Bundle?): InitResult {
    val isDarkMode = config?.getString(PluginHostConfigKeys.UI_THEME) == "dark"

    // Store theme-appropriate colors
    accentColor = if (isDarkMode) {
        0xFF90CAF9.toInt()  // Light blue for dark mode
    } else {
        0xFF1976D2.toInt()  // Dark blue for light mode
    }

    return InitResultFactory.success()
}
```

### Color Guidelines

**Foreground Colors:**

- **Default text:** Set `foregroundColor = 0` to use Dokuen's default text color (automatically
  contrasts with background)
- **Accent text:** Use theme-appropriate colors:
    - Dark mode: Lighter shades (e.g., light blue `0xFF90CAF9`, light green `0xFF81C784`)
    - Light mode: Darker shades (e.g., dark blue `0xFF1976D2`, dark green `0xFF388E3C`)
- **Semantic colors:**
    - Success: Dark mode `0xFF81C784`, Light mode `0xFF388E3C`
    - Warning: Dark mode `0xFFFFB74D`, Light mode `0xFFF57C00`
    - Error: Dark mode `0xFFE57373`, Light mode `0xFFD32F2F`

**Background Colors:**

- **Badges/Tags:** Use subtle backgrounds with sufficient contrast:
    - Dark mode: Darker backgrounds with lighter text
    - Light mode: Lighter backgrounds with darker text
- **Contrast ratio:** Aim for WCAG AA compliance (4.5:1 for normal text, 3:1 for large text)

**Example: Part-of-Speech Tags**

```kotlin
private fun createPosTag(pos: String, isDarkMode: Boolean): StyledSpan {
    val (fg, bg) = if (isDarkMode) {
        0xFFE3F2FD.toInt() to 0xFF1565C0.toInt()  // Light text on dark blue
    } else {
        0xFF1565C0.toInt() to 0xFFE3F2FD.toInt()  // Dark text on light blue
    }

    return StyledSpan(
        startIndex = 0,
        endIndex = pos.length,
        style = InlineStyle(
            bold = true,
            fontSize = 0.9f,
            foregroundColor = fg,
            backgroundColor = bg
        )
    )
}
```

### Testing Both Themes

Always test your plugin in both dark and light modes:

1. Enable dark mode in Android settings
2. Open Dokuen and perform lookups
3. Verify all text is legible and colors have sufficient contrast
4. Switch to light mode and repeat
5. Pay special attention to:
    - Part-of-speech tags
    - Example sentences
    - Highlighted text
    - Background colors

### Accessibility Checklist

- [ ] All text has sufficient contrast (4.5:1 minimum for normal text)
- [ ] Colors are not the only way to convey information (use bold, italic, etc.)
- [ ] Text without explicit colors uses default (theme-appropriate) colors
- [ ] Background colors have contrasting foreground colors
- [ ] Plugin tested in both dark and light modes
- [ ] Color choices work for users with color vision deficiencies

## Testing Your Plugin

### Unit Testing

```kotlin
class MyDictionaryPluginTest {
    @Test
    fun `lookup returns entry for known word`() = runTest {
        val plugin = MyDictionaryPlugin()
        val result = plugin.onLookup(
            contextText = "食べる",
            cursorStartIndex = 0,
            cursorEndIndex = 3
        )
        assertThat(result.entries).isNotEmpty()
        assertThat(result.entries.first().headword).isEqualTo("食べる")
    }

    @Test
    fun `lookup throws WORD_NOT_FOUND for unknown word`() = runTest {
        val plugin = MyDictionaryPlugin()
        val exception = assertThrows<DictionaryException> {
            plugin.onLookup("zzzunknown", 0, 10)
        }
        assertThat(exception.errorCode).isEqualTo(DictionaryErrorCode.WORD_NOT_FOUND)
    }
}
```

### Manual Testing with Dokuen

1. Build and install your plugin APK.
2. Open Dokuen and go to **Settings → Dictionary → Plugins**.
3. Enable your plugin and configure any required settings.
4. Start a reading session, tap a word, and verify the definition appears.
5. Test drag selection: sweep over several adjacent words and verify the result.
6. If your plugin has `HANDLES_SEGMENTATION = true`, also test that the surrounding
   context visible in your logs is the full block sentence, not just the selected word.
