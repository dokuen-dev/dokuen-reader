package io.github.dokuendev.dokuenreader.dictionary

fun wordReading(start: Int, end: Int, vararg ruby: RubySpan): WordReading =
    WordReading().apply {
        startIndex = start
        endIndex = end
        this.ruby = arrayOf(*ruby)
    }

fun rubySpan(start: Int, end: Int, text: String): RubySpan =
    RubySpan().apply {
        startIndex = start
        endIndex = end
        rubyText = text
    }

/** Each word as "start-end", followed by " start-end=reading" for each ruby span. */
fun List<WordReading>.describe(): List<String> =
    map { word ->
        "${word.startIndex}-${word.endIndex}" + word.ruby.joinToString("") { " ${it.startIndex}-${it.endIndex}=${it.rubyText}" }
    }
