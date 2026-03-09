package io.github.dokuendev.dokuenreader.plugin.core

import android.os.Bundle

/**
 * Convenience constructors for plugin API types.
 * These provide List-based APIs for better Kotlin ergonomics.
 * 
 * See the corresponding .aidl files for detailed field documentation.
 */

fun PluginConfigSchema(fields: List<ConfigField>): PluginConfigSchema {
    return PluginConfigSchema().apply {
        this.fields = fields.toTypedArray()
    }
}

fun ConfigField(
    key: String,
    displayName: String,
    description: String,
    @ConfigFieldType.Type type: Int,
    defaultValue: String?,
    isRequired: Boolean,
    enumValues: List<String>? = null
): ConfigField {
    return ConfigField().apply {
        this.key = key
        this.displayName = displayName
        this.description = description
        this.type = type
        this.defaultValue = defaultValue
        this.isRequired = isRequired
        this.enumValues = enumValues?.toTypedArray()
    }
}

fun InitResult(
    success: Boolean,
    errorMessage: String? = null,
    requirements: Bundle? = null
): InitResult {
    return InitResult().apply {
        this.success = success
        this.errorMessage = errorMessage
        this.requirements = requirements
    }
}
