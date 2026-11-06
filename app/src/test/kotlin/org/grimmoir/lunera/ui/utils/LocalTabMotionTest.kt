package org.grimmoir.lunera.ui.utils
import org.junit.Assert.*
import org.junit.Test
class LocalTabMotionTest {
    @Test fun songsFoldersAndSearchUseTabMotion() { assertTrue(LocalTabMotion.isChange("songs","library"));assertTrue(LocalTabMotion.isChange("library","search_input"));assertTrue(LocalTabMotion.isChange("search_input","playlists")) }
    @Test fun detailAndSettingsPagesKeepTheirOwnMotion() { assertFalse(LocalTabMotion.isChange("songs","settings"));assertFalse(LocalTabMotion.isChange("library","local_folder/Music")) }
    @Test fun reselectingATabIsNotAPageTransition() { assertFalse(LocalTabMotion.isChange("songs","songs"));assertFalse(LocalTabMotion.isChange(null,"home")) }
}
