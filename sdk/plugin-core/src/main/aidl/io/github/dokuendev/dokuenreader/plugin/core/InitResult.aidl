package io.github.dokuendev.dokuenreader.plugin.core;

/**
 * Result of plugin initialization.
 * 
 * Returned by initialize() to indicate success/failure and provide domain-specific
 * requirements as key-value pairs.
 */
parcelable InitResult {
    /**
     * Whether initialization succeeded.
     */
    boolean success;
    
    /**
     * If initialization failed, a localized error message.
     */
    @nullable String errorMessage;
    
    /**
     * Domain-specific requirements as key-value pairs.
     * See `OcrRequirementKeys` and `DictRequirementKeys` for details.
     */
    @nullable android.os.Bundle requirements;
}
