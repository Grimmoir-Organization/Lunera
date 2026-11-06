package org.grimmoir.lunera.ui.menu
import org.junit.Test
import org.junit.Assert.assertNotNull
import com.composables.sfsymbols.SfSymbol
class LocalMenuSymbolsTest {
    @Test fun localMenuAndPlaylistSymbolsExistInBundledLibrary() {
        listOf("heart", "heart.fill", "music.note.list", "text.line.first.and.arrowtriangle.forward", "text.append", "list.bullet", "text.badge.plus", "info.circle", "pencil", "folder", "folder.fill", "square.and.arrow.up", "trash", "arrow.up", "minus.circle").forEach {
            assertNotNull("Unknown SF Symbol would crash a visible local screen: $it", SfSymbol.named(it))
        }
    }
}
