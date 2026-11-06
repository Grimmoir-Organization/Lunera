/*
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package org.grimmoir.lunera.ui.component.backdrop.backdrops

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import org.grimmoir.lunera.ui.utils.lastNavWasPop
import org.grimmoir.lunera.ui.utils.LocalTabMotion
import org.grimmoir.lunera.ui.utils.LocalPageMotion
import org.grimmoir.lunera.ui.utils.isMorphRoute
import kotlinx.coroutines.flow.collectLatest

// Default retained for artwork and pager motion. Ordinary pages use their shorter clocks.
private const val NavTransitionFreezeWindowNs = 330_000_000L

/** Freezes the app backdrop for a navigation push, which does not trigger [BackdropFreeze]'s scroll-based freeze. */
class NavTransitionFreeze {
    private val startedAtNs = longArrayOf(0L)
    private var windowNs = NavTransitionFreezeWindowNs

    val frozen: () -> Boolean = {
        val started = startedAtNs[0]
        started != 0L && System.nanoTime() - started < windowNs
    }

    fun markTransitionStarted(durationMillis: Int = (NavTransitionFreezeWindowNs / 1_000_000L).toInt()) {
        windowNs = durationMillis.toLong() * 1_000_000L
        startedAtNs[0] = System.nanoTime()
    }
}

/** Freezes route pushes; predictive-back pops are gesture-driven and change route only after commit. */
@Composable
fun rememberNavTransitionFreeze(currentRoute: String?): NavTransitionFreeze {
    val freeze = remember { NavTransitionFreeze() }
    val previousRoute = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(currentRoute) {
        val from = previousRoute[0]
        previousRoute[0] = currentRoute
        if (from != null && from != currentRoute && !lastNavWasPop()) {
            val duration = when {
                LocalTabMotion.isChange(from, currentRoute) -> LocalTabMotion.TotalMillis + 34
                isMorphRoute(from) || isMorphRoute(currentRoute) -> (NavTransitionFreezeWindowNs / 1_000_000L).toInt()
                else -> LocalPageMotion.TotalMillis + 34
            }
            freeze.markTransitionStarted(duration)
        }
    }
    return freeze
}

/**
 * Freezes backdrop recording while a pager changes tabs. Observe scroll state
 * in a coroutine so the draw-phase freeze check does not trigger invalidations.
 */
@Composable
fun rememberPagerTransitionFreeze(pagerState: PagerState): NavTransitionFreeze {
    val freeze = remember { NavTransitionFreeze() }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collectLatest { scrolling ->
                while (scrolling) {
                    freeze.markTransitionStarted()
                    withFrameNanos { }
                }
            }
    }
    return freeze
}
