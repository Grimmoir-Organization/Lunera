/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Lunera switch presentation revised in 2026.
 */
package org.grimmoir.lunera.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.grimmoir.lunera.ui.ios.foundation.IosTheme

/** Shared compact Lunera switch, with a full-size touch target and accessible state. */
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(160), label = "luneraSwitch")
    val crimson = Color(0xFFC41E3A)
    val track = if (checked) crimson else IosTheme.colors.systemFill
    val border = if (checked) crimson else IosTheme.colors.secondaryLabel
    Box(
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(if (onCheckedChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            else Modifier.semantics { role = Role.Switch; toggleableState = ToggleableState(checked); if (!enabled) disabled() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(44.dp, 26.dp).clip(RoundedCornerShape(13.dp))
            .background(track.copy(alpha = if (enabled) 1f else .4f))
            .border(1.dp, border.copy(alpha = if (enabled) .8f else .3f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.CenterStart) {
            Box(Modifier.offset { IntOffset((3.dp + 18.dp * progress).roundToPx(), 0) }
                .size(20.dp).clip(CircleShape).background(if (enabled) Color.White else Color(0xFFCCCCCC)))
        }
    }
}
@Composable
fun GlassSwitchCompat(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, thumbContent: (@Composable () -> Unit)? = null, enabled: Boolean = true, colors: SwitchColors? = null) =
    GlassSwitch(checked, onCheckedChange, enabled, modifier)
