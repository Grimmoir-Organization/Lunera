package org.grimmoir.lunera
import org.junit.Assert.*
import org.junit.Test
import org.grimmoir.lunera.utils.withinSelectedFolder
class LuneraFolderRulesTest {
    @Test fun selectedFolderIncludesChildren() { assertTrue(withinSelectedFolder("Music/Rock/Live", "Music/Rock")); assertTrue(withinSelectedFolder("Music/Rock/", "Music/Rock")) }
    @Test fun similarlyNamedFolderIsExcluded() { assertFalse(withinSelectedFolder("Music/Rockabilly", "Music/Rock")) }
    @Test fun noRootDoesNotSelectAllStorage() { assertFalse(withinSelectedFolder("Music", "")) }
}
