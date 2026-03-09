package io.github.dokuendev.dokuenreader.plugin.core

import android.os.Bundle

/**
 * Factory for creating [InitResult] instances.
 */
object InitResultFactory {
    /**
     * Creates a successful initialization result.
     * 
     * @param requirements Domain-specific requirements as key-value pairs.
     */
    fun success(requirements: Bundle? = null): InitResult =
        InitResult(success = true, errorMessage = null, requirements = requirements)

    /**
     * Creates a failed initialization result.
     * 
     * @param errorMessage A localized error message describing the failure.
     */
    fun failure(errorMessage: String): InitResult =
        InitResult(success = false, errorMessage = errorMessage, requirements = null)
}
