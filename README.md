# Dokuen Japanese Reader

**The all-in-one Japanese reading companion for Android.**

## What is Dokuen?

**Dokuen Japanese Reader** (formerly *Dokuen Furigana Reader*) is an advanced Japanese reader and study tool for Android. It provides a non-intrusive furigana overlay that works across almost any Android app—whether you are reading manga, browsing news, playing video games, or watching videos with subtitles.

Instead of switching back and forth between a reader and a dictionary, Dokuen brings the information to you, in-place and in real-time.

### Key Features

* **System-wide Overlay:** Get furigana on top of kanji in any app without leaving your current screen.
* **Camera Mode:** Point your phone at physical books, manga, signs, or menus to see furigana instantly.
* **Integrated Dictionary:** Tap any word to see definitions, readings, and more.
* **Anki Integration:** Send new vocabulary directly to your Anki decks with a single tap.
* **Video Support:** Smart auto-pause feature for looking up subtitles on streaming services like Netflix or YouTube.

## Download

Download Dokuen Japanese Reader from the [Google Play Store](https://play.google.com/store/apps/details?id=io.github.dokuendev.dokuenreader).

## FAQ

**Q: Why do I see a "recording" icon and timer in my status bar?**

* Dokuen uses the Android Media Projection API to read the text on your screen. Android displays this icon/timer whenever the service is active to ensure transparency. However, **Dokuen does not record video.** It only captures a single still frame of your screen the moment you tap the floating button to process the text.

**Q: Does Dokuen send my data to a server?**

* **Standard Mode:** All OCR processing and dictionary lookups happen locally on your device. No data leaves your device.
* **Cloud Scan Mode:** If you select Cloud Scan Mode, the captured image is sent to the **Google Cloud Vision API** for processing. These images are processed in real-time and **deleted immediately** after the text is extracted. I don’t store your images or reading history.

**Q: Why does the OCR sometimes mis-identify words?**

* On-device OCR can struggle with stylized fonts and complex backgrounds. **Cloud Scan Mode** uses a significantly more powerful engine and is recommended for difficult manga layouts or complex fonts.

**Q: Is there an iOS version?**

* Currently, Dokuen is only available for Android due to the system-level overlay permissions required. iOS doesn’t allow apps to draw over other applications in this way.

**Q: Does it work with vertical text (縦書き)?**

* Yes! Dokuen fully supports both horizontal and vertical Japanese text layouts. If you are using the on-device OCR, you must **manually select the text direction** (Horizontal or Vertical) in settings before tapping the Start button, since the on-device engine cannot distinguish between them automatically. **Cloud Scan Mode**, on the other hand, can auto-detect the direction and even supports mixed horizontal and vertical text in the same image.

**Q: Can I use Google Lens OCR instead?**

* I’ve looked into this extensively, but there is no public API for Google Lens. Other tools that use it essentially have to "trick" Google into thinking a real user is uploading an image via a browser. This requires reverse engineering Google’s internal Protobufs, plus client impersonation (mimicking TLS fingerprints) to bypass bot detection. This violates both the Google Terms of Service and Play Store Developer Policies. Since I want to keep Dokuen on the Play Store, I can’t risk a permanent account ban or app suspension by using unofficial APIs.

## Bug Reports & Feedback

This repository is the central hub for Dokuen users to report issues and suggest new features. 

### How to Report a Bug

Before filing a new report, please check the **[Issues](../../issues)** tab to see if the problem has already been reported. If not, feel free to open a new issue.

To help us fix the problem faster, please include the following in your report:

1. **Device Model:** (e.g., Pixel 7, Samsung S23)
2. **Android Version:** (e.g., Android 13, 14)
3. **App Involved:** If the overlay isn’t working on a specific app (e.g., Kindle, X, Chrome), please specify which one.
4. **Steps to Reproduce:** What were you doing when the bug occurred?
5. **Screenshots:** Visuals are extremely helpful for UI or OCR issues.

### Feature Requests

Have an idea for a new feature? I’d love to hear it. Please open an issue and tag it with the `enhancement` label.

---

## Support & Links

* **Privacy Policy:** [https://dokuen-dev.github.io/furigana-reader/privacy/](https://dokuen-dev.github.io/furigana-reader/privacy/)
* **Email Support:** [dokuen.reader@gmail.com](mailto:dokuen.reader@gmail.com)
