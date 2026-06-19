# Dokuen Plugin Directory

Welcome to the official directory of plugins for the Dokuen Reader family of apps.

Here you can find both official and community plugins that add support for many more OCR engines,
expand Dokuen's capabilities, and more.

> **⚠️ Disclaimer:** Plugins are developed and maintained by third-party community members. While
> Dokuen's architecture sandboxes plugins to protect the main app, you should always review a
> community plugin's source code, permissions, and metadata before installing.

## OCR Plugins

| Plugin Name                                                  | Description                                       | Author        | License    | Download                                      |
|--------------------------------------------------------------|---------------------------------------------------|---------------|------------|-----------------------------------------------|
| [Google Vision BYOK](sdk/sample-plugins/ocr_sample_gcp_byok) | Google Cloud Vision OCR with 'Bring Your Own Key' | Dokuen Reader | Apache 2.0 | Included in base app                          |
| [ML Kit (sample)](sdk/sample-plugins/ocr_sample_mlkit)       | Google ML Kit on-device text recognition          | Dokuen Reader | Apache 2.0 | [GitHub](sdk/sample-plugins/ocr_sample_mlkit) |

## Dictionary Plugins

| Plugin Name                                                        | Description                                                               | Author        | License    | Download                                                                                                                 |
|--------------------------------------------------------------------|---------------------------------------------------------------------------|---------------|------------|--------------------------------------------------------------------------------------------------------------------------|
| [Yomitan](https://github.com/dokuen-dev/dokuen-plugin-yomitan)     | Dokuen port of the popular browser extension                              | Dokuen Reader | GPL 3.0    | [Google Play Store](https://play.google.com/store/apps/details?id=io.github.dokuendev.dokuen.plugins.dictionary.yomitan) |
| [AI Dictionary (sample)](sdk/sample-plugins/dictionary_sample_llm) | AI dictionary that generates definitions using Gemini, Claude, or ChatGPT | Dokuen Reader | Apache 2.0 | Included in base app                                                                                                     |

## Translator Plugins

| Plugin Name                                                                        | Description                                                     | Author                                  | License    | Download                                                                      |
|------------------------------------------------------------------------------------|-----------------------------------------------------------------|-----------------------------------------|------------|-------------------------------------------------------------------------------|
| [Google Translate web](https://github.com/Luccavco/dokuen-google-translate-plugin) | Translate via Google Translate, no API key required             | [Luccavco](https://github.com/luccavco) | Apache 2.0 | [GitHub](https://github.com/Luccavco/dokuen-google-translate-plugin/releases) |
| [Google Translate (sample)](sdk/sample-plugins/dictionary_sample_google_translate) | Sample translator plugin using the Google Cloud Translation API | Dokuen Reader                           | Apache 2.0 | Included in base app                                                          |

## Build Your Own

Don't see the plugin you want? If you're a developer and want to create a new plugin that isn't
listed here, check out the [Plugin SDK documentation](sdk/README.md) to get started.

Once your plugin is published, you can submit it to be featured in this directory.
