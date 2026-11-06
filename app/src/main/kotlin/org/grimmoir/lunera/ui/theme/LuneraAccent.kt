package org.grimmoir.lunera.ui.theme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import org.grimmoir.lunera.constants.*
import org.grimmoir.lunera.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun rememberLuneraAccent(dark: Boolean): Color {
    val context = LocalContext.current
    val (enabled) = rememberPreference(HomeBackgroundEnabledKey, false)
    val (path) = rememberPreference(HomeBackgroundPathKey, "")
    val (dynamic) = rememberPreference(DynamicThemeKey, true)
    val (selected) = rememberPreference(SelectedThemeColorKey, 0)
    val system = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else DefaultThemeColor
    val imageAccent by produceState<Color?>(null, enabled, path) {
        value = null
        if (enabled && path.isNotEmpty()) value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(path, bounds)
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 192).coerceAtLeast(1) }
                android.graphics.BitmapFactory.decodeFile(path, options)?.let { bitmap ->
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    val color = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
                    bitmap.recycle(); color?.let(::Color)
                }
            }.getOrNull()
        }
    }
    return imageAccent ?: if (dynamic || selected == 0) system else Color(selected)
}
