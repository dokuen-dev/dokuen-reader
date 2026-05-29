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
     */
    data class UpdateResult(val result: DictionaryResult) : CustomActionResult()
}
