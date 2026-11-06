package org.grimmoir.lunera.lyrics
import org.grimmoir.lunera.lyrics.bitchord.*
import java.util.Locale

internal fun List<LyricLine>.toLuneraLyrics(): String {
    if (none { it.timeMs > 0 || it.words.isNotEmpty() }) return joinToString("\n") { it.text }
    fun stamp(ms: Long) = String.format(Locale.ROOT, "%02d:%02d.%03d", ms / 60_000, ms / 1000 % 60, ms % 1000)
    fun LyricLine.row(background: Boolean = false): String = buildString {
        append("[${stamp(timeMs.coerceAtLeast(0))}]")
        if (background) append("{bg}")
        if (alignment == LyricAlignment.End) append("{agent:v2}")
        if (words.isEmpty()) append(text) else words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            append("<${stamp(word.startMs)}|${stamp(word.endMs)}>${word.text}")
        }
    }
    return flatMap { line -> listOfNotNull(line.row(), line.background?.row(true)) }.joinToString("\n")
}

