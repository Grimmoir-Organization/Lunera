package org.grimmoir.lunera.lyrics
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.grimmoir.lunera.models.MediaMetadata
import org.grimmoir.lunera.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
class LyricsHelper @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun getLyrics(mediaMetadata: MediaMetadata, initialLyrics: LyricsWithProvider? = null, onUpgrade: suspend (LyricsWithProvider) -> Unit = {}): LyricsWithProvider {
        if (initialLyrics?.provider?.startsWith("Manual") == true) return initialLyrics
        val local = org.grimmoir.lunera.utils.LocalLyricFiles.forSong(context, mediaMetadata.id)
            ?: org.grimmoir.lunera.utils.LocalAudioTags.lyrics(context, Uri.parse(mediaMetadata.id))
        return LyricsWithProvider(local ?: initialLyrics?.lyrics ?: LYRICS_NOT_FOUND, if (local != null) "Local file" else initialLyrics?.provider ?: "Local")
    }
    suspend fun getAllLyrics(mediaId: String, songTitle: String, songArtists: String, duration: Int, album: String? = null, callback: (LyricsResult) -> Unit) = Unit
    fun cancelCurrentLyricsJob() = Unit
}
data class LyricsResult(val providerId: String, val providerName: String, val lyrics: String)
data class LyricsWithProvider(val lyrics: String, val provider: String)
