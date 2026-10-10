package io.github.dokuendev.dokuenreader.plugin.core

/**
 * A plugin SDK version (MAJOR.MINOR.PATCH), ordered component by component, so 0.10.0 is later
 * than 0.9.9.
 *
 * Use it to compare a version such as the value of [PluginHostConfigKeys.HOST_SDK_VERSION]
 * with the `@since` version of an API element.
 *
 * @since 0.7.0
 */
data class SdkVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SdkVersion> {

    override fun compareTo(other: SdkVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Parses "MAJOR.MINOR.PATCH" (surrounding whitespace allowed). Returns null for null or
         * anything else, including negative or non-numeric components.
         */
        fun parse(text: String?): SdkVersion? {
            val parts = text?.trim()?.split('.') ?: return null
            if (parts.size != 3) return null
            val numbers = parts.map { part -> part.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
            return SdkVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}
