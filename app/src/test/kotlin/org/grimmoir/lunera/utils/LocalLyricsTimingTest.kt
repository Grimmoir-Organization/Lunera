package org.grimmoir.lunera.utils
import org.junit.Assert.*
import org.junit.Test
class LocalLyricsTimingTest {
    @Test fun timestampsUseCentisecondsAndClampNegativeTime() {
        assertEquals("[00:00.00]", LocalLyricsTiming.timestamp(-10))
        assertEquals("[01:02.34]", LocalLyricsTiming.timestamp(62345))
    }
    @Test fun restampingReplacesOldTimestamp() { assertEquals("[00:05.00]Hello", LocalLyricsTiming.stamp("[00:01.20]Hello", 5000)) }
    @Test fun repeatedLrcTimestampsCanBeRestamped() { assertEquals("[00:05.00]Hello", LocalLyricsTiming.stamp("[00:01.20][00:02.30]Hello", 5000)) }
    @Test fun srtCuesBecomeLrc() {
        assertEquals("[00:01.25]Hello\n[01:02.00]World", LocalLyricsTiming.normalize("1\n00:00:01,250 --> 00:00:02,000\n<i>Hello</i>\n\n2\n00:01:02,000 --> 00:01:03,000\nWorld\n"))
    }
    @Test fun vttCuesBecomeLrc() { assertEquals("[00:03.50]Hello", LocalLyricsTiming.normalize("WEBVTT\n\n00:03.500 --> 00:05.000\nHello")) }
    @Test fun numericLyricLinesAreRetained() { assertEquals("[00:01.00]1234", LocalLyricsTiming.normalize("1\n00:00:01,000 --> 00:00:02,000\n1234")) }
    @Test fun existingLrcStaysUnchanged() { assertEquals("[00:01.00]Hello", LocalLyricsTiming.normalize("[00:01.00]Hello")) }
    @Test fun plainTextCanBeManuallyTimed() { assertEquals("Hello\nWorld", LocalLyricsTiming.normalize("Hello\r\nWorld")); assertEquals("[00:02.00]World", LocalLyricsTiming.stamp("World",2000)) }
}
