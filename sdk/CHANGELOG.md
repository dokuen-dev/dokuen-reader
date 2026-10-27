# Changelog

## [0.9.0]

### Added

- Support for dictionary plugins to show a progress message while a custom action runs, through
  `CustomActionContext.setProgressMessage(message)`, in place of Dokuen's default progress text.
- `onProgressMessage` method to `ICustomActionCallback` AIDL.
- `CustomActionContext.MAX_PROGRESS_MESSAGE_LENGTH`: the length, in UTF-16 code units, that the
  host cuts progress messages to (100).
- Optional `plugin_inline_icon` and `plugin_inline_icon_monochrome` service meta-data, each an
  `android:resource` drawable, for a small icon that Dokuen shows where the plugin is selected.

## [0.8.1]

### Changed

- The SDK is now published to Maven Central, under the group `io.github.dokuendev.dokuenreader`
  (`io.github.dokuendev.dokuenreader:plugin-core`, `:ocr`, and `:dictionary`). Earlier versions
  were published only through JitPack, as `com.github.dokuen-dev.dokuen-reader:<module>`, and
  new versions are no longer published there. To upgrade, change the group in your dependencies
  and remove the JitPack repository if nothing else in your project uses it. The API is the same
  as in 0.8.0.

## [0.8.0]

### Added

- Support for dictionary plugins to request the readings of a text from the host during custom
  actions, through `CustomActionContext.requestReadings(text)`.
- `requestReadings` method to `ICustomActionCallback` AIDL, along with the
  `IReadingsRequestCallback` interface, the `WordReading` AIDL parcelable, and a `WordReading(...)`
  factory function.
- `CustomActionContext.MAX_READINGS_TEXT_LENGTH`: the longest text, in UTF-16 code units, a
  readings request may carry (500).

## [0.7.0]

### Added

- Support for dictionary plugins to request images (OCR screenshot or clipboard) or clipboard
  text from the host during custom actions.
- Two-argument `onExecuteCustomAction(actionPayload, context)` overload to
  `DictionaryPluginService`, providing a `CustomActionContext` with `requestImage()` and
  `requestClipboardText()`.
- Methods to `ICustomActionCallback` AIDL to support asynchronous host-bound image and text
  requests.
- `ImageRequestOptions` and `RequestedImage` AIDL parcelables, with `IImageRequestCallback` and
  `ITextRequestCallback` interfaces.
- Optional `message` to `CustomActionResult.UpdateResult`, so an action can update the displayed
  results and show a status message at the same time.
- `onUpdateResultWithMessage` to `ICustomActionCallback` AIDL, which carries such a result.
- `CustomActionContext.MAX_CLIPBOARD_TEXT_LENGTH`: the length, in UTF-16 code units, that the host
  cuts clipboard text to (10,000).
- `PluginHostConfigKeys.HOST_SDK_VERSION`: the SDK version the host implements, passed to
  `initialize()`, which tells the plugin which API elements the host supports.
- `@since` tags on API elements, naming the SDK version that introduced them (see "Host
  Compatibility" in the SDK README).

## [0.6.0]

### Added

- `displayFlags` integer bitmask to `DictionaryEntry` AIDL to support entry-level presentation
  rules.
- `FLAG_HEADWORD_STROKE_ORDER` constant in `DictionaryExtensions.kt` to allow plugins to request a
  stroke-order diagram presentation for the headword.
- `HeadwordSpan` AIDL and class to support link annotations for character ranges within a headword.
- `headwordSpans` property to `DictionaryEntry` AIDL to allow compound headwords to have independent
  lookup links.

## [0.5.0]

### Added

- `BlockSpan` AIDL and class to support nested block-level layout containers (paragraphs, list
  items, boxes, and tables) separated from inline text styling.
- Kotlin helper constructor `BlockSpan()` and added `blockSpans` support to the `StyledText()`
  factory.

### Changed

- Refactored `StyledText` to a "Separated Span" architecture, separating structural layout
  (`blockSpans`) from inline formatting (`styledSpans`).
- Cleaned up `InlineStyle` by removing deprecated block-level properties (`listItemOrdinal`,
  `listIndentLevel`, `listMarkerOverride`, `isBlock`, `isTable`).
- Renamed `backgroundColor` to `textBackgroundColor` in `InlineStyle` to separate character
  highlight from block container backgrounds.
- Corrected span conflict resolution semantics description in documentation and AIDL to clarify that
  scalar properties follow "innermost-wins" behavior.

## [0.4.1]

### Changed

- Updated documentation to recommend linking against explicit release versions rather than
  `main-SNAPSHOT`.
- Updated all sample plugins to use SDK version `0.4.1` by default.

## [0.4.0]

### Added

- Support for plugin-hosted configuration UIs where a plugin specifies its own Activity to be
  launched by the host instead of a schema-built UI.
- Added `getConfigActivityName()` and `isConfigured()` to `IOcrService` and `IDictionaryService`
  AIDL interfaces.
- Added `configActivityName` property and `isConfigured()` method to `OcrPluginService` and
  `DictionaryPluginService` base SDK classes.
- Support for blocks, tables, hover text, and links in InlineStyle.
- Added `executeCustomAction` to Dictionary API, allowing links within entries to trigger custom
  actions.
- `LIST_ITEM_BULLET` constant (`-1`) for `listItemOrdinal` to create bullet list items with a
  default "•" marker without requiring `listMarkerOverride`.

## [0.3.0]

### Added

- Initial release of the Dictionary Plugin API (`dictionary`).
    - Support for styled text, ruby markup, and optional host/plugin split responsibility for
      segmentation and deinflection.
- Sample dictionary and translator plugins.

### Removed

- `PluginCapabilityKeys.HAS_CUSTOM_CONFIG`
    - Custom config presence is now inferred from whether `getConfigSchema` returns a populated
      schema or not.

## [0.2.0]

### Added

- Added `regexPattern` property to `ConfigField` (AIDL and Kotlin builder) to allow regex-based
  validation of `STRING` type configuration fields.
- Updated `ocr_sample_gcp_byok` sample plugin to use `regexPattern` for validating the GCP API key
  format.

## [0.1.0]

### Added

- Initial release of the Dokuen Plugin SDK.
- Core plugin infrastructure and base services (`plugin-core`).
- OCR Plugin API implementation (`ocr`).
- Sample OCR plugins.
