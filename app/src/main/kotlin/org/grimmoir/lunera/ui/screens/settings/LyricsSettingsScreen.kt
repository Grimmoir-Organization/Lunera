/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.grimmoir.lunera.R
import org.grimmoir.lunera.constants.AppleMusicLyricsBlurKey
import org.grimmoir.lunera.constants.LyricsGlowEffectKey
import org.grimmoir.lunera.constants.LyricsStandardBlurKey
import org.grimmoir.lunera.ui.component.IosSettingsGroup
import org.grimmoir.lunera.ui.component.IosSettingsItem
import org.grimmoir.lunera.ui.ios.compat.Switch
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.utils.rememberPreference
import org.grimmoir.lunera.utils.rememberEnumPreference
import org.grimmoir.lunera.constants.LyricsAnimationStyle
import org.grimmoir.lunera.constants.LyricsAnimationStyleKey
import org.grimmoir.lunera.ui.component.EnumDialog
import org.grimmoir.lunera.ui.component.lyricsStyleLabel
import androidx.compose.runtime.saveable.rememberSaveable

/** Lyrics look: glow and the two blur modes. Kept apart from the animation style and text layout. */
@Composable
fun LyricsSettingsScreen(navController: NavController) {
    val (animationStyle, selectAnimationStyle) = rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.FLAMINGO)
    var showStyleDialog by rememberSaveable { mutableStateOf(false) }
    if (showStyleDialog) {
        EnumDialog(
            onDismiss = { showStyleDialog = false },
            onSelect = { selectAnimationStyle(it); showStyleDialog = false },
            title = stringResource(R.string.lyrics_animation_style),
            current = animationStyle,
            values = LyricsAnimationStyle.entries,
            valueText = { lyricsStyleLabel(it) },
        )
    }
    val (glow, onGlowChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = true)
    val (appleBlur, onAppleBlurChange) = rememberPreference(AppleMusicLyricsBlurKey, defaultValue = true)
    val (standardBlur, onStandardBlurChange) = rememberPreference(LyricsStandardBlurKey, defaultValue = false)

    SettingsPage(navController = navController, title = stringResource(R.string.lyrics), horizontalPadding = 16.dp) {
        IosSettingsGroup(title = stringResource(R.string.lyrics_animation_style), items = listOf(
            IosSettingsItem(
                icon = painterResource(R.drawable.lyrics),
                title = { Text(lyricsStyleLabel(animationStyle)) },
                description = { Text(stringResource(R.string.lyrics_centered_scroll_description)) },
                onClick = { showStyleDialog = true },
            ),
        ))
        LyricsFontSettings()
        IosSettingsGroup(
            title = "Lyrics effects",
            items = listOf(
                IosSettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                    trailingContent = { Switch(checked = glow, onCheckedChange = onGlowChange) },
                    onClick = { onGlowChange(!glow) },
                ),
                IosSettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.apple_music_lyrics_blur)) },
                    description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                    trailingContent = { Switch(checked = appleBlur, onCheckedChange = onAppleBlurChange) },
                    onClick = { onAppleBlurChange(!appleBlur) },
                ),
                IosSettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.standard_lyrics_blur)) },
                    description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                    trailingContent = { Switch(checked = standardBlur, onCheckedChange = onStandardBlurChange) },
                    onClick = { onStandardBlurChange(!standardBlur) },
                ),
            ),
        )
        Spacer(Modifier.height(32.dp))
    }
}
