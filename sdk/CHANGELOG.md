# Changelog

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
