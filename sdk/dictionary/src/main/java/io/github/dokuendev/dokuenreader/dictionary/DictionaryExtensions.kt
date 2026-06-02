package io.github.dokuendev.dokuenreader.dictionary

/**
 * Extension functions and convenience constructors for Dictionary API types.
 * These provide idiomatic Kotlin APIs for better ergonomics.
 *
 * See the corresponding .aidl files for detailed field documentation.
 */

// ============================================================================
// Constructor-style factory functions
// ============================================================================

/**
 * Creates a DictionaryEntry with the specified properties.
 *
 * @param headword The dictionary form of the word being defined
 * @param pronunciation Optional ruby annotations for the headword (e.g., furigana)
 * @param body The complete definition content with formatting
 */
fun DictionaryEntry(
    headword: String,
    pronunciation: Array<RubySpan>? = null,
    body: StyledText
): DictionaryEntry = DictionaryEntry().apply {
    this.headword = headword
    this.pronunciation = pronunciation
    this.body = body
}

/**
 * Creates a StyledText with the specified properties.
 *
 * @param text The plain text content
 * @param blockSpans Optional block-level layout containers applied to character ranges
 * @param styledSpans Optional inline style annotations applied to character ranges
 * @param rubySpans Optional ruby text annotations for character ranges
 */
fun StyledText(
    text: String,
    blockSpans: Array<BlockSpan>? = null,
    styledSpans: Array<StyledSpan>? = null,
    rubySpans: Array<RubySpan>? = null
): StyledText = StyledText().apply {
    this.text = text
    this.blockSpans = blockSpans
    this.styledSpans = styledSpans
    this.rubySpans = rubySpans
}

/**
 * Creates a DictionaryResult with the specified entries.
 *
 * @param entries The list of dictionary entries matching the query
 */
fun DictionaryResult(
    entries: Array<DictionaryEntry>
): DictionaryResult = DictionaryResult().apply {
    this.entries = entries
}

/**
 * Creates a RubySpan with the specified properties.
 *
 * @param startIndex Start index in the text (inclusive)
 * @param endIndex End index in the text (exclusive)
 * @param rubyText The ruby text to display above/beside the base text
 */
fun RubySpan(
    startIndex: Int,
    endIndex: Int,
    rubyText: String
): RubySpan = RubySpan().apply {
    this.startIndex = startIndex
    this.endIndex = endIndex
    this.rubyText = rubyText
}

/**
 * Creates a StyledSpan with the specified properties.
 *
 * @param startIndex Start index in the text (inclusive)
 * @param endIndex End index in the text (exclusive)
 * @param style The style to apply to this range
 */
fun StyledSpan(
    startIndex: Int,
    endIndex: Int,
    style: InlineStyle
): StyledSpan = StyledSpan().apply {
    this.startIndex = startIndex
    this.endIndex = endIndex
    this.style = style
}

/**
 * Creates a BlockSpan with the specified properties.
 *
 * @param startIndex Start index in the text (inclusive)
 * @param endIndex End index in the text (exclusive)
 * @param blockType Block type: 0=Normal, 1=List Item, 2=Box, 3=Table
 * @param indentLevel Indent depth (0 = no indent, 1+ = indent level)
 * @param listMarker Optional list marker string (only used for blockType=1)
 * @param backgroundColor Background color in ARGB format (0 = no background)
 */
fun BlockSpan(
    startIndex: Int,
    endIndex: Int,
    blockType: Int = 0,
    indentLevel: Int = 0,
    listMarker: String? = null,
    backgroundColor: Int = 0
): BlockSpan = BlockSpan().apply {
    this.startIndex = startIndex
    this.endIndex = endIndex
    this.blockType = blockType
    this.indentLevel = indentLevel
    this.listMarker = listMarker
    this.backgroundColor = backgroundColor
}

// ============================================================================
// Constants
// ============================================================================

/**
 * Block type constant for normal paragraphs.
 */
const val BLOCK_TYPE_NORMAL = 0

/**
 * Block type constant for list items.
 */
const val BLOCK_TYPE_LIST_ITEM = 1

/**
 * Block type constant for box containers with background.
 */
const val BLOCK_TYPE_BOX = 2

/**
 * Block type constant for table blocks.
 */
const val BLOCK_TYPE_TABLE = 3

/**
 * Creates an InlineStyle with the specified properties.
 *
 * @param bold Whether the text should be bold
 * @param italic Whether the text should be italic
 * @param fontSize Relative font size multiplier (1.0 = normal, 0.8 = smaller, 1.2 = larger)
 * @param foregroundColor Foreground (text) color in ARGB format (0 = use default theme color)
 * @param textBackgroundColor Text background color in ARGB format (0 = no background)
 * @param hoverText If non-null, this span represents clickable pop-up hover text
 * @param linkUrl If non-null, this span represents a link with the specified URL
 */
fun InlineStyle(
    bold: Boolean = false,
    italic: Boolean = false,
    fontSize: Float = 1.0f,
    foregroundColor: Int = 0,
    textBackgroundColor: Int = 0,
    hoverText: String? = null,
    linkUrl: String? = null
): InlineStyle = InlineStyle().apply {
    this.bold = bold
    this.italic = italic
    this.fontSize = fontSize
    this.foregroundColor = foregroundColor
    this.textBackgroundColor = textBackgroundColor
    this.hoverText = hoverText
    this.linkUrl = linkUrl
}
