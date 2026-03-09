package io.github.dokuendev.dokuenreader.dictionary;

/**
 * Placeholder asynchronous callback for Dictionary plugins.
 */
interface IDictionaryCallback {
    /**
     * Report successful lookups.
     * @param definition The definition text.
     */
    void onSuccess(String definition);

    /**
     * Report failures.
     */
    void onFailure(int errorCode, String errorMessage);
}
