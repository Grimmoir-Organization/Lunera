package org.grimmoir.lunera.ui.screens.library
import org.junit.Assert.*
import org.junit.Test
class SongFocusPositionTest {
    @Test fun firstSongIsBelowTheSingleHeader() { assertEquals(SongFocusPosition("a",1), SongFocusPosition.find(listOf("a","b"),"a",null)) }
    @Test fun currentTrackWinsOverLastPlayed() { assertEquals("b", SongFocusPosition.find(listOf("a","b"),"b","a")?.songId) }
    @Test fun lastPlayedWorksWithNoActiveSession() { assertEquals(2, SongFocusPosition.find(listOf("a","b"),null,"b")?.rowIndex) }
    @Test fun missingCurrentTrackDoesNotSelectAnotherSong() { assertNull(SongFocusPosition.find(listOf("a"),"deleted","a")) }
    @Test fun positionFollowsTheVisibleSortOrder() { assertEquals(3, SongFocusPosition.find(listOf("c","b","a"),"a",null)?.rowIndex) }
    @Test fun longLibrariesResolveTheirLastRow() { val ids=(1..10000).map(Int::toString); assertEquals(10000,SongFocusPosition.find(ids,"10000",null)?.rowIndex) }
    @Test fun emptyLibraryAndNoHistoryHaveNoTarget() { assertNull(SongFocusPosition.find(emptyList(),"a",null)); assertNull(SongFocusPosition.find(listOf("a"),null,null)) }
}
