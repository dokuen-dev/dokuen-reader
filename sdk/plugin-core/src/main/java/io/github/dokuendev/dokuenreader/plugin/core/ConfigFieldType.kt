package io.github.dokuendev.dokuenreader.plugin.core

import androidx.annotation.IntDef

/**
 * Data types for [ConfigField.type].
 * 
 * These define what kind of input field the Dokuen app will display
 * in the plugin settings UI.
 */
object ConfigFieldType {
    /**
     * String input field.
     * User can enter free-form text.
     */
    const val STRING = 0

    /**
     * Boolean input field.
     * Displayed as a checkbox or switch.
     */
    const val BOOLEAN = 1

    /**
     * Integer input field.
     * User can enter numeric values only.
     */
    const val INT = 2

    /**
     * Enum dropdown field.
     * User selects from a predefined list of string values.
     * Requires ConfigField.enumValues to be non-empty.
     */
    const val ENUM = 3

    /**
     * Annotation for type-safe field type values.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(STRING, BOOLEAN, INT, ENUM)
    annotation class Type
}
