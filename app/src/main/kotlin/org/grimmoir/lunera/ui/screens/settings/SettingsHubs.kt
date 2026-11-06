/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.grimmoir.lunera.R
import org.grimmoir.lunera.ui.component.liquidGlass
import org.grimmoir.lunera.ui.component.IosPageSheet
import androidx.compose.runtime.mutableStateOf
import org.grimmoir.lunera.ui.component.SymbolIcon
import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.component.shapes.ContinuousRoundedRectangle
import org.grimmoir.lunera.ui.ios.compat.HorizontalDivider
import org.grimmoir.lunera.ui.ios.compat.Icon
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.ui.ios.foundation.IosColors
import org.grimmoir.lunera.ui.ios.foundation.IosMetrics
import org.grimmoir.lunera.ui.ios.foundation.IosTheme
import org.grimmoir.lunera.ui.ios.foundation.iosRowPress

/** What a settings row does when tapped. */
sealed interface SettingsAction {
    data class Route(val route: String) : SettingsAction
    data object PlayerDesign : SettingsAction
    data object LyricsStyle : SettingsAction
}

/** One row: a coloured glyph tile, a name, and where it goes. */
data class SettingsLink(
    val title: String,
    val icon: Int,
    val tint: IosColors.() -> Color,
    val action: SettingsAction,
    val subtitle: String? = null,
    /** Extra words people search with that are not in the title. */
    val keywords: String = "",
)

data class SettingsLinkGroup(val title: String?, val links: List<SettingsLink>)

/** A root category and the page it opens. */
data class SettingsHub(
    val id: String,
    val title: String,
    val icon: Int,
    val tint: IosColors.() -> Color,
    val groups: List<SettingsLinkGroup>,
) {
    val route get() = "settings/hub/$id"
    fun asLink() = SettingsLink(title, icon, tint, SettingsAction.Route(route))
}

private fun route(r: String) = SettingsAction.Route(r)

/** The settings tree: root categories, the pages they open, and every destination under them. */
object SettingsHubs {
    val appearance = SettingsHub("appearance", "Appearance", R.drawable.palette, { systemPurple }, listOf(
        SettingsLinkGroup("Theme & colors", listOf(
            SettingsLink("Themes & colors", R.drawable.palette, { systemPurple }, route("settings/appearance/theme")),
            SettingsLink("Liquid Glass", R.drawable.palette, { systemCyan }, route("settings/appearance/liquidglass")),
            SettingsLink("Player design", R.drawable.play, { systemPink }, SettingsAction.PlayerDesign)
        ))
    ))
    val player = SettingsHub("player", "Player & audio", R.drawable.play, { systemPink }, listOf(
        SettingsLinkGroup(null, listOf(
            SettingsLink("Playback controls", R.drawable.sliders, { systemIndigo }, route("settings/player")),
            SettingsLink("Song transitions", R.drawable.shuffle, { systemPurple }, route("settings/song_transitions"))
        ))
    ))
    val playback = player
    val library = SettingsHub("library", "Library", R.drawable.music_note, { systemOrange }, listOf(
        SettingsLinkGroup(null, listOf(SettingsLink("Music folders", R.drawable.music_note, { systemOrange }, route("settings/player/local_folders"), "Choose folders and scan for changes")))
    ))
    val updates = SettingsHub("updates", "Updates", R.drawable.info, { systemGray }, listOf(
        SettingsLinkGroup(null, listOf(SettingsLink("Updates", R.drawable.info, { systemGray }, route("settings/update"), "Check for new Lunera releases")))
    ))
    val help = SettingsHub("help", "About Lunera", R.drawable.info, { systemGray }, listOf(
        SettingsLinkGroup(null, listOf(SettingsLink("About Lunera", R.drawable.info, { systemGray }, route("settings/about"))))
    ))
    val accounts = library
    val privacy = library
    val all = listOf(appearance, player, library, updates, help)
    fun byId(id: String?) = all.firstOrNull { it.id == id }
}

/** Which settings sheet is open; one app-wide slot so no two screens can each open their own. */
object SettingsSheets {
    var open by mutableStateOf<SettingsAction?>(null)
}

/** Runs a row's action: a page, or one of the sheets that open over the current screen. */
@Composable
fun rememberSettingsActionHandler(navController: NavController): (SettingsAction) -> Unit = { action ->
    when (action) {
        is SettingsAction.Route -> navController.navigate(action.route)
        SettingsAction.PlayerDesign, SettingsAction.LyricsStyle -> SettingsSheets.open = action
    }
}

/** Hosted once at the app root; shows whichever settings sheet is open. */
@Composable
fun SettingsSheetHost(navController: NavController) {
    val close = { SettingsSheets.open = null }
    when (SettingsSheets.open) {
        SettingsAction.PlayerDesign -> IosPageSheet("Player design", close) {
            PlayerDesignSheetContent(onMoreOptions = {
                close()
                navController.navigate("settings/appearance/playertheme")
            })
        }
        SettingsAction.LyricsStyle -> IosPageSheet("Lyrics style", close) { LyricsAnimationSheetContent() }
        else -> Unit
    }
}

/** A category's page: its groups of rows, iOS inset-grouped. */
@Composable
fun SettingsHubScreen(navController: NavController, hub: SettingsHub) {
    val onAction = rememberSettingsActionHandler(navController)
    SettingsPage(navController = navController, title = hub.title, horizontalPadding = 16.dp) {
        SettingsHubBody(hub, onAction)
    }
}

@Composable
fun ColumnScope.SettingsHubBody(hub: SettingsHub, onAction: (SettingsAction) -> Unit) {
    hub.groups.forEachIndexed { index, group ->
        if (index > 0) Spacer(Modifier.height(28.dp))
        SettingsLinkCard(group.title, group.links, onAction)
    }
}

@Composable
fun SettingsLinkCard(
    title: String?,
    links: List<SettingsLink>,
    onAction: (SettingsAction) -> Unit,
    trailing: (SettingsLink) -> String? = { null },
    selected: SettingsLink? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) Text(
            title,
            style = IosTheme.typography.footnote,
            fontWeight = FontWeight.SemiBold,
            color = IosTheme.colors.secondaryLabel,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(IosMetrics.GroupedCornerRadius))
                .then(if (org.grimmoir.lunera.ui.component.hasCustomHomeBackground()) Modifier.liquidGlass(config = org.grimmoir.lunera.ui.component.LocalGlassEffectConfig.current, shape = ContinuousRoundedRectangle(IosMetrics.GroupedCornerRadius), applyEdgeEffects = false) else Modifier.background(IosTheme.colors.secondarySystemGroupedBackground)),
        ) {
            links.forEachIndexed { i, link ->
                if (i > 0) HorizontalDivider(
                    Modifier.padding(start = 58.dp),
                    color = IosTheme.colors.separator,
                    thickness = IosMetrics.SeparatorThickness,
                )
                SettingsLinkRow(link, trailing(link), selected == link) { onAction(link.action) }
            }
        }
    }
}

@Composable
fun SettingsLinkRow(link: SettingsLink, trailing: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.background(IosTheme.colors.systemFill) else Modifier)
            .iosRowPress(onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(ContinuousRoundedRectangle(7.dp))
                .background(link.tint(IosTheme.colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(link.icon), null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(link.title, style = IosTheme.typography.body, color = IosTheme.colors.label)
            if (link.subtitle != null) Text(
                link.subtitle,
                style = IosTheme.typography.footnote,
                color = IosTheme.colors.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(trailing, style = IosTheme.typography.body, color = IosTheme.colors.secondaryLabel, maxLines = 1)
            Spacer(Modifier.width(8.dp))
        }
        SymbolIcon(symbol = sf("chevron.right"), contentDescription = null, size = 14.dp, tint = IosTheme.colors.tertiaryLabel)
    }
}
