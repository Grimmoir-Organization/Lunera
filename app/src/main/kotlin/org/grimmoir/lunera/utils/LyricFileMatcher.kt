package org.grimmoir.lunera.utils

import java.text.Normalizer
import java.util.Locale

/** Conservative filename matching within one song folder; ambiguous matches need manual selection. */
object LyricFileMatcher {
    private val versions = setOf("instrumental", "remix", "live", "cover", "karaoke", "slowed", "sped", "remaster")
    private val formats = listOf("lrc", "srt", "vtt", "txt")
    private fun stem(name: String) = name.substringBeforeLast('.', name)
    private val labels = Regex("\\b(official|lyrics?|audio|video|visualizer|hd|hq|mp3|320kbps|128kbps)\\b")
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val spaces = Regex("\\s+")
    private val names = object : LinkedHashMap<String, Set<String>>(256, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Set<String>>?) = size > 8192
    }
    private fun normalized(name: String): Set<String> = synchronized(names) {
        names.getOrPut(name) {
            Normalizer.normalize(stem(name), Normalizer.Form.NFKC).lowercase(Locale.ROOT)
                .replace(labels, " ").replace(punctuation, " ").trim().split(spaces).filter { it.isNotBlank() }.toSet()
        }
    }
    private fun formatOrder(name: String) = formats.indexOf(name.substringAfterLast('.').lowercase(Locale.ROOT))
    fun candidates(audioName: String, names: List<String>): List<String> = Index(names).candidates(audioName)

    /** Built once per folder during a scan, so common near-matches do not rescan every lyric name. */
    class Index(names: List<String>) {
        private val files = names.filter { it.substringAfterLast('.', "").lowercase(Locale.ROOT) in formats }
        private val exactNames = files.groupBy { stem(it).lowercase(Locale.ROOT) }
        private val words = files.associateWith { normalized(it) }
        private val normalizedNames = files.groupBy { words.getValue(it) }
        fun candidates(audioName: String): List<String> {
            exactNames[stem(audioName).lowercase(Locale.ROOT)]?.let { return it.sortedBy(::formatOrder) }
            val wanted = normalized(audioName)
            if (wanted.isEmpty()) return emptyList()
            normalizedNames[wanted]?.let { return it.sortedBy(::formatOrder) }
            val scored = files.map { name ->
                val candidate = words.getValue(name)
                val intersection = wanted.count { it in candidate }
                val union = wanted.size + candidate.size - intersection
                val conflictingVersion = wanted.intersect(versions) != candidate.intersect(versions)
                val subset = minOf(wanted.size, candidate.size) >= 2 && intersection == minOf(wanted.size, candidate.size) && kotlin.math.abs(wanted.size - candidate.size) <= 3
                val score = if (conflictingVersion) 0.0 else if (subset) .9 else intersection.toDouble() / union.coerceAtLeast(1)
                name to score
            }.filter { it.second >= .8 }.sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { formatOrder(it.first) })
            val best = scored.firstOrNull() ?: return emptyList()
            val competing = scored.firstOrNull { words.getValue(it.first) != words.getValue(best.first) }
            if (competing != null && best.second - competing.second < .15) return emptyList()
            return scored.filter { words.getValue(it.first) == words.getValue(best.first) }.map { it.first }
        }
    }
}
