package org.grimmoir.lunera.ui.screens.library

data class SongFocusPosition(val songId: String, val rowIndex: Int) {
    companion object {
        /** The list has one header. Never focus a different song when the active one is absent. */
        fun find(ids: List<String>, current: String?, lastPlayed: String?): SongFocusPosition? {
            val id = current ?: lastPlayed ?: return null
            val index = ids.indexOf(id)
            return if (index < 0) null else SongFocusPosition(id, index + 1)
        }
    }
}
