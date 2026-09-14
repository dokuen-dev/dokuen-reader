# Dokuen Japanese Reader

**Universal furigana overlay, camera OCR, and immersion reading companion for Android.**

<p><kbd><img src="https://github.com/user-attachments/assets/bd93374c-3a36-4682-b6f5-af1ab674a598" width="250"></kbd>&nbsp;&nbsp;&nbsp;&nbsp;<kbd><img src="https://github.com/user-attachments/assets/1b861875-fb90-4a27-8717-f9e52919f89a" width="250"></kbd>&nbsp;&nbsp;&nbsp;&nbsp;<kbd><img src="https://github.com/user-attachments/assets/206a5bca-fa66-4ec5-b416-25f19a7d8d10" width="250"></kbd></p>

## What is Dokuen?

**Dokuen Japanese Reader** (formerly *Dokuen Furigana Reader*) is an advanced Japanese reading and study companion for Android. Instead of interrupting your immersion by switching between apps and dictionaries, Dokuen overlays furigana (phonetic readings) and a one-tap dictionary directly on top of Japanese text across any Android app, from manga and raw visual novels to video subtitles and web browsers.

Designed specifically for active immersion learners and sentence miners, Dokuen features a modular architecture that lets you tailor your OCR engines, dictionaries, and SRS workflows to your exact study routine.

### Key Features

* **System-wide Overlay:** Render furigana on top of kanji in any app, across games, browsers, manga apps, and media players without leaving your current screen.
* **Camera Mode:** Point your phone at physical manga volumes, books, street signs, packaging, or restaurant menus to see furigana appear directly over print.
* **Tap-to-Define Lookups:** Tap any word to view definitions, readings, and more.
* **Comprehension Helper:** Drag-select full sentences or tricky clauses to generate quick structural aids and contextual translations.
* **Anki Integration:** Send target words, context sentences, and readings directly to your Anki decks with a single tap.
* **Modular OCR Engine Plugins:** Choose the text recognition engine that fits your hardware and privacy needs. Run fast, lightweight local engines or high-accuracy cloud models.
* **Custom Dictionary Plugins:** Connect your preferred dictionary ecosystems, with ready-to-use Yomitan and JPDB plugins or custom community plugins.
* **Quick-Action Shortcuts & Scripting:** Trigger custom URL schemes, launch external dictionary apps, or write simple action scripts to bridge Dokuen directly into your study pipeline.
* **Subtitles & Video Support:** Smart auto-pause feature allows you to inspect subtitles on streaming platforms like Netflix without breaking playback flow.

## Download

Get Dokuen Japanese Reader on the [Google Play Store](https://play.google.com/store/apps/details?id=io.github.dokuendev.dokuenreader).

## Plugins & Integrations

Dokuen is built from the ground up to be extensible:
* **OCR Engines:** Switch between high-accuracy cloud recognition (e.g., Google Cloud Vision) and local, on-device offline models.
* **Dictionaries:** Connect pre-built plugins such as Yomitan or JPDB, or configure custom dictionaries.
* **External Tools:** Set up quick actions to pass terms directly into third-party apps or custom SRS setups.

For documentation on installing plugins or building your own, see the [Plugins Guide](PLUGINS.md).

To explore or customize quick actions, see [Dictionary Extensions](EXTENSIONS.md).

## FAQ

**Q: Why do I see a "recording" icon and timer in my status bar?**

* Dokuen uses Android’s standard Media Projection API to inspect the text on your screen. Android displays this status bar indicator whenever the capture permission is granted. **Dokuen does not record video.** It captures a single still frame only when you tap the scan button to process visible text.

**Q: Does Dokuen send my screen data to external servers?**

* **Local Engines:** When using local OCR plugins and the offline built-in dictionary, all processing occurs entirely on your device. No data leaves your phone.
* **Cloud OCR Plugins:** If you enable a cloud-based engine (such as Google Cloud Vision), the captured frame is sent securely to extract text. Images are processed in real-time and **deleted immediately** following text recognition. Dokuen does not store your captures or reading history.

**Q: Why does the OCR occasionally misidentify characters?**

* Highly stylized display typography, complex manga sound effects (onomatopoeia), or textured backgrounds can sometimes trip up lightweight on-device models. For difficult manga layouts or stylized fonts, switch to a cloud-based OCR plugin for significantly higher recognition accuracy.

**Q: Does Dokuen support vertical text (縦書き)?**

* Yes. Dokuen handles both horizontal and vertical Japanese text layouts. Depending on your active OCR engine, you can toggle text direction in settings, while advanced cloud plugins automatically detect and parse mixed orientations in a single frame.

**Q: Can I use Yomitan, JPDB, or other dictionary systems?**

* Yes. Dokuen supports custom dictionary plugins (including official plugins for Yomitan and JPDB available on Google Play) as well as configurable quick-action shortcuts. Check out the [Plugins Guide](PLUGINS.md) for setup details.

**Q: Is there an iOS version available?**

* No. Dokuen requires Android’s system-level overlay and media projection permissions to draw interactive furigana over third-party apps. iOS does not allow background applications to inspect or draw across other running apps in this manner.

## Bug Reports & Feedback

This repository tracks bug reports, integration improvements, and feature discussions.

### How to Report a Bug

1. Check the **[Issues](../../issues)** tab to confirm the issue has not already been submitted.
2. If opening a new issue, include:
   * **Device Model:** (e.g., Pixel 8, Galaxy S24)
   * **Android Version:** (e.g., Android 14, 15)
   * **Active Plugins:** Specified OCR engine and dictionary plugins in use.
   * **Target App:** The specific app where the issue occurred (e.g., Kindle, Chrome, specific game/reader).
   * **Steps to Reproduce:** Exact steps that lead to the error.
   * **Screenshots/Logs:** Highly recommended for layout alignment or OCR misidentification reports.

### Feature Requests

Have an idea for a new feature? I’d love to hear it. Please open an issue and tag it with the `enhancement` label.

---

## Support & Links

* **Privacy Policy:** [https://dokuen-dev.github.io/furigana-reader/privacy/](https://dokuen-dev.github.io/furigana-reader/privacy/)
* **Support Email:** [dokuen.reader@gmail.com](mailto:dokuen.reader@gmail.com)
