# Dokuen Plugin Core Foundation

This module contains the shared infrastructure for the Dokuen Plugin ecosystem (`BasePluginService`,
`PluginConfigSchema`, `PluginSecurity`, etc.).
It acts as a foundation layer that domains (like `ocr` or `dictionary`) depend on to construct their
SDK wrapper implementations.

You do not need to include this module directly in your dependencies. The individual plugin-type
packages, such as `io.github.dokuendev.dokuenreader:ocr:...`, include this package transitively.

### Internal Logic

This package contains the raw IPC and AIDL `Parcelables`. Third-party plug-in developers do not
typically interact with these components.