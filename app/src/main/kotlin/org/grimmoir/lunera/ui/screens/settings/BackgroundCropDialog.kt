/** Copyright (C) 2026 Grimmoir Organization. SPDX-License-Identifier: GPL-3.0-only */
package org.grimmoir.lunera.ui.screens.settings

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import org.grimmoir.lunera.ui.ios.compat.*
import java.io.File

/** Native gesture crop editor. The original is kept separately so reframing is lossless. */
@Composable
fun BackgroundCropDialog(path: String, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val colors = AppTheme.colorScheme
    var editorLaunched by rememberSaveable(path) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val outputPath = rememberSaveable(path) { File(context.filesDir, "lunera_background_crop_${System.currentTimeMillis()}.jpg").absolutePath }
    val output = remember(outputPath) { File(outputPath) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when {
            result.resultCode == Activity.RESULT_OK && output.isFile && output.length() > 0 -> onSaved(output.absolutePath)
            result.resultCode == UCrop.RESULT_ERROR -> { output.delete(); error = "This image could not be cropped. Choose another image." }
            else -> { output.delete(); onDismiss() }
        }
    }
    LaunchedEffect(path) {
        if (editorLaunched) return@LaunchedEffect
        editorLaunched = true
        runCatching {
            val authority = "${context.packageName}.FileProvider"
            val source = FileProvider.getUriForFile(context, authority, File(path))
            val dest = FileProvider.getUriForFile(context, authority, output)
            val metrics = context.resources.displayMetrics
            val options = UCrop.Options().apply {
                setToolbarTitle("Crop background")
                setToolbarColor(colors.surface.toArgb())
                setToolbarWidgetColor(colors.onSurface.toArgb())
                setRootViewBackgroundColor(colors.surface.toArgb())
                setActiveControlsWidgetColor(0xFFDC143C.toInt())
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(95)
                setFreeStyleCropEnabled(false)
                setShowCropGrid(true)
                setShowCropFrame(true)
                setAspectRatioOptions(0, com.yalantis.ucrop.model.AspectRatio("Phone", metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat()))
                setAllowedGestures(1, 2, 1)
                setHideBottomControls(false)
            }
            launcher.launch(UCrop.of(source, dest).withAspectRatio(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat()).withMaxResultSize(2400, 4000).withOptions(options).getIntent(context).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        }.onFailure { error = "The image editor could not open this file." }
    }
    error?.let { message -> AlertDialog(onDismissRequest = onDismiss, title = { Text("Background") }, text = { Text(message) }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }) }
}
