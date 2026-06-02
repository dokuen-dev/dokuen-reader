# Simple Dictionary Plugin Sample

A basic sample dictionary plugin for Dokuen Reader that demonstrates loading an offline dictionary
file and performing lookups.

## Features

This sample demonstrates:

- **Loading dictionary from assets** - Reading a dictionary file in `onInitialize()`.
- **Basic dictionary lookup** - Simple word-to-definition mapping
- **Styled text** - Using bold and italic formatting for parts of speech and definitions
- **Ruby annotations** - Showing pronunciation above kanji characters
- **No segmentation** - Plugin receives pre-segmented dictionary forms (HANDLES_SEGMENTATION =
  false)
- **Dictionary form required** - Plugin expects deinflected forms (REQUIRES_DICTIONARY_FORM = true)

## Requirements

- Android SDK 29+ (Android 10+)
- Kotlin 1.8+

## Setup

### For Plugin Developers

1. Clone this repository
2. Open the project in Android Studio
3. Build and install the APK:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### For End Users

1. Download the APK from the releases page
2. Install it on your Android device
3. Open Dokuen Reader
4. Go to Settings → Dictionary → Dictionary Source
5. Select "Simple Dictionary Sample"

## How It Works

### Plugin Metadata

The plugin declares required metadata in `AndroidManifest.xml`:

```xml
<meta-data android:name="plugin_name" android:value="Simple Dictionary Sample" />
<meta-data android:name="plugin_version" android:value="1.0.0" />
<meta-data android:name="plugin_author" android:value="Dokuen Reader" />
<meta-data android:name="plugin_description" android:value="A simple dictionary plugin demonstrating basic lookup functionality" />
<meta-data android:name="plugin_license" android:value="Apache 2.0" />
```

### Capabilities

The plugin reports its capabilities to Dokuen:

- **HANDLES_SEGMENTATION**: false - Plugin receives pre-segmented text
- **REQUIRES_DICTIONARY_FORM**: true - Plugin expects deinflected forms
- **SUPPORTED_SOURCE_LANGUAGES**: Japanese (`ja`)
- **SUPPORTED_TARGET_LANGUAGES**: English (`en`)

### Processing Flow

1. **Initialization** - Plugin loads `dictionary.csv` from assets into memory
2. **User taps a word** in Dokuen Reader
3. **Host app segments** the text based on tap selection mode
4. **Host app deinflects** the word to dictionary form (e.g., "食べた" → "食べる")
5. **Plugin receives** the dictionary form via `onLookup()`
6. **Plugin looks up** the word in its loaded dictionary
7. **Plugin returns** `DictionaryResult` with styled definitions

### Dictionary File Format

**Important: CSV is for demonstration only.**

> *This sample uses a CSV file to demonstrate asset loading patterns. Production dictionary plugins
should use a compiled database format like SQLite with Room instead. See
the [Extending This Sample](#extending-this-sample) section for SQLite implementation guidance.*

The dictionary is stored in `app/src/main/assets/dictionary.csv`:

```csv
# Simple Japanese-English Dictionary
# Format: headword,reading,part_of_speech,definition
食べる,たべる,verb,"to eat; to consume"
本,ほん,noun,"book; publication"
見る,みる,verb,"to see; to look; to watch"
```

**Format:**

- Lines starting with `#` are comments
- Empty lines are ignored
- Fields: `headword,reading,part_of_speech,definition`
- Definitions can be quoted to include commas

### Data Model

Each dictionary entry contains:

- **Headword** - The word being defined (with ruby annotations for reading)
- **Pronunciation** - Array of pronunciation strings
- **Body** - Definition text with styling (bold part of speech, regular definition)

Example entry structure:

```kotlin
DictionaryEntry(
    headword = "食べる",
    pronunciation = arrayOf(RubySpan(0, 1, "た")),
    body = StyledText(
        text = "verb\nto eat; to consume",
        styledSpans = arrayOf(
            StyledSpan(0, 4, InlineStyle(bold = true, italic = true))
        )
    )
)
```

## Dictionary Contents

The sample includes 30 common Japanese words covering:

- Basic verbs (食べる, 見る, 行く, 来る, 読む, 書く, 話す, 聞く)
- Common nouns (本, 水, 学校, 先生, 学生, 友達, 家)
- Animals (犬, 猫, 魚)
- Food categories (肉, 野菜, 果物)
- Time words (朝, 昼, 夜, 今日, 明日, 昨日, 時間, 分, 秒)

## Code Structure

```
app/src/main/
├── assets/
│   └── dictionary.csv                  # Dictionary data file
└── java/.../simple/
    └── SimpleDictionaryPluginService.kt # Main plugin implementation

Key methods:
- onInitialize()                        # Load dictionary from assets
- onLookup()                           # Perform dictionary lookup
- parseCsvLine()                       # Parse CSV with quoted fields
- createDictionaryEntry()              # Format entry with styling
- onShutdown()                         # Clean up resources
```

## Error Handling

The plugin handles several error cases:

- **Initialization failure**: Returns `InitResult.failure()` if dictionary file cannot be loaded
- **Word not found**: Throws `DictionaryException` with `WORD_NOT_FOUND` error code
- **Malformed CSV lines**: Logs warning and skips the line

## Performance

**CSV-based (this sample):**

- **Initialization**: ~10-50ms (loads 30 entries from CSV)
- **Lookup**: < 1ms (in-memory HashMap lookup)
- **Memory usage**: < 100 KB for dictionary data
- **Scalability**: Poor - initialization time grows linearly with file size

**SQLite-based (recommended for production):**

- **Initialization**: ~1-5ms (just opens database file)
- **Lookup**: < 1ms (indexed query)
- **Memory usage**: ~1-2 MB (database cache, not full dictionary)
- **Scalability**: Excellent - handles millions of entries efficiently

## Extending This Sample

**Do not use CSV for production plugins.**

To create a production dictionary plugin, you **must** use a proper database:

### 1. Use SQLite with Room (Recommended)

Room is Android's recommended database library, providing compile-time SQL verification and
type-safe queries.

**Step 1: Add Room dependencies**

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}

plugins {
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

**Step 2: Define your database schema**

```kotlin
@Entity(tableName = "dictionary")
data class DictionaryEntry(
    @PrimaryKey val headword: String,
    val reading: String,
    val partOfSpeech: String,
    val definition: String,
    @ColumnInfo(name = "search_key") val searchKey: String // For indexing
)

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE headword = :word LIMIT 1")
    suspend fun lookup(word: String): DictionaryEntry?

    @Query("SELECT * FROM dictionary WHERE search_key LIKE :prefix || '%' LIMIT 20")
    suspend fun prefixSearch(prefix: String): List<DictionaryEntry>
}

@Database(entities = [DictionaryEntry::class], version = 1)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
}
```

**Step 3: Pre-build your database**

Build your SQLite database offline using a script, then include it in assets:

```python
# build_database.py
import sqlite3
import csv

db = sqlite3.connect('dictionary.db')
cursor = db.cursor()

cursor.execute('''
    CREATE TABLE dictionary (
        headword TEXT PRIMARY KEY,
        reading TEXT NOT NULL,
        partOfSpeech TEXT NOT NULL,
        definition TEXT NOT NULL,
        search_key TEXT NOT NULL
    )
''')

cursor.execute('CREATE INDEX idx_search_key ON dictionary(search_key)')

with open('dictionary.csv', 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    next(reader)  # Skip header
    for row in reader:
        cursor.execute(
            'INSERT INTO dictionary VALUES (?, ?, ?, ?, ?)',
            (row[0], row[1], row[2], row[3], row[0])  # search_key = headword
        )

db.commit()
db.close()
```

**Step 4: Load database in onInitialize()**

```kotlin
private lateinit var database: DictionaryDatabase

override suspend fun onInitialize(config: Bundle?): InitResult {
    return withContext(Dispatchers.IO) {
        try {
            // Copy database from assets to internal storage on first run
            val dbFile = File(applicationContext.getDatabasePath("dictionary.db").path)
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                applicationContext.assets.open("dictionary.db").use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Open database with Room
            database = Room.databaseBuilder(
                applicationContext,
                DictionaryDatabase::class.java,
                "dictionary.db"
            ).build()

            InitResultFactory.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load database", e)
            InitResultFactory.failure("Failed to load database: ${e.message}")
        }
    }
}

override suspend fun onLookup(
    contextText: String,
    cursorStartIndex: Int,
    cursorEndIndex: Int
): DictionaryResult {
    val word = contextText.substring(cursorStartIndex, cursorEndIndex)

    val entry = database.dictionaryDao().lookup(word)
        ?: throw DictionaryException(
            DictionaryErrorCode.WORD_NOT_FOUND,
            "Word not found"
        )

    return DictionaryResult(entries = arrayOf(formatEntry(entry)))
}

override fun onShutdown() {
    if (::database.isInitialized) {
        database.close()
    }
}
```

### 2. Alternative: Use Raw SQLite (No Room)

If you prefer not to use Room, you can use Android's built-in SQLite support:

```kotlin
private lateinit var db: SQLiteDatabase

override suspend fun onInitialize(config: Bundle?): InitResult {
    return withContext(Dispatchers.IO) {
        try {
            // Copy database from assets
            val dbFile = File(applicationContext.filesDir, "dictionary.db")
            if (!dbFile.exists()) {
                applicationContext.assets.open("dictionary.db").use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Open database
            db = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            InitResultFactory.success()
        } catch (e: Exception) {
            InitResultFactory.failure("Failed to load database: ${e.message}")
        }
    }
}

override suspend fun onLookup(
    contextText: String,
    cursorStartIndex: Int,
    cursorEndIndex: Int
): DictionaryResult = withContext(Dispatchers.IO) {
    val word = contextText.substring(cursorStartIndex, cursorEndIndex)

    db.query(
        "dictionary",
        arrayOf("headword", "reading", "partOfSpeech", "definition"),
        "headword = ?",
        arrayOf(word),
        null, null, null, "1"
    ).use { cursor ->
        if (!cursor.moveToFirst()) {
            throw DictionaryException(
                DictionaryErrorCode.WORD_NOT_FOUND,
                "Word not found"
            )
        }

        val entry = DictEntry(
            headword = cursor.getString(0),
            reading = cursor.getString(1),
            partOfSpeech = cursor.getString(2),
            definition = cursor.getString(3)
        )

        DictionaryResult(entries = arrayOf(formatEntry(entry)))
    }
}
```

### 3. Additional Production Features

Once you have a database, add these features:

1. **Full-text search** - Use SQLite FTS5 for searching within definitions
2. **Prefix matching** - Support typing partial words
3. **Multiple definitions** - Return all senses for polysemous words
4. **Metadata** - Include JLPT level, frequency rank, example sentences
5. **Caching** - Use Room's in-memory cache or implement LRU cache
6. **Database updates** - Download and install updated dictionaries
7. **Compression** - Use SQLite's built-in compression for large text fields

## License

Apache 2.0 - See LICENSE file for details
