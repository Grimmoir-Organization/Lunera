package org.grimmoir.lunera.ui.component
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.grimmoir.lunera.R
import org.grimmoir.lunera.ui.ios.compat.Icon
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.ui.screens.library.apple.SfStyle
import java.util.Locale

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared reference avatar for tab headers and the iPad account footer. */
@Composable
fun AccountAvatar(
    imageUrl: String?,
    name: String = "",
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Box(modifier.size(size).semantics { contentDescription = "Settings" }, contentAlignment = Alignment.Center) {
        SymbolIcon(symbol = sf("gearshape"), contentDescription = "Settings", modifier = Modifier.size(if (size <= 32.dp) 20.dp else 26.dp))
    }
}
