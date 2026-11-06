/** Adapted from Flamingo by Yos-X, GPL-3.0. See NOTICE. */
package org.grimmoir.lunera.ui.component.flamingo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


interface YosMediaEvent {

    fun onSeek(position: Int)
}
