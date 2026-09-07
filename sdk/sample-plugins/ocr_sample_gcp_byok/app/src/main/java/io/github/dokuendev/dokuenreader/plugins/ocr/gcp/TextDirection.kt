package io.github.dokuendev.dokuenreader.plugins.ocr.gcp

enum class TextDirection(val value: String) {
    NOT_SET(""),
    HORIZONTAL("horizontal"),
    VERTICAL("vertical"),
    AUTO("auto");

    companion object {
        fun fromString(text: String?): TextDirection {
            return entries.firstOrNull { it.value.equals(text, ignoreCase = true) }
                ?: NOT_SET // Default value
        }
    }
}
