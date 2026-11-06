/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera
import org.grimmoir.lunera.constants.DefaultOpenTabKey
import org.grimmoir.lunera.extensions.toEnum
import org.grimmoir.lunera.utils.dataStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.grimmoir.lunera.ui.component.HomeImageBackground

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.grimmoir.lunera.constants.AppBackgroundColorKey
import org.grimmoir.lunera.constants.AppBarHeight
import org.grimmoir.lunera.constants.AppleMusicUiKey
import org.grimmoir.lunera.constants.DarkModeKey
import org.grimmoir.lunera.constants.DockedAccessoryHeight
import org.grimmoir.lunera.constants.EnableSettingsPopupKey
import org.grimmoir.lunera.constants.ForceTabletLayoutKey
import org.grimmoir.lunera.constants.IosOverscrollKey
import org.grimmoir.lunera.constants.MiniPlayerHeight
import org.grimmoir.lunera.constants.NavigationBarAnimationSpec
import org.grimmoir.lunera.constants.NavigationBarHeight
import org.grimmoir.lunera.constants.PureBlackKey
import org.grimmoir.lunera.constants.PlayerBackgroundStyle
import org.grimmoir.lunera.constants.PlayerBackgroundStyleKey
import org.grimmoir.lunera.constants.SelectedThemeColorKey
import org.grimmoir.lunera.constants.ShowHistoryButtonKey
import org.grimmoir.lunera.constants.PauseListenHistoryKey
import org.grimmoir.lunera.constants.ShowStatsButtonKey
import org.grimmoir.lunera.constants.SideBarCollapsedKey
import org.grimmoir.lunera.constants.SlimNavBarHeight
import org.grimmoir.lunera.constants.SlimNavBarKey
import org.grimmoir.lunera.constants.UseNewMiniPlayerDesignKey
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.history.HistoryLocalDataSource
import org.grimmoir.lunera.playback.DownloadUtil
import org.grimmoir.lunera.playback.PlayerConnection
import org.grimmoir.lunera.ui.component.AppFloatingNavBar
import org.grimmoir.lunera.ui.component.AppFloatingNowPlayingPill
import org.grimmoir.lunera.ui.component.AppFloatingSideBar
import org.grimmoir.lunera.ui.component.BottomSheetPage
import org.grimmoir.lunera.ui.component.FloatingMiniPlayerWidthFraction
import org.grimmoir.lunera.ui.component.LocalAppBackdrop
import org.grimmoir.lunera.ui.component.LocalAppBackdropFrozen
import org.grimmoir.lunera.ui.component.LocalAppMotionFrozen
import org.grimmoir.lunera.ui.component.LocalAppleMusicUi
import org.grimmoir.lunera.ui.component.LocalBottomSheetPageState
import org.grimmoir.lunera.ui.component.LocalDownloads
import org.grimmoir.lunera.ui.component.LocalGlassEffectConfig
import org.grimmoir.lunera.ui.component.LocalItemPrefs
import org.grimmoir.lunera.ui.component.LocalMenuState
import org.grimmoir.lunera.ui.component.LocalNavSearchState
import org.grimmoir.lunera.ui.component.LocalPooledGlass
import org.grimmoir.lunera.ui.component.LocalSheetPresentation
import org.grimmoir.lunera.ui.component.NavBarSearchInputBar
import org.grimmoir.lunera.ui.component.OverlayMenu
import org.grimmoir.lunera.ui.component.PooledGlass
import org.grimmoir.lunera.ui.component.QueueConflictDialog
import org.grimmoir.lunera.ui.component.QueueReplacedUndoSnackbar
import org.grimmoir.lunera.ui.component.SheetPresentationState
import org.grimmoir.lunera.ui.component.SideBarAccountRow
import org.grimmoir.lunera.ui.component.AppTabViewTopPill
import org.grimmoir.lunera.ui.component.NavMorph
import org.grimmoir.lunera.ui.component.SideBarCollapsedWidth
import org.grimmoir.lunera.ui.component.SideBarContentInset
import org.grimmoir.lunera.ui.component.SideBarMargin
import org.grimmoir.lunera.ui.component.TabletWidthThreshold
import org.grimmoir.lunera.ui.component.backdrop.BackdropEffectScope
import org.grimmoir.lunera.ui.component.backdrop.GlassPoolScale
import org.grimmoir.lunera.ui.component.backdrop.backdrops.layerBackdrop
import org.grimmoir.lunera.ui.component.backdrop.backdrops.rememberLayerBackdrop
import org.grimmoir.lunera.ui.component.backdrop.effects.blur
import org.grimmoir.lunera.ui.component.backdrop.effects.colorControls
import org.grimmoir.lunera.ui.component.backdrop.glassPool
import org.grimmoir.lunera.ui.component.backdrop.rememberGlassPool
import org.grimmoir.lunera.ui.component.floatingtabbar.FloatingTabBarInlineBehavior
import org.grimmoir.lunera.ui.component.floatingtabbar.rememberFloatingTabBarScrollConnection
import org.grimmoir.lunera.ui.component.glassSaturation
import org.grimmoir.lunera.ui.component.iosSheetPresentation
import org.grimmoir.lunera.ui.component.isGlassAllowed
import org.grimmoir.lunera.ui.component.rememberBottomSheetState
import org.grimmoir.lunera.ui.component.rememberItemPrefs
import org.grimmoir.lunera.ui.component.rememberSheetPresentation
import org.grimmoir.lunera.ui.component.shimmer.ShimmerTheme
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.LocalContentColor
import org.grimmoir.lunera.ui.ios.compat.Scaffold
import org.grimmoir.lunera.ui.ios.compat.SnackbarHost
import org.grimmoir.lunera.ui.ios.compat.Surface
import org.grimmoir.lunera.ui.ios.foundation.LocalIosBackdrop
import org.grimmoir.lunera.ui.menu.PlayerMenu
import org.grimmoir.lunera.ui.menu.YouTubeSongMenu
import org.grimmoir.lunera.ui.player.BottomSheetPlayer
import org.grimmoir.lunera.ui.player.InstallPlayerMorphLayers
import org.grimmoir.lunera.ui.screens.Screens
import org.grimmoir.lunera.ui.screens.settings.DarkMode
import org.grimmoir.lunera.ui.screens.settings.NavigationTab
import org.grimmoir.lunera.ui.theme.AppleTokens
import org.grimmoir.lunera.ui.theme.LuneraTheme
import org.grimmoir.lunera.ui.theme.ForcedDarkScope
import org.grimmoir.lunera.ui.theme.DefaultThemeColor
import org.grimmoir.lunera.ui.utils.LocalSharedTransitionScope
import org.grimmoir.lunera.ui.utils.ShowMediaInfo
import org.grimmoir.lunera.ui.utils.appBarScrollBehavior
import org.grimmoir.lunera.ui.utils.rememberIosOverscrollFactory
import org.grimmoir.lunera.ui.utils.resetHeightOffset
import org.grimmoir.lunera.utils.SyncUtils
import org.grimmoir.lunera.utils.get
import org.grimmoir.lunera.utils.rememberEnumPreference
import org.grimmoir.lunera.utils.rememberPreference
import org.grimmoir.lunera.viewmodels.AccountImageState
import com.music.innertube.models.SongItem
import com.valentinilk.shimmer.LocalShimmerTheme
import dev.chrisbanes.haze.HazeState
import org.grimmoir.lunera.constants.TopBarBlurStyle
import org.grimmoir.lunera.constants.TopBarBlurStyleKey
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder

private const val DIAG_DISABLE_BACKDROP = false
private const val USE_SHARED_GLASS_POOL = true

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Composable
internal fun AppRootContent(
    activity: MainActivity,
    intentRouter: AppIntentRouter,
    playerConnection: PlayerConnection?,
    database: dagger.Lazy<MusicDatabase>,
    downloadUtil: dagger.Lazy<DownloadUtil>,
    syncUtils: dagger.Lazy<SyncUtils>,
    historyDataSource: dagger.Lazy<HistoryLocalDataSource>,
) {
    AppStartupEffects(activity, playerConnection)

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT,
    )
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val (wallpaperEnabled) = rememberPreference(org.grimmoir.lunera.constants.HomeBackgroundEnabledKey, false)
    val (wallpaperPath) = rememberPreference(org.grimmoir.lunera.constants.HomeBackgroundPathKey, "")
    val hasWallpaper = wallpaperEnabled && wallpaperPath.isNotBlank()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme, hasWallpaper) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val artworkChromeDark = useDarkTheme || LocalAmbientDarkChrome.current.value
    LaunchedEffect(artworkChromeDark) {
        org.grimmoir.lunera.utils.setSystemBarAppearance(activity.window, artworkChromeDark)
    }

    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
        pureBlackEnabled && useDarkTheme
    }

    // Keep artwork tint local so track changes do not recompute the app theme.
    val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
    val themeColor = org.grimmoir.lunera.ui.theme.rememberLuneraAccent(useDarkTheme)

    LuneraTheme(
        darkTheme = useDarkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor,
    ) {
        val (appBackgroundColorInt) = rememberPreference(AppBackgroundColorKey, defaultValue = 0)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // Base color visible wherever a screen does not draw its own background.
                .background(
                    when {
                        appBackgroundColorInt != 0 -> Color(appBackgroundColorInt)
                        pureBlack -> Color.Black
                        else -> AppTheme.colorScheme.surface
                    }
                )
        ) {
            val focusManager = LocalFocusManager.current
            val density = LocalDensity.current
            val configuration = LocalWindowInfo.current
            val cutoutInsets = WindowInsets.displayCutout
            val windowsInsets = WindowInsets.systemBars
            val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
            val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            val navController = rememberNavController()

            val navigationArtwork = remember(navController) { org.grimmoir.lunera.ui.utils.NavigationArtworkState() }
            InstallMorphNavListener(navController, navigationArtwork)

            val accountImageUrl by AccountImageState.url.collectAsStateWithLifecycle()
            val (sidebarAccountName) = rememberPreference(org.grimmoir.lunera.constants.AccountNameKey, "")
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("songs") }

            val navItems = rememberAppNavItems()
            val localOnlyMode = navItems.localOnlyMode
            val navigationItems = navItems.navigationItems
            val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
            val (appleMusicUi) = rememberPreference(AppleMusicUiKey, defaultValue = true)
            val floatingNavigationItems = navItems.floatingNavigationItems
            // Expand only at the list top; collapse as soon as the list scrolls away.
            val floatingNavBarScrollConnection = rememberFloatingTabBarScrollConnection(
                inlineBehavior = FloatingTabBarInlineBehavior.ExpandedAtTopOnly,
            )
            val glassConfigState = rememberGlassConfigState(artworkChromeDark)
            val glassEffectConfig = glassConfigState.config
            val darkGlassEffectConfig = rememberGlassConfigState(artworkChromeDark = true).config
            // Gate backdrop capture once for devices that cannot render glass.
            val glassAllowed = isGlassAllowed()

            // Share one blurred backdrop across glass surfaces to avoid duplicate effect passes.
            val glassPoolBackdrop = rememberGlassPool()
            val poolSaturation = glassSaturation(glassEffectConfig.vibrancy)
            val poolBlurPx = with(LocalDensity.current) {
                glassEffectConfig.blurRadius.dp.toPx()
            } * GlassPoolScale
            val poolEffects: BackdropEffectScope.() -> Unit =
                remember(poolSaturation, poolBlurPx) {
                    {
                        if (poolSaturation != 1f) colorControls(saturation = poolSaturation)
                        if (poolBlurPx > 0f) blur(poolBlurPx)
                    }
                }
            val pooledGlass = remember(
                glassPoolBackdrop,
                glassEffectConfig.blurRadius,
                glassEffectConfig.vibrancy,
                glassAllowed,
                glassEffectConfig.anyComponentEnabled,
            ) {
                if (USE_SHARED_GLASS_POOL && glassAllowed && glassEffectConfig.anyComponentEnabled) {
                    PooledGlass(
                        backdrop = glassPoolBackdrop,
                        blurRadiusDp = glassEffectConfig.blurRadius,
                        vibrancy = glassEffectConfig.vibrancy,
                    )
                } else {
                    null
                }
            }

            val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
            val defaultOpenTab = remember {
                NavigationTab.HOME
            }
            val tabOpenedFromShortcut = remember {
                when (activity.intent?.action) {
                    MainActivity.ACTION_SEARCH -> NavigationTab.SEARCH
                    MainActivity.ACTION_LIBRARY -> NavigationTab.LIBRARY
                    else -> null
                }
            }

            val openSearchOnLaunch = remember {
                (tabOpenedFromShortcut ?: defaultOpenTab) == NavigationTab.SEARCH
            }
            val initialMainTabRoute = remember {
                when (tabOpenedFromShortcut ?: defaultOpenTab) {
                    NavigationTab.HOME, NavigationTab.SEARCH -> Screens.Songs.route
                    NavigationTab.LIBRARY -> Screens.Library.route
                }
            }

            val topLevelScreens = remember {
                listOf(
                    Screens.Songs.route,
                    Screens.Playlists.route,
                    Screens.Library.route,
                    Screens.Songs.route,
                    "settings",
                )
            }

            val search = rememberAppSearchController(navController, database, localOnlyMode, activity.lifecycleScope)
            val (query, onQueryChange) = search.query
            var searchKeyboardActive by search.keyboardActive

            val currentRoute by remember {
                derivedStateOf { navBackStackEntry?.destination?.route }
            }
            LaunchedEffect(currentRoute) {
                Timber.tag("Navigation").d("route -> $currentRoute")
            }

            // Keep search as a NavHost destination so it uses the normal transition and backdrop handling.
            LaunchedEffect(Unit) {
                if (openSearchOnLaunch) {
                    navController.navigate(Screens.Search.route) { launchSingleTop = true }
                }
            }

            val effectiveRoute = currentRoute

            var downloads by remember { mutableStateOf<Map<String, androidx.media3.exoplayer.offline.Download>>(emptyMap()) }
            LaunchedEffect(currentRoute) {
                if (!AppRoutePolicy.needsDownloadState(currentRoute)) {
                    downloads = emptyMap()
                    return@LaunchedEffect
                }

                withFrameNanos { }
                val util = downloadUtil.get()
                activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    util.downloads.collect { downloads = it }
                }
            }

            val inSearchScreen by remember {
                derivedStateOf {
                    currentRoute?.startsWith("search/") == true ||
                        currentRoute == Screens.Search.route
                }
            }
            val inSearchInputScreen by remember {
                derivedStateOf { currentRoute == Screens.Search.route }
            }
            LaunchedEffect(inSearchScreen) {
                if (!inSearchScreen) searchKeyboardActive = false
            }
            // The floating nav bar keeps Settings as one of its own tabs (see
            // floatingNavigationItems above), so it should stay visible there too —
            // only the classic nav bar treats Settings as a top-bar-only destination.
            val navigationItemRoutes = remember(floatingNavigationItems) {
                floatingNavigationItems.map { it.route }.toSet()
            }
            LaunchedEffect(currentRoute, navigationItems) {
                val route = currentRoute ?: return@LaunchedEffect
                if (route in Screens.MainRoutes && navigationItems.none { it.route == route }) {
                    navController.navigate(navigationItems.first().route) {
                        popUpTo(route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            val isKeyboardOpen = WindowInsets.isImeVisible
            val shouldShowNavigationBar = remember(currentRoute) {
                when {
                    // Settings owns a bottom-docked search field; keep the app tab bar out of its way.
                    currentRoute == Screens.Settings.route -> false
                    // Hide navigation on Settings detail pages as well as the Settings root.
                    currentRoute?.startsWith("settings/") == true -> false
                    !AppRoutePolicy.showsNavigation(currentRoute) -> false
                    else -> true
                }
            }

            val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height

            // The floating bar stays at the bottom in landscape; only classic navigation switches to the rail.
            // The forced layout toggle lets phone users preview the tablet sidebar.

            val (forceTabletLayout) = rememberPreference(ForceTabletLayoutKey, defaultValue = false)
            // A real tablet gets the side bar on its own; the toggle only exists
            // to force it onto a device that wouldn't otherwise qualify.
            val isWideScreen = configuration.containerDpSize.width >= TabletWidthThreshold
            val showRail = forceTabletLayout || isWideScreen
            // Settings owns the whole window on tablets too: no side bar, no mini player.
            val isSettingsRoute = currentRoute == Screens.Settings.route || currentRoute?.startsWith("settings/") == true
            val (sideBarCollapsed, onSideBarCollapsedChange) = rememberPreference(SideBarCollapsedKey, defaultValue = false)
            // The artist page is full-bleed: its navigation moves up into a pill and the page keeps the whole width.
            val topNavigation = showRail && currentRoute?.startsWith("artist/{artistId}?thumb=") == true
            var topNavMenuOpen by remember { mutableStateOf(false) }
            LaunchedEffect(currentRoute) { topNavMenuOpen = false }
            // A spring overshoots past its target; an inset below zero would be negative padding.
            val sideBarContentInsetRaw by animateDpAsState(
                targetValue = when {
                    topNavigation && !topNavMenuOpen -> 0.dp
                    isSettingsRoute -> 0.dp
                    sideBarCollapsed && !topNavigation -> SideBarCollapsedWidth + SideBarMargin * 2
                    else -> SideBarContentInset
                },
                animationSpec = spring(0.9f, 400f),
                label = "sideBarContentInset",
            )
            val sideBarContentInset = sideBarContentInsetRaw.coerceAtLeast(0.dp)

            val navPadding = if (shouldShowNavigationBar && !showRail) {
                if (slimNav) SlimNavBarHeight else NavigationBarHeight
            } else {
                0.dp
            }

            val navigationBarHeight by animateDpAsState(
                targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                animationSpec = NavigationBarAnimationSpec,
                label = "navBarHeight",
            )

            val playerMediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
                ?: remember { mutableStateOf(null) }
            val playerBottomSheetState = rememberBottomSheetState(
                dismissedBound = 0.dp,
                // Keep the sheet's own mini player hidden; the nav surface hosts it.
                collapsedBound = 0.dp,
                expandedBound = maxHeight,
            )

            LaunchedEffect(playerConnection) {
                if (playerMediaMetadata == null) playerConnection?.service?.restoreLuneraQueue(userInitiated = true)
            }
            // Both ends of the mini-to-full morph are recorded into GraphicsLayers
            // owned here, above both the player and the nav bar the pill docks into --
            // remembering them any lower would release them mid-gesture.
            InstallPlayerMorphLayers()

            val playerAboveBars by remember(playerBottomSheetState) {
                derivedStateOf { playerBottomSheetState.progress > 0f }
            }

            // Reserve space only while the floating bar and its player accessory are visible.
            // Search keeps the accessory until the keyboard replaces the bar.

            val hasDockedPlayerAccessory =
                playerMediaMetadata != null && !showRail && shouldShowNavigationBar &&
                    (!inSearchScreen || !searchKeyboardActive)

            // Record the backdrop only when a visible component or expanded player needs it.
            // isCollapsed changes only when opening or closing, unlike per-frame progress.
            val backdropHasConsumer =
                (glassEffectConfig.navBarEnabled && shouldShowNavigationBar && !showRail) ||
                    (glassEffectConfig.sidePanelEnabled && showRail) ||
                    (glassEffectConfig.miniPlayerEnabled && hasDockedPlayerAccessory) ||
                    // The expanded player always samples the shared backdrop.
                    !playerBottomSheetState.isCollapsed

            // Reserve a separate row only when the collapsed mini player sits above the dismissed anchor.
            val reservesStandaloneMini = playerBottomSheetState.collapsedBound > playerBottomSheetState.dismissedBound &&
                !playerBottomSheetState.isDismissed
            val playerAwareWindowInsets = remember(
                bottomInset,
                shouldShowNavigationBar,
                reservesStandaloneMini,
                showRail,
                hasDockedPlayerAccessory,
                sideBarContentInset,
                inSearchInputScreen,
            ) {
                var bottom = bottomInset
                if (shouldShowNavigationBar && !showRail) {
                    bottom += NavigationBarHeight
                }
                if (reservesStandaloneMini && !inSearchInputScreen) bottom += MiniPlayerHeight
                if (hasDockedPlayerAccessory) bottom += DockedAccessoryHeight
                windowsInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    // Space for the floating side bar, carried the same way the
                    // bottom bar's space is: screens spend this as list content
                    // padding, so rows start clear of the panel but the list
                    // still spans the full width and scrolls under its glass.
                    .add(
                        WindowInsets(left = if (showRail) sideBarContentInset else 0.dp)
                    )
            }
            appBarScrollBehavior(
                canScroll = {
                    !inSearchScreen &&
                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                }
            )

            val topAppBarScrollBehavior = appBarScrollBehavior(
                canScroll = {
                    !inSearchScreen &&
                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                },
            )

            // Track pager tab changes through effectiveRoute; they do not update the NavHost entry.
            // Keep the back-stack key too, because a new search query uses the same route template.
            LaunchedEffect(navBackStackEntry, effectiveRoute) {
                // Only the results route (search/{query}) carries a query arg;
                // the search_input landing does not — guard against a null arg
                // so tapping search never NPEs.
                val rawQuery = navBackStackEntry?.arguments?.getString("query")
                if (inSearchScreen && rawQuery != null) {
                    val searchQuery = withContext(Dispatchers.IO) {
                        try {
                            URLDecoder.decode(rawQuery, "UTF-8")
                        } catch (e: IllegalArgumentException) {
                            rawQuery
                        }
                    }
                    onQueryChange(
                        TextFieldValue(
                            searchQuery,
                            TextRange(searchQuery.length)
                        )
                    )
                } else if (!inSearchScreen && navigationItems.fastAny { it.route == effectiveRoute }) {
                    onQueryChange(TextFieldValue())
                }

                topAppBarScrollBehavior.state.resetHeightOffset()

                effectiveRoute?.let {
                    setPreviousTab(it)
                }
            }

            LaunchedEffect(playerConnection) {
                val player = playerConnection?.player ?: return@LaunchedEffect
                if (player.currentMediaItem == null) {
                    if (!playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.dismiss()
                    }
                } else {
                    if (playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }
            }

            DisposableEffect(playerConnection, playerBottomSheetState) {
                val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                            mediaItem != null &&
                            playerBottomSheetState.isDismissed
                        ) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                }
            }

            var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

            // React to pager tab changes and leave Library subpages to draw their own toolbar.
            val (libraryFilter, onLibraryFilterChange) = rememberEnumPreference(
                org.grimmoir.lunera.constants.ChipSortTypeKey,
                org.grimmoir.lunera.constants.LibraryFilter.LIBRARY,
            )
            LaunchedEffect(effectiveRoute, showRail, libraryFilter) {
                shouldShowTopBar = effectiveRoute in topLevelScreens &&
                    effectiveRoute != "settings" &&
                    !(effectiveRoute == Screens.Library.route && libraryFilter != org.grimmoir.lunera.constants.LibraryFilter.LIBRARY)
            }

            val coroutineScope = rememberCoroutineScope()
            var sharedSong: SongItem? by remember {
                mutableStateOf(null)
            }
            val snackbarHostState = remember { SnackbarHostState() }

            // One handler for the three avatar/profile entry points (top bar, side bar, nav graph).
            val onAccountClick: () -> Unit = remember(navController) {
                { navController.navigate(Screens.Settings.route) { launchSingleTop = true } }
            }

            val ringtoneViewModel: org.grimmoir.lunera.ui.screens.settings.RingtoneViewModel = viewModel()
            val ringtoneUiState by ringtoneViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                val queuedIntent = activity.takePendingIntent()
                if (queuedIntent != null) {
                    intentRouter.handleDeepLinkIntent(queuedIntent, navController)
                } else {
                    intentRouter.handleDeepLinkIntent(activity.intent, navController)
                }
            }

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { intent ->
                    intentRouter.handleDeepLinkIntent(intent, navController)
                }

                activity.addOnNewIntentListener(listener)
                onDispose { activity.removeOnNewIntentListener(listener) }
            }

            val currentTitleRes = remember(effectiveRoute) {
                when (effectiveRoute) {
                    Screens.Home.route -> org.grimmoir.lunera.R.string.playlists
                    Screens.Playlists.route -> org.grimmoir.lunera.R.string.playlists
                    Screens.Search.route -> org.grimmoir.lunera.R.string.search
                    Screens.Library.route -> org.grimmoir.lunera.R.string.lunera_folders
                    Screens.Songs.route -> org.grimmoir.lunera.R.string.lunera_all_songs
                    else -> null
                }
            }

            val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
            val (historyButtonEnabled) = rememberPreference(ShowHistoryButtonKey, defaultValue = true)
            val (showStatsButton) = rememberPreference(ShowStatsButtonKey, defaultValue = true)
            var hasHistory by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(pauseListenHistory, historyButtonEnabled) {
                if (!pauseListenHistory || !historyButtonEnabled) {
                    hasHistory = null
                    return@LaunchedEffect
                }

                withFrameNanos { }
                historyDataSource.get().observeEventCount().collect { hasHistory = it > 0 }
            }
            val showHistoryButton =
                historyButtonEnabled && (!pauseListenHistory || hasHistory == true)

            val backdropState = rememberAppBackdropState(pureBlack, currentRoute, floatingNavBarScrollConnection)
            val baseBg = backdropState.baseBg
            val appBackdrop = backdropState.appBackdrop
            val wallpaperBackdrop = rememberLayerBackdrop()
            val backdropFrozenProvider = backdropState.frozenProvider
            val appMotionFrozen = backdropState.motionFrozen
            val backdropFreezeConnection = backdropState.scrollConnection

            // One provider replaces Android's stretch/glow edge effect with the
            // iOS rubber-band for every scroll container in the app.
            val iosOverscroll by rememberPreference(IosOverscrollKey, defaultValue = true)
            val iosOverscrollFactory = rememberIosOverscrollFactory()

            val enterSearch = search.enterSearch
            val navSearchState = search.navSearchState(inSearchScreen, inSearchInputScreen, navController)

            // Counts the sheets currently on screen, so the presentation below holds
            // until the last one closes rather than the first. Every sheet registers
            // itself through the shared presenter -- see PresentingSheetEffect.
            val sheetPresentationState = remember { SheetPresentationState() }

            val fullscreenVideo = remember { org.grimmoir.lunera.ui.player.FullscreenVideoState() }
            CompositionLocalProvider(
                org.grimmoir.lunera.ui.player.LocalFullscreenVideo provides fullscreenVideo,
                LocalOverscrollFactory provides
                    if (iosOverscroll) iosOverscrollFactory else LocalOverscrollFactory.current,
                LocalDatabase provides database,
                LocalLocalOnlyMode provides localOnlyMode,
                LocalNavSearchState provides navSearchState,
                LocalSheetPresentation provides sheetPresentationState,
                // onSurface already carries the accent-contrast treatment (see
                // ColorScheme.accentText), so pure black must not force plain white
                // back over it.
                LocalContentColor provides AppTheme.colorScheme.onSurface,
                LocalPlayerConnection provides playerConnection,
                LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                LocalDownloadUtil provides downloadUtil,
                LocalShimmerTheme provides ShimmerTheme,
                LocalSyncUtils provides syncUtils,
                // Disable Listen Together until ready; current entry points handle a missing manager.
                LocalGlassEffectConfig provides glassEffectConfig,
                org.grimmoir.lunera.ui.theme.LocalDarkGlassEffectConfig provides darkGlassEffectConfig,
                LocalAppleMusicUi provides appleMusicUi,
                LocalTabView provides showRail,
                LocalAppBackdrop provides if (org.grimmoir.lunera.ui.component.hasCustomHomeBackground()) wallpaperBackdrop else appBackdrop,
                LocalIosBackdrop provides appBackdrop,
                LocalAppBackdropFrozen provides backdropFrozenProvider,
                LocalAppMotionFrozen provides appMotionFrozen,
                LocalRingtoneViewModel provides ringtoneViewModel,
                // Read preferences once for all list rows and grid tiles.
                LocalItemPrefs provides rememberItemPrefs(),
                // Share one downloads collector across the app.
                LocalDownloads provides downloads,
            ) {

                // Sheet pages present as cards; popup menus leave the page in place.
                val menuPresentation = rememberSheetPresentation(
                    LocalBottomSheetPageState.current.isVisible,
                )

                val scaffoldPresentation = { menuPresentation }
                val playerLayerPresentation = { menuPresentation }

                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                val onNavItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState, enterSearch, playerConnection) {
                    { screen: Screens, _: Boolean ->
                        if (playerBottomSheetState.isExpanded) playerBottomSheetState.collapseSoft()
                        if (screen == Screens.Home) {
                            if (playerConnection?.player?.mediaItemCount == 0) playerConnection?.service?.restoreLuneraQueue(userInitiated = true)
                            playerBottomSheetState.expandSoft()
                            navController.navigate(Screens.Home.route) { launchSingleTop = true }
                        } else if (screen == Screens.Search) {
                            enterSearch()
                        } else if (navController.currentDestination?.route == screen.route) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                            coroutineScope.launch { topAppBarScrollBehavior.state.resetHeightOffset() }
                        } else {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                }

                val onSearchLongClick: () -> Unit = remember(navController) {
                    {
                        navController.navigate(Screens.Search.route) {
                            launchSingleTop = true
                        }
                    }
                }

                val navBarTotalHeight = bottomInset + NavigationBarHeight

                // Top-level tabs and routes without local chrome share this haze source.

                val topBarHazeState = remember { HazeState() }
                // On API 33+ glass-capable devices, the top bar blurs appBackdrop directly.
                val topBarBlurStyle by rememberEnumPreference(TopBarBlurStyleKey, TopBarBlurStyle.HAZE_PROGRESSIVE)
                val directTopBlur = glassAllowed &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    topBarBlurStyle == TopBarBlurStyle.SINGLE_PASS
                val showFallbackTopHaze = !shouldShowTopBar &&
                    AppRoutePolicy.showsFallbackTopHaze(currentRoute)
                val menuBackdrop = rememberLayerBackdrop()
                val playerBackdropHolder = remember { org.grimmoir.lunera.ui.player.PlayerBackdropHolder() }
                val menuSourceBackdrop = (if (playerBottomSheetState.isExpanded) playerBackdropHolder.backdrop else null) ?: menuBackdrop

                Box(Modifier.fillMaxSize().layerBackdrop(wallpaperBackdrop)) {
                    HomeImageBackground()
                }

                // Capture the page and chrome behind the menu. Keep the menu as a sibling
                // to avoid a RenderNode cycle.
                Box(
                    Modifier.fillMaxSize().then(
                        if (AppRoutePolicy.isWebViewRoute(currentRoute)) Modifier else Modifier.layerBackdrop(menuBackdrop),
                    ),
                ) {

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AppTopBar(
                                visible = shouldShowTopBar || showFallbackTopHaze,
                                chromeVisible = shouldShowTopBar,
                                showRail = showRail,
                                startInset = if (showRail) sideBarContentInset +
                                    (if (sideBarCollapsed) 0.dp else 10.dp) else 0.dp,
                                hazeState = topBarHazeState,
                                backdrop = if (directTopBlur && shouldShowTopBar) appBackdrop else null,
                                cutoutInsets = cutoutInsets,
                                statusBarTop = statusBarTop,
                                effectiveRoute = effectiveRoute,
                                titleRes = currentTitleRes,
                                accountImageUrl = accountImageUrl,
                                accountName = sidebarAccountName,
                                navController = navController,
                                onAccountClick = onAccountClick,
                            )
                        },
                    bottomBar = {
                        // Keep player and navigation outside Scaffold's shrink animation.
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .iosSheetPresentation(scaffoldPresentation, statusBarTop)
                ) {
                    // The top bar overlays content, and screens already apply their own
                    // top inset. Scaffold padding here would add that clearance twice.
                    // Keep the rail's playlist covers and NavHost destinations in one
                    // shared-transition tree so sidebar entry points can morph too.
                    SharedTransitionLayout(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                        org.grimmoir.lunera.ui.utils.LocalNavigationArtwork provides navigationArtwork,
                    ) {
                    Box(Modifier.fillMaxSize()) {
                        val onRailItemClick = onNavItemClick
                        org.grimmoir.lunera.ui.component.AnalysisBanner(
                            Modifier.align(Alignment.TopCenter).zIndex(10f),
                        )

                        val onRailSearchLongClick: () -> Unit = remember(navController) {
                            {
                                navController.navigate(Screens.Search.route) {
                                    launchSingleTop = true
                                }
                            }
                        }

                        Box(Modifier.fillMaxSize()) {
                            // Keep shared backdrop capture and transitions around navigation content.
                            Box(
                                modifier = Modifier
                                    // Keep an opaque background under both screens during predictive-back fades.
                                    .background(if (org.grimmoir.lunera.ui.component.hasCustomHomeBackground()) Color.Transparent else AppTheme.colorScheme.background)
                                    // Settings pages provide their own Haze source. Nested sources are ignored by Haze.
                                    .then(
                                        if ((directTopBlur && shouldShowTopBar) || currentRoute == "settings" || currentRoute?.startsWith("settings/") == true) {
                                            Modifier
                                        } else {
                                            Modifier.hazeSource(topBarHazeState)
                                        },
                                    )
                                    // WebViews render on a separate hardware canvas and flicker when
                                    // recorded into the backdrop layer. Skip recording when WebView
                                    // routes are active or glass is unavailable or unused.
                                    .then(
                                        if (DIAG_DISABLE_BACKDROP ||
                                            AppRoutePolicy.isWebViewRoute(currentRoute) ||
                                            !glassAllowed ||
                                            // The direct top blur samples it too (instead
                                            // of a separate Haze recording of this subtree).
                                            !((glassEffectConfig.anyComponentEnabled && backdropHasConsumer) ||
                                                (directTopBlur && shouldShowTopBar))
                                        ) {
                                            Modifier
                                        } else {
                                            // Keep one backdrop source. Extra full-screen layers force
                                            // Compose to redraw the NavHost during scroll, even when
                                            // backdrop recording is frozen.
                                            Modifier.layerBackdrop(
                                                appBackdrop,
                                                frozen = backdropFrozenProvider,
                                            )
                                        }
                                    )
                                    .nestedScroll(backdropFreezeConnection)
                                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                    .nestedScroll(floatingNavBarScrollConnection)
                            ) {
                                AppNavHost(
                                navController = navController,
                                startDestination = initialMainTabRoute,
                                scrollBehavior = topAppBarScrollBehavior,
                                activity = activity,
                                snackbarHostState = snackbarHostState,
                                accountImageUrl = accountImageUrl,
                                accountName = sidebarAccountName,
                                onAccountClick = onAccountClick,
                                )
                            }
                        }

                        org.grimmoir.lunera.ui.component.RingtoneTrimmerDialog(
                            isVisible = ringtoneUiState.showTrimmer,
                            songId = ringtoneUiState.targetSongId,
                            songTitle = ringtoneUiState.targetSongTitle,
                            duration = ringtoneUiState.targetSongDuration,
                            onDismiss = { ringtoneViewModel.hideTrimmer() },
                            onResolveStreamUrl = { ringtoneViewModel.getStreamUrl(activity, it) },
                            onConfirm = { start, end -> ringtoneViewModel.setAsRingtone(activity, start, end) }
                        )

                        if (ringtoneUiState.showProgress) {
                            org.grimmoir.lunera.ui.component.RingtoneProgressDialog(
                                isVisible = ringtoneUiState.showProgress,
                                progress = ringtoneUiState.progress,
                                statusMessage = ringtoneUiState.statusMessage,
                                isComplete = ringtoneUiState.isComplete,
                                isSuccess = ringtoneUiState.isSuccess,
                                onDismiss = { ringtoneViewModel.dismissProgress() },
                                onOpenSettings = { ringtoneViewModel.openRingtoneSettings(activity) }
                            )
                        }


                        // Keep full-screen tools clear of the rail overlay.

                        if (showRail && !isSettingsRoute && currentRoute != "ambient_mode") {
                            // Each screen owns its back button; a global one would duplicate it.
                            // Sidebar sections include phone top-bar items and playlists with bookmarkedAt set.
                            val sidebarSections = emptyList<org.grimmoir.lunera.ui.component.SideBarSection>()
                            val visibleRailItems = navigationItems
                            val onRailClick: (Screens, Boolean) -> Unit = { screen, selected ->
                                if (screen == Screens.Library) {
                                    onLibraryFilterChange(org.grimmoir.lunera.constants.LibraryFilter.LIBRARY)
                                }
                                onRailItemClick(screen, selected)
                            }

                            // Side panel and top pill are one shared element: tab labels and the glass
                            // container glide between the two layouts instead of cross-fading.
                                AnimatedContent(
                                    targetState = topNavigation && !topNavMenuOpen,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                                    label = "navMorph",
                                ) { asPill ->
                                    CompositionLocalProvider(
                                        org.grimmoir.lunera.ui.utils.LocalNavAnimatedVisibilityScope provides this,
                                    ) {
                                    val morph = NavMorph(this@SharedTransitionLayout, this)
                                    Box(Modifier.fillMaxSize()) {
                                        if (asPill) {
                                            AppTabViewTopPill(
                                                navigationItems = visibleRailItems,
                                                currentRoute = effectiveRoute,
                                                onItemClick = onRailClick,
                                                onOpenSidebar = { topNavMenuOpen = true },
                                                pureBlack = pureBlack,
                                                morph = morph,
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .windowInsetsPadding(windowsInsets.only(WindowInsetsSides.Top))
                                                    .padding(top = 6.dp),
                                            )
                                        } else {
                                            AppFloatingSideBar(
                                                navigationItems = visibleRailItems,
                                                currentRoute = effectiveRoute,
                                                onItemClick = onRailClick,
                                                sections = sidebarSections,
                                                pureBlack = pureBlack,
                                                collapsed = sideBarCollapsed && !topNavigation,
                                                onToggleCollapsed = {
                                                    if (topNavigation) topNavMenuOpen = false
                                                    else onSideBarCollapsedChange(!sideBarCollapsed)
                                                },
                                                onEditClick = { navController.navigate("settings/appearance/theme") },
                                                puckColorIsDefault = glassConfigState.puckColorIsDefault,
                                                footer = { footerCollapsed ->
                                                    SideBarAccountRow(
                                                        accountImageUrl = accountImageUrl,
                                                        accountName = sidebarAccountName,
                                                        collapsed = footerCollapsed,
                                                        onClick = onAccountClick,
                                                    )
                                                },
                                                morph = morph,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .windowInsetsPadding(
                                                        windowsInsets.only(WindowInsetsSides.Vertical)
                                                    )
                                                    .padding(start = SideBarMargin, top = 12.dp, bottom = 12.dp),
                                            )
                                        }
                                    }
                                    }
                                }

                            // In rail mode, dock the mini player at the right edge.
                            // Show the search field there while the user is searching.
                            if (playerMediaMetadata != null || inSearchScreen) {
                                // Center in the remaining content area after the sidebar inset.
                                BoxWithConstraints(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(start = sideBarContentInset),
                                ) {
                                    val pillWidth = maxWidth * FloatingMiniPlayerWidthFraction
                                    AnimatedContent(
                                        targetState = inSearchScreen,
                                        transitionSpec = {
                                            if (targetState) {
                                                (slideInVertically(
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                ) { it / 2 } + fadeIn()) togetherWith
                                                    (slideOutVertically(
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                    ) { it } + fadeOut())
                                            } else {
                                                (slideInVertically(
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                ) { it } + fadeIn()) togetherWith
                                                    (slideOutVertically(
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                    ) { it / 2 } + fadeOut())
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .windowInsetsPadding(
                                                windowsInsets.only(WindowInsetsSides.Bottom)
                                            )
                                            .padding(bottom = 12.dp),
                                        label = "tabViewSearchPill",
                                    ) { searching ->
                                        if (searching) {
                                            NavBarSearchInputBar(
                                                state = navSearchState,
                                                pureBlack = pureBlack,
                                                modifier = Modifier.width(pillWidth),
                                            )
                                        } else if (playerMediaMetadata != null) {
                                            AppFloatingNowPlayingPill(
                                                onClick = { playerBottomSheetState.expandSoft() },
                                                onLyricsClick = {
                                                    playerBottomSheetState.expandSoft()
                                                    playerConnection?.requestShowLyrics?.value = true
                                                },
                                                onQueueClick = {
                                                    playerBottomSheetState.expandSoft()
                                                    playerConnection?.requestShowQueue?.value = true
                                                },
                                                pureBlack = pureBlack,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                    }
                }

                // Overlay the player and navigation above the Scaffold. Only menu sheets
                // shrink this layer; player expansion is handled inside it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .iosSheetPresentation(playerLayerPresentation, statusBarTop)
                ) {
                    if (!showRail && currentRoute?.startsWith("settings/") != true && AppRoutePolicy.showsPlayer(currentRoute)) {
                        Box(Modifier.fillMaxSize()) {
                            // Apple Music-style progressive scrim
                            if (appleMusicUi && isGlassAllowed()) {
                                val chromeBackground = if (LocalAmbientDarkChrome.current.value) {
                                    LocalAmbientBackgroundColor.current.value
                                } else baseBg
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(navBarTotalHeight + 56.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.35f to chromeBackground.copy(alpha = 0.15f),
                                                0.6f to chromeBackground.copy(alpha = 0.4f),
                                                0.85f to chromeBackground.copy(alpha = 0.7f),
                                                1f to chromeBackground,
                                            )
                                        )
                                )
                            }

                            if (pooledGlass != null) {
                                // Draws nothing: it only keeps the shared blur current,
                                // and must run after the content records and before the
                                // chrome samples it.
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .glassPool(
                                            source = appBackdrop,
                                            pooled = glassPoolBackdrop,
                                            effects = poolEffects,
                                            scale = GlassPoolScale,
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier.zIndex(if (playerAboveBars) 1f else 0f),
                            ) {
                                ForcedDarkScope {
                                    CompositionLocalProvider(org.grimmoir.lunera.ui.player.LocalPlayerBackdropHolder provides playerBackdropHolder) {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )
                                    }
                                }
                            }

                            // Album, artist and playlist routes keep the floating nav bar and its mini player in the dark palette.
                val darkRouteActive = org.grimmoir.lunera.ui.theme.isForcedDarkRoute(currentRoute)
                // Keep glass chrome outside its captured content to avoid a recursive RenderNode draw.
                            ForcedDarkScope(enabled = darkRouteActive) {
                            CompositionLocalProvider(LocalPooledGlass provides pooledGlass) {
                                val menuState = LocalMenuState.current
                                val bottomSheetPageState = LocalBottomSheetPageState.current
                                AppFloatingNavBar(
                                    navigationItems = floatingNavigationItems,
                                    currentRoute = effectiveRoute,
                                    onItemClick = onNavItemClick,
                                    scrollConnection = floatingNavBarScrollConnection,
                                    pureBlack = pureBlack,
                                    showPlayerAccessory = hasDockedPlayerAccessory,
                                    onAccessoryClick = { playerBottomSheetState.expandSoft() },
                                    onAccessoryLyricsClick = {
                                        playerBottomSheetState.expandSoft()
                                        playerConnection?.requestShowLyrics?.value = true
                                    },
                                    onAccessoryQueueClick = {
                                        playerBottomSheetState.expandSoft()
                                        playerConnection?.requestShowQueue?.value = true
                                    },
                                    // Long-press the mini bar for the same quick-actions menu a
                                    // long-pressed row opens elsewhere (reference: menu pop up.jpg).
                                    onAccessoryLongClick = {
                                        val metadata = playerConnection?.mediaMetadata?.value
                                        if (metadata != null) {
                                            menuState.show {
                                                PlayerMenu(
                                                    mediaMetadata = metadata,
                                                    navController = navController,
                                                    playerBottomSheetState = playerBottomSheetState,
                                                    onShowDetailsDialog = {
                                                        bottomSheetPageState.showFloating { ShowMediaInfo(metadata.id) }
                                                    },
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    },
                                    tabContextMenu = { screen -> navTabContextMenu(screen, navController) },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .widthIn(max = 500.dp)
                                        .padding(horizontal = AppleTokens.Gutter)
                                        .padding(bottom = bottomInset + 8.dp)
                                        .graphicsLayer {
                                            val hiddenOffset =
                                                size.height + (bottomInset + 8.dp).toPx()
                                            val navBarHeightPx = navigationBarHeight.toPx()
                                            translationY = if (navBarHeightPx == 0f) {
                                                hiddenOffset
                                            } else {
                                                hiddenOffset * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                            }

                                        }
                                )
                            }
                            }
                        }
                    } else {
                        if (AppRoutePolicy.showsBottomSheetPlayer(currentRoute) && !isSettingsRoute) {
                            ForcedDarkScope {
                                CompositionLocalProvider(org.grimmoir.lunera.ui.player.LocalPlayerBackdropHolder provides playerBackdropHolder) {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(bottomInsetDp)
                                .graphicsLayer {
                                    val progress = playerBottomSheetState.progress
                                    alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                }
                                .background(baseBg)
                        )
                    }
                }
            } // Box end

            // menuBackdrop records the entire preceding Box, including the player.
            // Keep the same glass material over the expanded player as over Home.
                // Menus on album, artist, playlist and player surfaces use the dark palette too.
                val menuDark = org.grimmoir.lunera.ui.theme.isForcedDarkRoute(currentRoute) || currentRoute?.startsWith("search") == true ||
                    playerBottomSheetState.progress > 0f
                ForcedDarkScope(enabled = menuDark) {
                OverlayMenu(
                    state = LocalMenuState.current,
                    glass = true,
                    backdrop = menuSourceBackdrop,
                    liquid = true,
                )
                }

                ForcedDarkScope(enabled = menuDark) {
                BottomSheetPage(
                    state = LocalBottomSheetPageState.current,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    liveBackdrop = menuSourceBackdrop,
                    liquidGlassPlayerMode = playerBackgroundStyle == PlayerBackgroundStyle.GLASS,
                )
                }

                // Queue actions share one entry point, so host the conflict prompt here.
                QueueConflictDialog(playerConnection = playerConnection)

                // Player design and Lyrics style open as full-screen page sheets from anywhere.
                org.grimmoir.lunera.ui.screens.settings.SettingsSheetHost(navController)
                QueueReplacedUndoSnackbar(
                    playerConnection = playerConnection,
                    snackbarHostState = snackbarHostState,
                )

                sharedSong?.let { song ->
                    playerConnection?.let {
                        Dialog(
                            onDismissRequest = { sharedSong = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(
                                modifier = Modifier.padding(24.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = AlertDialogDefaults.containerColor,
                                tonalElevation = AlertDialogDefaults.TonalElevation,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    YouTubeSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = { sharedSong = null },
                                    )
                                }
                            }
                        }
                    }
                }

                org.grimmoir.lunera.ui.player.FullscreenVideoOverlay(fullscreenVideo)
            }
        }
    }
}
