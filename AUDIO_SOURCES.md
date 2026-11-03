# Dokuen Custom Audio Sources

Play pronunciation audio from a website or service you choose. A custom audio source tells Dokuen
how to look up a word: which URL to request, what to send with the request, and how to find the
audio in the response.

Once a source is selected, Dokuen uses it for the dictionary's speaker buttons and for the audio on
new Anki cards. Custom audio sources are available to everyone and don't need Dokuen Pro. Setting
one up takes no programming unless the service needs a script, and even then, a script is usually
only a few lines.

## Where to add a source

1. Open Dokuen > Settings > **Dictionary**.
2. In the **Pronunciation audio** section, tap the gear button.
3. Tap **Add audio source**.
4. Enter a name and a URL. Open **Advanced** if the source needs anything else.
5. Use **Test the source** to try it out.
6. Tap **Save**.

Saving a new source also selects it, unless it still needs a required setting.

## The simplest source

Many services need only a name and a URL:

| Field         | Value                                     |
|---------------|-------------------------------------------|
| **Name**      | `My audio site`                           |
| **Audio URL** | `https://audio.example.com/ja/{term}.mp3` |

When you look up 食べる, Dokuen requests `https://audio.example.com/ja/%E9%A3%9F%E3%81%B9%E3%82%8B.mp3`
and plays what comes back.

Everything else in this guide is optional, and lives in the collapsed **Advanced** section. Its
header summarizes what's inside, so you can see at a glance how a source is set up.

## Placeholders

Dokuen replaces these placeholders in the URL, in header values, and in form field values:

| Placeholder     | Replaced with                                                                                      |
|-----------------|----------------------------------------------------------------------------------------------------|
| `{term}`        | The word as written, such as `食べる`                                                                 |
| `{reading}`     | The word's reading in kana, such as `たべる`. If a word has no reading, this is the same as `{term}`. |
| `{language}`    | Always `ja`                                                                                        |
| `{setting:KEY}` | The value of the source's setting with that key. See [Settings](#settings).                        |

The chips under the URL field insert `{term}` and `{reading}` at the cursor. Any other text in
braces is left as it is.

- **Encoding is handled for you.** Values placed in a URL are percent-encoded, with spaces written
  as `%20`, and form field values are encoded when they're sent. Don't encode them again yourself.
  Header values are inserted as they are, except that line breaks are removed.
- **Every source must use `{term}` or `{reading}`** in its URL, in a header value, or in a form
  field value. Otherwise, it would give the same answer for every word.
- **The host can't contain placeholders.** `https://{term}.example.com/` isn't allowed, but any part
  of the URL after the host can use them.

Dokuen's placeholders and audio list format are the same as those of Yomitan's custom audio sources,
so you can usually reuse a template you already have, as long as it uses `https://`.

Source URLs must start with `https://`, because Android blocks unencrypted connections. For the same
reason, an `http://` audio URL in a list or returned by a script won't play.

## What the URL returns

Under **Advanced** > **What the URL returns**, choose how Dokuen should read the response:

| Choice                            | What Dokuen expects                                                  |
|-----------------------------------|----------------------------------------------------------------------|
| **Audio file**                    | The response is the audio itself. This is the default.               |
| **Audio list (JSON)**             | The response is a JSON list of audio URLs, which are tried in order. |
| **Something else (use a script)** | Your script reads the response and returns a list of audio URLs.     |

Audio can be in any format your device can play, such as MP3, Ogg, M4A, or WAV.

### The audio list format

```json
{
  "type": "audioSourceList",
  "audioSources": [
    { "url": "https://audio.example.com/clips/12345.mp3" },
    { "url": "/clips/67890.ogg" }
  ]
}
```

Dokuen tries each URL in order until one has audio. A relative URL is resolved against the URL of
the request. Other fields are ignored, and a response in any other shape counts as no audio for
that word.

### Scripts

A script is plain JavaScript that receives the response and returns the audio URLs to try, best
first. It can use these variables:

| Variable   | Value                                                                          |
|------------|--------------------------------------------------------------------------------|
| `body`     | The response body as text                                                      |
| `status`   | The HTTP status code. Scripts only run for a successful response (200 to 299). |
| `term`     | The word being looked up                                                       |
| `reading`  | The word's reading                                                             |
| `settings` | An object holding the values of the source's settings, by key                  |

You can write a script in either of two ways:

**As a bare expression** (no `return` needed):

```js
JSON.parse(body).clips.map(clip => clip.url)
```

**As a full script with an explicit `return`** (needed as soon as you have more than one statement):

```js
const clips = JSON.parse(body).clips;
const matching = clips.filter(clip => clip.reading === reading);
return matching.map(clip => clip.url);
```

The result must be an array of strings. Return an empty array when the service has no audio for the
word. A relative URL is resolved against the URL of the request, and anything that isn't an `http`
or `https` URL is ignored.

A script can only read the response and return URLs. It has no `fetch`, no DOM, and no Node.js
modules, so it can't make other requests, and it has no access to your files or to other apps. To
read HTML, use a regular expression, as in [this example](#a-script-that-reads-a-web-page).

## Method, headers, and form fields

These are all under **Advanced**:

- **Method** is `GET` by default. Choose `POST` if the service expects one.
- **Headers** are sent with the lookup request. Tap **Add header**, then enter a name and a value.
  A `User-Agent` header replaces the one Dokuen sends by default.
- **Form fields** appear when the method is `POST`. They're sent as a standard URL-encoded web form,
  in the order you list them. JSON request bodies aren't supported.

Headers, form fields, and settings only go with the lookup request itself. Dokuen fetches the audio
URLs from a list or a script with a plain `GET` and nothing else attached, so those URLs must work
on their own.

## Settings

A setting is a value that you enter separately from the source's URL and headers, such as an API key.

1. Under **Advanced** > **Settings**, tap **Add setting**.
2. Enter a **Key**, which is how templates refer to the setting. For example, the key `apiToken` is
   used as `{setting:apiToken}`.
3. Enter a **Label**, which is the name shown next to its field.
4. Turn on **Required** if the source can't work without a value.
5. Tap **Save**. A field for the setting's value now appears near the top of the editor, under the URL.

Scripts can read the same value as `settings.apiToken`. To change a setting, tap its edit (pencil)
icon. Its dialog also has the **Delete** button.

Put API keys in a setting rather than typing them directly into a URL or a header. Setting values
are stored only on your device, separately from the source, and aren't included in Android backups
or device transfers. After you restore to a new device, the source appears with **Needs setup**
until you enter the value again.

Keep in mind that a source sends the words you look up, and anything you put in its request, to the
service it contacts. Only set up sources for services you trust.

## Testing a source

**Test the source** looks up a word using what's currently in the editor, including changes you
haven't saved, and plays the audio it finds. Change the **Word** and **Reading** to try other words,
including one the service shouldn't have.

| Result                                                        | What it means                                                                                                                                          |
|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Found audio and played it.                                    | Everything works.                                                                                                                                      |
| The source has no audio for this word.                        | The service answered, but with no usable audio. A wrong URL often looks like this, because a "not found" answer is how a service says it has no audio. |
| The source rejected the request. Check its settings.          | The lookup was answered with a 401 or 403, which usually means a wrong or missing API key.                                                             |
| Couldn't reach the source. Check the URL and your connection. | The request failed or timed out, or the server answered with a 429 or a 5xx error.                                                                     |
| A required setting has no value.                              | Enter the setting's value in the editor.                                                                                                               |
| The parse script failed: ...                                  | Your script threw an error, took too long, returned the wrong thing, or the response was too large. The message says which.                            |
| Found audio, but it couldn't be played.                       | The response wasn't audio your device can play. It may be an HTML page, so check whether the source needs a different response type.                   |

## Examples

Each example lists only the fields that differ from the defaults. Apart from WaniKani, these use
placeholder addresses, so replace `example.com` and the response details with those of the real
service.

### A GET request that returns audio

The word is in the URL, and the response is the audio file.

| Field         | Value                                                            |
|---------------|------------------------------------------------------------------|
| **Name**      | `Audio by word`                                                  |
| **Audio URL** | `https://audio.example.com/{language}/{term}.mp3?kana={reading}` |

### A GET request with a header

This is the same kind of source, for a service that picks its format from the `Accept` header.

| Field                  | Value                                       |
|------------------------|---------------------------------------------|
| **Name**               | `Ogg audio`                                 |
| **Audio URL**          | `https://audio.example.com/say?word={term}` |
| **Advanced > Headers** | Name `Accept`, value `audio/ogg`            |

### A GET request that returns a JSON list

Use this for a service that answers in the [audio list format](#the-audio-list-format).

| Field                               | Value                                                         |
|-------------------------------------|---------------------------------------------------------------|
| **Name**                            | `Audio list`                                                  |
| **Audio URL**                       | `https://api.example.com/audio?term={term}&reading={reading}` |
| **Advanced > What the URL returns** | **Audio list (JSON)**                                         |

### A POST request that returns audio

Here, the word is sent as form fields rather than in the URL.

| Field                      | Value                                                     |
|----------------------------|-----------------------------------------------------------|
| **Name**                   | `Audio by form`                                           |
| **Audio URL**              | `https://audio.example.com/lookup`                        |
| **Advanced > Method**      | `POST`                                                    |
| **Advanced > Form fields** | `word` = `{term}`, `kana` = `{reading}`, `format` = `mp3` |

This sends the body `word=%E9%A3%9F%E3%81%B9%E3%82%8B&kana=%E3%81%9F%E3%81%B9%E3%82%8B&format=mp3`.

### A POST request that returns a JSON list

The method and the response type are independent, so any method works with any response type.

| Field                               | Value                            |
|-------------------------------------|----------------------------------|
| **Name**                            | `Search by form`                 |
| **Audio URL**                       | `https://api.example.com/search` |
| **Advanced > What the URL returns** | **Audio list (JSON)**            |
| **Advanced > Method**               | `POST`                           |
| **Advanced > Form fields**          | `query` = `{term}`               |

### A script that reads a JSON API

Use a script when a service's JSON has its own shape. Suppose that it answers like this, listing
every reading of a word:

```json
{
  "results": [
    { "reading": "たべる", "audio": "/clips/12345.mp3" },
    { "reading": "たべる", "audio": "/clips/67890.mp3" },
    { "reading": "くう", "audio": "/clips/24680.mp3" }
  ]
}
```

| Field                               | Value                                    |
|-------------------------------------|------------------------------------------|
| **Name**                            | `Dictionary API`                         |
| **Audio URL**                       | `https://api.example.com/words?q={term}` |
| **Advanced > What the URL returns** | **Something else (use a script)**        |

**Parse script:**

```js
const results = JSON.parse(body).results;
return results
    .filter(result => result.reading === reading)
    .map(result => result.audio);
```

For 食べる read as たべる, this returns the first two clips. Their relative URLs are resolved against
the request URL, so Dokuen first tries `https://api.example.com/clips/12345.mp3`.

### A script that reads a web page

Suppose that a dictionary site's page for a word has its recordings in `<audio>` tags. A regular
expression can pull them out.

| Field                               | Value                                        |
|-------------------------------------|----------------------------------------------|
| **Name**                            | `Dictionary site`                            |
| **Audio URL**                       | `https://dictionary.example.com/word/{term}` |
| **Advanced > What the URL returns** | **Something else (use a script)**            |

**Parse script:**

```js
[...body.matchAll(/<audio[^>]+src="([^"]+)"/g)].map(match => match[1])
```

### A service that needs an API key: WaniKani

[WaniKani](https://www.wanikani.com) has recordings for its vocabulary, available through its
[API](https://docs.api.wanikani.com/). This source uses a setting for your API token, headers to
send it, and a script to find the recordings for the right reading.

First, create a token on WaniKani's
[API Tokens page](https://www.wanikani.com/settings/personal_access_tokens). Dokuen only reads from
WaniKani, so the token doesn't need any write permissions.

Then, set up the source:

| Field                               | Value                                                                                |
|-------------------------------------|--------------------------------------------------------------------------------------|
| **Name**                            | `WaniKani`                                                                           |
| **Audio URL**                       | `https://api.wanikani.com/v2/subjects?types=vocabulary,kana_vocabulary&slugs={term}` |
| **Advanced > What the URL returns** | **Something else (use a script)**                                                    |
| **Advanced > Headers**              | Name `Authorization`, value `Bearer {setting:apiToken}`                              |
|                                     | Name `Wanikani-Revision`, value `20170710`                                           |
| **Advanced > Settings**             | Key `apiToken`, label `API token`, **Required** on                                   |

**Parse script:**

```js
const subject = JSON.parse(body).data[0];
if (!subject) return [];

return subject.data.pronunciation_audios
    .filter(audio => audio.metadata.pronunciation === reading)
    .filter(audio => audio.content_type === 'audio/mpeg')
    .map(audio => audio.url);
```

Finally, paste your token into the **API token** field near the top of the editor, tap **Test**, and
then tap **Save**.

Words that WaniKani doesn't teach count as having no audio. WaniKani's free accounts cover only its
first three levels, and its API documentation asks apps to respect that limit, so use this source
with a subscription for words above level 3. WaniKani isn't affiliated with Dokuen.

## Selecting, editing, and deleting sources

Your sources are listed on the audio page, the one the gear button opens.

- **To select a source,** tap its row. A source that shows **Needs setup** opens the editor instead,
  and is selected once you've entered its required values and saved.
- **To edit a source,** tap its edit (pencil) icon. Changes take effect as soon as you save, and
  Cancel or Back asks before discarding them.
- **To delete a source,** tap its delete (trash) icon and confirm. If it was the selected source,
  Dokuen goes back to its default audio source. This also deletes the source's setting values.

You can also change the selected source from the dropdown in the **Pronunciation audio** section of
the Dictionary settings, and tap the speaker button beside it to hear a sample.

If you used the **Custom URL** option in an earlier version of Dokuen, your URL has already been
moved into a custom audio source, which you can edit here.

## How lookups behave

- **In the dictionary,** a speaker button shows a spinner while the audio loads, and waits about 3
  seconds. If the audio isn't ready by then or the source has none, the word is read aloud instead,
  and the button shows a small red exclamation mark. A slow download keeps going in the background,
  so tapping the button again often plays it.
- **On Anki cards,** Dokuen waits for the download to finish, however long the request takes.
- **Recent answers are remembered** while the app is running, including "no audio". Editing the
  source or changing a setting's value starts fresh. Failed lookups aren't remembered, so they're
  tried again next time.

## Limits

- **Sources:** a name of at most 30 characters, different from your other sources' names.
- **Headers:** at most 10 on a source.
- **Form fields:** at most 20 on a source.
- **Settings:** at most 5 on a source. A key starts with a letter, and uses only letters, digits,
  and underscores, up to 32 characters. A label can be up to 40 characters.
- **Requests:** each request has 15 seconds to finish, and **Test** has 30 seconds overall.
- **Scripts:**
    - The script itself can be at most 32 KB.
    - The response it receives can be at most 2 MB.
    - It has 1 second to run, and 8 MB of memory.
    - It can return at most 20 URLs, each at most 2,048 characters.

## Troubleshooting

- **Save is disabled.** The editor shows what's wrong next to the field. If nothing is shown, the
  name or the URL is probably still empty. Errors in the **Advanced** section open it automatically.
- **"Use {term} or {reading} in the URL, a header, or a form field."** A form field only counts when
  the method is `POST`.
- **"Start the URL with https://."** Dokuen doesn't accept `http://` URLs, because Android blocks
  unencrypted connections. Check whether the service also offers `https://`.
- **"Refers to a setting that doesn't exist."** A template uses `{setting:KEY}`, but no setting has
  that key. Keys are case-sensitive.
- **"Form fields need the POST method."** Switch the method to `POST`, or remove the fields.
- **"The host isn't valid."** Check the part of the URL between `https://` and the next `/`. A
  backslash there isn't allowed.
- **It works in a browser but not in Dokuen.** The service may depend on cookies or on a login in
  your browser, which Dokuen doesn't have. Check whether it offers an API with a key, or a header it
  expects.
- **A list or script finds audio, but none plays.** The audio URLs may need the headers or key that
  the lookup request had, but they're fetched with nothing attached. If the service accepts a key in
  the URL, a script can add `settings.yourKey` to each URL it returns.
- **Debugging a script.** Throw an error to see a value, because the message appears in the test
  result. For example, `throw new Error(body.slice(0, 200))` shows the start of the response.
