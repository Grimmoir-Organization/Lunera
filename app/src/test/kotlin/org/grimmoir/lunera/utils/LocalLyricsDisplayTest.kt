package org.grimmoir.lunera.utils
import org.junit.Assert.*
import org.junit.Test
class LocalLyricsDisplayTest {
 @Test fun sectionHeadingsAreNotTimestamps() { assertFalse(LocalLyricsTiming.hasTimestamps("[Chorus]\nA line")) }
 @Test fun metadataBeforeRealLrcStillCountsAsSynced() { assertTrue(LocalLyricsTiming.hasTimestamps("[ar:Artist]\n[00:12.50]A line")) }
 @Test fun copiedPageChromeIsRemovedButVerseTextRemains() { assertEquals("[Intro: Choir]\nLaudate omnes",LocalLyricsTiming.cleanPlainText("14 ContributorsHappy Nation Lyrics [Intro: Choir]\nLaudate omnes\nEmbed")) }
 @Test fun manualLyricsAndSectionLabelsArePreserved() { val text="[Verse 1]\nA line\n\n[Chorus]\nAnother line";assertEquals(text,LocalLyricsTiming.cleanPlainText(text)) }
}
