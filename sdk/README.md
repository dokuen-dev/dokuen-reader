# Dokuen Plugin SDK

Welcome to the Dokuen Plugin SDK! This repository contains the tools, interfaces, and documentation
you need to build custom plugins for the Dokuen Reader family of Android apps.

Dokuen uses a modular, out-of-process plugin architecture. This means your plugin is a standalone
Android app (APK) that users install alongside Dokuen. They communicate securely via Android's
built-in Inter-Process Communication (IPC) using AIDL.

## SDK Structure

The SDK is organized into the following modules:

- **[plugin-core](./plugin-core/README.md)** - Shared infrastructure for all plugin types
  (configuration, security, error handling)
- **[ocr](./ocr/README.md)** - The OCR plugin API
- **[dictionary](./dictionary/README.md)** - The dictionary plugin API (coming soon)
- **[sample-plugins/](./sample-plugins/)**: Fully functional, runnable example Android apps
  demonstrating how to implement the APIs.

## How Plugins Work

Building a plugin for Dokuen is straightforward. You build a standard Android app that exposes a
specific `Service`.

1. **Declare Intent:** Your app's `AndroidManifest.xml` declares a `Service` with a specific intent
   filter (e.g., `io.github.dokuendev.dokuenreader.ocr.api.BIND_OCR_PROCESSOR`).

2. **Declare Metadata:** You provide required metadata (Name, Author, Description, License) in your
   manifest so Dokuen can display it to the user.

3. **Implement Service:** Extend the appropriate Service base class for your plugin type (e.g.,
   `OcrPluginService`) and implement the required methods.

When users install your app, Dokuen automatically discovers it, reads the manifest metadata, and
allows the user to enable it in their settings.

## Getting Started

Jump into the specific documentation for the type of plugin you want to build:

* [**Building an OCR Plugin Guide**](./ocr/README.md)
* [**Building a Dictionary Plugin Guide**](./dictionary/README.md)

## System Requirements

- **minSdk**: 29 (Android 10)
- **Kotlin**: 1.8+ recommended
- **Gradle**: 8.0+

## Distributing Your Plugin

Because plugins are standard Android apps, you can distribute them however you like!

* Publish them on the **Google Play Store**
* Distribute them via **F-Droid**
* Host the APKs on your own **GitHub Releases** page

Users simply install your APK, and Dokuen will handle the rest.

## Get Listed in the Official Directory

After you've published your plugin, feature it in Dokuen's official
[**Plugin Directory**](../PLUGINS.md) so that users can find it. Because that list is linked
directly from the in-app "Install Plugins" UI, it is the best way to get your work discovered.

### How to Submit

You can request that your plugin be added to the directory by either submitting a pull request
on [PLUGINS.md](../PLUGINS.md), or opening an issue titled `[Plugin Submission]: Your Plugin Name`
and providing the following details in the issue body:

* Plugin Name
* Author
* Short Description
* License
* Link to Repository / Download Page
