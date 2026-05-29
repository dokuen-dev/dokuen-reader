package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult;

/**
 * Callback interface for custom action execution on dictionary plugins.
 */
interface ICustomActionCallback {
    oneway void onSuccessMessage(String message);
    oneway void onUpdateResult(in DictionaryResult result);
    oneway void onFailure(String errorMessage);
}
