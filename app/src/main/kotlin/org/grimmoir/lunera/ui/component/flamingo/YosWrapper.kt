package org.grimmoir.lunera.ui.component.flamingo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@Composable
@NonRestartableComposable
internal fun YosWrapper(content: @Composable () -> Unit) = content()
