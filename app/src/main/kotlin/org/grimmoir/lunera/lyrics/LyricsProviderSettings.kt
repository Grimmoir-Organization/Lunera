package org.grimmoir.lunera.lyrics
import org.grimmoir.lunera.utils.get

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import org.grimmoir.lunera.constants.EnableBetterLyricsKey
import org.grimmoir.lunera.constants.EnableKugouKey
import org.grimmoir.lunera.constants.EnableLrcLibKey
import org.grimmoir.lunera.constants.EnableMusixmatchKey
import org.grimmoir.lunera.constants.EnablePaxsenixKey
import org.grimmoir.lunera.constants.EnableSimpMusicKey
import org.grimmoir.lunera.constants.EnableUnisonKey
import org.grimmoir.lunera.constants.EnableYouLyPlusKey
import org.grimmoir.lunera.constants.PreferredLyricsProvider
import org.grimmoir.lunera.utils.dataStore

object LyricsProviderSettings {
    fun idForLegacyPreference(provider: PreferredLyricsProvider): String = when (provider) {
        PreferredLyricsProvider.LRCLIB -> "LrcLib"
        PreferredLyricsProvider.KUGOU -> "Kugou"
        PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
        PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
        PreferredLyricsProvider.YOULYPLUS -> "YouLyPlus"
        PreferredLyricsProvider.PAXSENIX -> "Paxsenix"
    }

    fun enabledKey(id: String): Preferences.Key<Boolean> = when (id) {
        "BetterLyrics" -> EnableBetterLyricsKey
        "Kugou" -> EnableKugouKey
        "LrcLib" -> EnableLrcLibKey
        "Musixmatch" -> EnableMusixmatchKey
        "Paxsenix" -> EnablePaxsenixKey
        "SimpMusic" -> EnableSimpMusicKey
        "Unison" -> EnableUnisonKey
        "YouLyPlus" -> EnableYouLyPlusKey
        else -> booleanPreferencesKey("enableLyrics$id")
    }

    fun isEnabled(context: Context, provider: LyricsProvider): Boolean =
        context.dataStore[enabledKey(provider.id)] ?: true

    fun initialize(context: Context, provider: LyricsProvider) {
        provider.initialize(context.applicationContext)
    }
}
