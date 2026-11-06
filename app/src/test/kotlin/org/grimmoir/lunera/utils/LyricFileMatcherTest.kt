package org.grimmoir.lunera.utils
import org.junit.Assert.*
import org.junit.Test
class LyricFileMatcherTest {
    @Test fun exactTimedFileHasPriorityOverPlainText() {
        assertEquals(listOf("Song.LRC", "Song.txt"), LyricFileMatcher.candidates("Song.mp3", listOf("Song.txt", "Song.LRC", "Other.lrc")))
    }
    @Test fun punctuationAndCommonLabelsDoNotPreventMatching() {
        assertEquals(listOf("Artist - My Song (Lyrics).lrc"), LyricFileMatcher.candidates("Artist_My Song [Official Audio].mp3", listOf("Artist - My Song (Lyrics).lrc", "Other song.lrc")))
    }
    @Test fun unrelatedFileIsNeverGuessed() {
        assertTrue(LyricFileMatcher.candidates("My Song.mp3", listOf("Your Song.lrc")).isEmpty())
    }
    @Test fun twoSimilarButDifferentTitlesNeedManualChoice() {
        assertTrue(LyricFileMatcher.candidates("one two three four five.mp3", listOf("one two three four five six.lrc", "one two three four five seven.lrc")).isEmpty())
    }
    @Test fun unicodeAndCaseAreSupported() {
        assertEquals(listOf("夜に駆ける (Lyrics).lrc"), LyricFileMatcher.candidates("夜に駆ける.mp3", listOf("夜に駆ける (Lyrics).lrc")))
    }
    @Test fun instrumentalVersionDoesNotMatchVocalRecording() {
        assertTrue(LyricFileMatcher.candidates("Artist Song Instrumental.mp3", listOf("Artist Song.lrc")).isEmpty())
    }
    @Test fun indexedNearMatchWorksInALargeFolder() {
        val index = LyricFileMatcher.Index((1..10000).map { "Artist Song $it (Lyrics).lrc" })
        assertEquals(listOf("Artist Song 9999 (Lyrics).lrc"), index.candidates("Artist_Song_9999 [Official Audio].mp3"))
    }
 @Test fun titleOnlyFileCanMatchAnUnambiguousArtistPrefix() { assertEquals(listOf("Happy Nation.lrc"),LyricFileMatcher.candidates("Ace of Base - Happy Nation.mp3",listOf("Happy Nation.lrc"))) }
 @Test fun liveVersionDoesNotMatchTheStudioLyrics() { assertTrue(LyricFileMatcher.candidates("Artist Happy Nation Live.mp3",listOf("Artist Happy Nation.lrc")).isEmpty()) }
}
