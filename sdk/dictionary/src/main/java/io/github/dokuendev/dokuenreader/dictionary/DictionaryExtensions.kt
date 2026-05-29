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
 * @param styledSpans Optional style annotations applied to character ranges
 * @param rubySpans Optional ruby text annotations for character ranges
 */
fun StyledText(
    text: String,
    styledSpans: Array<StyledSpan>? = null,
    rubySpans: Array<RubySpan>? = null
): StyledText = StyledText().apply {
    this.text = text
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

// ============================================================================
// Constants
// ============================================================================

/**
 * Sentinel value for [InlineStyle.listItemOrdinal] that marks a bullet list item.
 *
 * When `listItemOrdinal` is set to this value, the item is rendered with a default
 * bullet marker ("•") unless `listMarkerOverride` is set.
 *
 * Note: `listMarkerOverride` only has an effect if `listItemOrdinal` is non-zero (such as [LIST_ITEM_BULLET]).
 * Setting `listItemOrdinal` to `0` (default) takes precedence and means the span is not a list item,
 * regardless of whether `listMarkerOverride` is specified.
 *
 * ```kotlin
 * InlineStyle(listItemOrdinal = LIST_ITEM_BULLET)           // "•" marker
 * InlineStyle(listItemOrdinal = LIST_ITEM_BULLET,
 *             listMarkerOverride = "→ ")                     // "→ " marker
 * ```
 */
const val LIST_ITEM_BULLET = -1

/**
 * Creates an InlineStyle with the specified properties.
 *
 * @param bold Whether the text should be bold
 * @param italic Whether the text should be italic
 * @param fontSize Relative font size multiplier (1.0 = normal, 0.8 = smaller, 1.2 = larger)
 * @param foregroundColor Foreground color in ARGB format (0 = use default theme color)
 * @param backgroundColor Background color in ARGB format (0 = no background)
 * @param listItemOrdinal List item type and ordinal: 0 = not a list item, >0 = numbered position,
 *   [LIST_ITEM_BULLET] (-1) = bullet list item.
 * @param listIndentLevel Indent depth for list items (0 = no indent, 1+ = indent level)
 * @param listMarkerOverride Custom list marker override string (only has an effect if [listItemOrdinal] is non-zero)
 * @param isBlock Whether this span represents a block container
 * @param isTable Whether this span represents a table block
 * @param hoverText If non-null, this span represents clickable pop-up hover text
 * @param linkUrl If non-null, this span represents a link with the specified URL
 */
fun InlineStyle(
    bold: Boolean = false,
    italic: Boolean = false,
    fontSize: Float = 1.0f,
    foregroundColor: Int = 0,
    backgroundColor: Int = 0,
    listItemOrdinal: Int = 0,
    listIndentLevel: Int = 0,
    listMarkerOverride: String? = null,
    isBlock: Boolean = false,
    isTable: Boolean = false,
    hoverText: String? = null,
    linkUrl: String? = null
): InlineStyle = InlineStyle().apply {
    this.bold = bold
    this.italic = italic
    this.fontSize = fontSize
    this.foregroundColor = foregroundColor
    this.backgroundColor = backgroundColor
    this.listItemOrdinal = listItemOrdinal
    this.listIndentLevel = listIndentLevel
    this.listMarkerOverride = listMarkerOverride
    this.isBlock = isBlock
    this.isTable = isTable
    this.hoverText = hoverText
    this.linkUrl = linkUrl
}
