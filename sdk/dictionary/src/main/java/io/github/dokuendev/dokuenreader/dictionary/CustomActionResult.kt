package io.github.dokuendev.dokuenreader.dictionary

/**
 * Result of executing a custom action on a dictionary plugin.
 */
sealed class CustomActionResult {
    /**
     * Action completed successfully and displays a success status message in the host UI.
     */
    data class SuccessMessage(val message: String) : CustomActionResult()

    /**
     * Action completed successfully and dynamically updates the displayed dictionary results.
     *
     * If [message] is not null, the host UI also shows it, like it would with [SuccessMessage].
     */
    data class UpdateResult(
        val result: DictionaryResult,
        /** @since 0.7.0 */
        val message: String? = null
    ) : CustomActionResult()
}
