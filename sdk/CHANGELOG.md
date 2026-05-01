# Changelog

## [0.2.0]
### Added
- Added `regexPattern` property to `ConfigField` (AIDL and Kotlin builder) to allow regex-based validation of `STRING` type configuration fields.
- Updated `ocr_sample_gcp_byok` sample plugin to use `regexPattern` for validating the GCP API key format.

## [0.1.0]
### Added
- Initial release of the Dokuen Plugin SDK.
- Core plugin infrastructure and base services (`plugin-core`).
- OCR Plugin API implementation (`ocr`).
- Sample OCR plugins.
