package io.github.dokuendev.dokuenreader.dictionary;

import io.github.dokuendev.dokuenreader.dictionary.DictionaryResult;

/**
 * Callback interface for asynchronous dictionary lookup results.
 * 
 * This callback is provided to plugins during the lookup() call and must be invoked
 * exactly once to report the result back to the host app.
 * 
 * Usage Pattern:
 * 1. Plugin receives lookup(contextText, startIndex, endIndex, callback)
 * 2. Plugin performs dictionary lookup (potentially on a background thread)
 * 3. Plugin calls EITHER callback.onSuccess() OR callback.onFailure()
 * 4. Plugin must NOT call both methods
 * 5. Plugin must NOT call the callback more than once per lookup() invocation
 * 
 * Threading:
 * - The callback can be invoked from any thread
 * - The host app handles thread safety internally
 * - Plugins using oneway lookup() receive the call on a Binder background thread
 *   and can invoke the callback synchronously from that same thread
 * 
 * Timeout:
 * - The host enforces a 30-second timeout on lookup() calls
 * - If the timeout expires, the host ignores subsequent callback invocations
 * - Plugins SHOULD complete lookups within 30 seconds to avoid timeout
 */
interface IDictionaryCallback {
    /**
     * Report successful dictionary lookup.
     * 
     * Call this method when the plugin has successfully looked up the word and
     * constructed the DictionaryResult with all matching entries.
     * 
     * The DictionaryResult should contain:
     * - entries: Array of DictionaryEntry, each containing:
     *   - headword: The dictionary form of the word
     *   - pronunciation: Optional ruby annotations for the headword
     *   - body: StyledText with the full definition content
     * 
     * Performance Considerations:
     * - The host app will validate the result size against security limits
     * - Results exceeding MAX_DICTIONARY_ENTRIES (100) will be truncated
     * - Results exceeding MAX_BODY_LENGTH (50,000 chars) per entry will be truncated
     * - Results exceeding MAX_SPANS_PER_ENTRY (500) will have spans truncated
     * 
     * @param result DictionaryResult containing all matching entries.
     *               Must not be null. Can contain empty entries array if no matches found.
     */
    oneway void onSuccess(in DictionaryResult result);
    
    /**
     * Report dictionary lookup failure.
     * 
     * Call this method when lookup fails for any reason, including:
     * - Word not found (use WORD_NOT_FOUND error code)
     * - Invalid query format
     * - Network errors (for online dictionaries)
     * - Authentication failures (invalid API key)
     * - Resource exhaustion (out of memory)
     * 
     * See [DictionaryErrorCode] for error codes.
     * 
     * Error Messages:
     * The error message should be:
     * - User-friendly and actionable (e.g., "Network unavailable" not "SocketException")
     * - Localized if possible based on the USER_LOCALE from initialize() config
     * - Specific enough for users to fix the problem
     *
     * The host app will display this message to the user in the dictionary UI. If the
     * error message is empty or null, a generic message based on the error code will be used.
     * 
     * After calling onFailure(), the plugin should remain in a ready state.
     * The host app may call lookup() again with a new query.
     * 
     * @param errorCode Error code from [DictionaryErrorCode] indicating the failure type.
     *                  Use the most specific code available to help users diagnose issues.
     * @param errorMessage Human-readable error message describing what went wrong.
     *                     Should be localized based on USER_LOCALE if possible.
     */
    oneway void onFailure(int errorCode, String errorMessage);
}
