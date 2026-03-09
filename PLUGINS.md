# Dokuen Plugin Directory

Welcome to the official directory of plugins for the Dokuen Reader family of apps.

Here you can find both official and community plugins that add support for many more OCR engines,
expand Dokuen's capabilities, and more.

> **⚠️ Disclaimer:** Plugins are developed and maintained by third-party community members. While
> Dokuen's architecture sandboxes plugins to protect the main app, you should always review a
> community plugin's source code, permissions, and metadata before installing.

---

*The Plugins feature currently only available in the **beta version** of Dokuen Japanese Reader. To
try it out:*

* On Android:
  Visit [Dokuen's Play Store page](https://play.google.com/store/apps/details?id=io.github.dokuendev.dokuenreader)
  at tap "Join the beta".
* On Web: Visit [this link](https://play.google.com/apps/testing/io.github.dokuendev.dokuenreader)
  and click "Become a tester".

**Note that the beta version may be unstable.** If you encounter any issues, please report them on
the [GitHub issue tracker](https://github.com/dokuen-dev/dokuen-reader/issues).

## OCR Plugins

| Plugin Name                                                  | Description                                       | Author        | License    | Download             | 
|--------------------------------------------------------------|---------------------------------------------------|---------------|------------|----------------------| 
| [Google Vision BYOK](sdk/sample-plugins/ocr_sample_gcp_byok) | Google Cloud Vision OCR with 'Bring Your Own Key' | Dokuen Reader | Apache 2.0 | Included in base app | 
| [ML Kit](sdk/sample-plugins/ocr_sample_mlkit)                | Google ML Kit on-device text recognition (sample) | Dokuen Reader | Apache 2.0 | Included in base app |

## Dictionary Plugins

> Dictionary plugins aren't fully supported yet. The latest beta is focused on ironing out OCR
> plugins first, then we'll move on to dictionary plugins ASAP after that.

| Plugin Name | Description | Author | License | Download | 
|-------------|-------------|--------|---------|----------| 
|             |             |        |         |          | 

## Build Your Own

Don't see the plugin you want? If you're a developer and want to create a new plugin that isn't
listed here, check out the [Plugin SDK documentation](sdk/README.md) to get started.

Once your plugin is published, you can submit it to be featured in this directory.
