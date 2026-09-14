# Dokuen Dictionary Extensions

Add your own buttons to Dokuen's dictionary lookup bar. Each button runs a small JavaScript
snippet you write, and turns its result into an action: open a URL, launch another app, send
an HTTP request, or copy something to the clipboard.

This is **not** the [Dokuen Plugin SDK](sdk/README.md). Plugins are separate Android
apps that supply dictionary lookups. Dictionary Extensions are short scripts you write directly
inside Dokuen's settings. No Android Studio, no APK, no installation required.

## Where to write scripts

1. Open Dokuen → Settings → Dictionary → **Customize Bottom Bar**.
2. Tap **+** to create a new button, or tap an existing button and then the edit (pencil) icon.
3. Give it a name and icon, then write your script in the **Action** field.
4. Use **Test Run** with a sample word to try it out before saving.
5. Drag it into place on the bar preview and tap **Done**.

## The script contract

Your script receives one variable, `entries`: an array of objects, one per dictionary result
entry currently shown, each shaped like:

```js
{ word: "食べる", pronunciation: "たべる", definition: "to eat" }
```

Most scripts only care about the first entry, `entries[0]`.

Your script must produce a single plain JavaScript object describing what should happen when the
button is tapped. You can write it two ways:

**As a bare expression** (no `return` needed):

```js
{ type: 'clipboard', text: entries[0].word }
```

**As a full script with an explicit `return`** (needed as soon as you have more than one
statement - a variable, an `if`, a loop, etc.):

```js
const word = entries[0].word;
return {
    type: 'clipboard',
    text: word
};
```

Whichever form you use, the object must have a `type` field naming one of the action types below.
Returning `null`, an array, a string, or anything other than a plain object is treated as an
error.

## Action types

### `clipboard` - copy text

```js
{
    type: 'clipboard',
    text: entries[0].word,           // required
    successMessage: 'Copied!'        // optional, shown as status text
}
```

### `url` - open a web page or app via a URL

```js
{
    type: 'url',
    url: `https://www.renshuu.org/lookup/vocab/${encodeURIComponent(entries[0].word)}`,
    copyToClipboard: entries[0].word, // optional: also copy this text first
    successMessage: 'Opened!'         // optional
}
```

Only `http://` and `https://` URLs are allowed. If another app has registered itself to handle
the URL (a common trick for deep-linking into a dictionary or study app), Android will open that
app instead of a browser.

### `intent` - launch another app directly

```js
{
    type: 'intent',
    packageName: 'jp.takoboto',              // required: target app's package name
    action: 'jp.takoboto.SEARCH',            // required: the Intent action to send
    extras: {                                // required (can be empty {})
        q: entries[0].word
    },
    component: 'jp.takoboto/.MainActivity',  // optional: 'package/.ClassName', to target
                                              // a specific Activity instead of relying on
                                              // action resolution
    copyToClipboard: entries[0].word         // optional
}
```

Use this when a target app registers a custom `Intent` action (check that app's documentation,
or its `AndroidManifest.xml` if it's open source) rather than a generic `ACTION_SEND`. You cannot
target Dokuen itself with this action type.

### `post` - send an HTTP POST request

```js
{
    type: 'post',
    url: 'https://example.com/api/add',       // required, http/https only
    headers: {                                // optional
        'Content-Type': 'application/json',
        'Authorization': 'Bearer YOUR_TOKEN'
    },
    body: JSON.stringify({ word: entries[0].word }), // optional
    successMessage: 'Sent!',                  // optional
    copyToClipboard: entries[0].word          // optional
}
```

If you don't provide `successMessage`, the response body (trimmed and truncated) is shown
instead.

### `error` - show a failure message

```js
{ type: 'error', message: 'This word has no kanji reading.' }
```

Use this to stop and tell the user something rather than performing an action - for example,
when a script-level check fails before you'd otherwise return a `url` or `post`.

## Making an HTTP request *inside* your script

The action types above are declarative. But sometimes you need to make a request, inspect its
response, and *then* decide what to do (or make a second request). For that, a global
`httpPost(url, headersJson, body)` function is available inside your script:

```js
const responseText = httpPost(
    'https://example.com/api/lookup',
    JSON.stringify({ 'Content-Type': 'application/json' }),
    JSON.stringify({ word: entries[0].word })
);
const result = JSON.parse(responseText);
```

- All three arguments are strings. Build `headersJson` and `body` with `JSON.stringify(...)`.
- It's synchronous: your script pauses until the request completes.
- On success it returns the response body as a string.
- On failure (network error or non-2xx status) it returns a JSON string of the shape
  `{ "error": true, "status": <code>, "message": "..." }` instead of throwing. Check for
  `result.error` rather than wrapping the call in `try`/`catch`.

Every request your script makes with `httpPost` still has to finish inside the overall 2-second
execution budget (see Limits, below).

## Limits

- **2 second timeout**, covering script evaluation and any `httpPost` calls it makes. A script
  that doesn't finish in time fails with "Script timed out."
- **No filesystem or device APIs.** The only capabilities available are the `entries` variable
  and the `httpPost` function. No reading files, no other app data, no access to sensors,
  contacts, etc.
- **Plain JavaScript only.** There's no DOM, no `fetch`, no Node.js modules, no `require`/`import`
  - just the language itself (variables, functions, `JSON`, template literals, `encodeURIComponent`,
  and so on).

## Full examples

These are the actual scripts Dokuen ships by default; a good starting point to copy and adapt.

### Copy word only

The simplest built-in button - just puts the word on the clipboard. Demonstrates the bare
minimum needed for a working script.

```js
// This is a sample script demonstrating the "clipboard" action type.
// It can be modified to put whatever you want onto the clipboard.
return {
    type: 'clipboard',
    text: entries[0].word
};
```

### Open in Renshuu

Opens the word's entry on renshuu.org. Demonstrates building a URL from the word with a template
literal and `encodeURIComponent`. The other path (`COPY_TO_CLIPBOARD`) is an example of how to
handle web sites that don't allow deep linking via the URL, by putting the word on the clipboard
first then opening the browser.

```js
const COPY_TO_CLIPBOARD = false; // Set to true to copy to clipboard and open main site instead

if (COPY_TO_CLIPBOARD) {
    return {
        type: 'url',
        url: 'https://www.renshuu.org',
        copyToClipboard: entries[0].word
    };
} else {
    return {
        type: 'url',
        url: `https://www.renshuu.org/lookup/vocab/${encodeURIComponent(entries[0].word)}`
    };
}
```

### Open in DeepL

Sends the word to the DeepL app if it's installed, otherwise falls back to opening
deepl.com in a browser. Demonstrates the `intent` action type with `extras` for a standard
`ACTION_SEND`.

```js
const USE_APP = true; // Set to false to use web URL instead

if (USE_APP) {
    return {
        type: 'intent',
        packageName: 'com.deepl.mobiletranslator',
        action: 'android.intent.action.SEND',
        extras: {
            'android.intent.extra.TEXT': entries[0].word
        }
    };
} else {
    return {
        type: 'url',
        url: `https://www.deepl.com/en/translator#ja/en/${encodeURIComponent(entries[0].word)}`
    };
}
```

### Open in Takoboto

Looks the word up in the Takoboto dictionary app. Demonstrates targeting a **custom Intent
action** a specific app registers for itself (`jp.takoboto.SEARCH`) rather than a generic
`ACTION_SEND`. Check the target app's own documentation for the action string and extra key it
expects.

```js
{
    type: 'intent',
    packageName: 'jp.takoboto',
    action: 'jp.takoboto.SEARCH',
    extras: {
        q: entries[0].word
    }
}
```

### Open in Kanji Study app

Looks the word up in the Kanji Study app. Demonstrates the `component` field, used here to
target one specific Activity (`.activity.SearchActivity`) directly instead of letting Android
resolve the action to whichever activity it thinks should handle it.

```js
{
    type: 'intent',
    packageName: 'com.mindtwisted.kanjistudy',
    action: 'android.intent.action.PROCESS_TEXT',
    extras: {
        'android.intent.extra.PROCESS_TEXT': entries[0].word
    },
    component: 'com.mindtwisted.kanjistudy/.activity.SearchActivity'
}
```

### Add word to JPDB deck

The most advanced built-in example: adds the word to a deck on jpdb.io. Demonstrates using
`httpPost` for a **preliminary lookup call** (resolving the word to jpdb's internal vocabulary
ID), parsing its JSON response and validating it with a plain `if`. Returns a `post` action
built from the looked-up data, or an `error` action if the lookup didn't find anything usable.

```js
const API_KEY = 'YOUR_API_KEY'; // Replace with your jpdb.io API key
const DECK_ID = 0; // Replace with your target deck ID

const parseResponseStr = httpPost(
    'https://jpdb.io/api/v1/parse',
    JSON.stringify({
        'Authorization': 'Bearer ' + API_KEY,
        'Content-Type': 'application/json'
    }),
    JSON.stringify({
        text: entries[0].word,
        position_length_encoding: 'utf16',
        token_fields: ['vocabulary_index', 'position', 'length'],
        vocabulary_fields: ['vid', 'sid', 'spelling', 'reading', 'meanings', 'part_of_speech', 'frequency_rank']
    })
);

const parseResult = JSON.parse(parseResponseStr);
if (parseResult.error || !parseResult.vocabulary || parseResult.vocabulary.length === 0) {
    return {
        type: 'error',
        message: 'Failed to parse word: ' + (parseResult.message || 'unknown error')
    };
}

const vid = parseResult.vocabulary[0][0];
const sid = parseResult.vocabulary[0][1];
const word = parseResult.vocabulary[0][2];

return {
    type: 'post',
    url: 'https://jpdb.io/api/v1/deck/add-vocabulary',
    headers: {
        'Authorization': 'Bearer ' + API_KEY,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        id: DECK_ID,
        vocabulary: [
            [vid, sid]
        ]
    }),
    successMessage: 'Added ' + word + ' to JPDB deck!'
};
```

## Icons

Pick from Dokuen's built-in icon set, or import your own SVG via the **+** button at the end
of the row of icons.

- A **single-color** SVG (one `fill`/`stroke` color throughout) is automatically recolored to
  match the app's light/dark theme, just like the built-in icons.
- A **multi-color** SVG is rendered with its original colors as-is.

## Editing and restoring built-in buttons

The default buttons (DeepL, Renshuu, Takoboto, Yomiwa, Kanji Study, JPDB, copy-word-only) are
regular script-backed buttons. You're free to open one and edit its script, e.g. flip a
`USE_APP`/`COPY_TO_CLIPBOARD` constant, or add your own API key. If you want to undo your changes,
open the button and tap **Restore Defaults** to reset it back to the script it shipped with. This
doesn't affect where the button sits on the bar, only its name/icon/script.

The five buttons with a lock icon in the details view (read aloud, add to Anki, copy, share,
repair) are handled natively and don't have an editable script.

## Troubleshooting

- **"Script evaluation failed."** - This indicates an issue verifying your Dokuen Pro license, not
  a bug in your code. Because extensions are only accessible if you had Pro in the first place,
  this usually means an expired subscription or a temporary issue syncing with Google Play. Check
  your account status in Settings → Dokuen Pro.
- Any other error message shown after tapping a button, or in the Test Run result, is your
  script's own error (a JavaScript exception, a `type: 'error'` result you returned, or a
  validation failure like a missing required field). The message itself explains what went
  wrong.
- Build request bodies and header maps with `JSON.stringify(...)` rather than hand-writing JSON
  strings, to avoid quoting mistakes.
- Use **Test Run** with a representative sample word while you iterate. It runs the exact same
  execution path as tapping the button for real.
