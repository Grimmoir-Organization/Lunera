/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package org.grimmoir.lunera.ui.component.backdrop.backdrops
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import org.grimmoir.lunera.ui.component.backdrop.internal.recordLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @param frozen briefly holds the glass capture while foreground content continues drawing.
 *   A first draw or a size change always captures; thaw schedules a fresh capture.
 */
fun Modifier.layerBackdrop(
    backdrop: LayerBackdrop,
    frozen: () -> Boolean = { false }
): Modifier = this then LayerBackdropElement(backdrop, frozen)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
    val frozen: () -> Boolean
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop, frozen)
    }

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.frozen = frozen
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (frozen != other.frozen) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * backdrop.hashCode() + frozen.hashCode()
    }
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop,
    var frozen: () -> Boolean
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    private var recordedSize = IntSize.Zero
    private var thawJob: Job? = null
    private val capturedStaticRegions = ArrayList<Rect>()

    private fun scheduleThawRedraw() {
        if (thawJob?.isActive == true) return
        thawJob = coroutineScope.launch {
            while (frozen()) delay(32L)
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        if (frozen() && backdrop.hasRecording && recordedSize == size.toIntSize()) {
            // Replay the capture only in glass samplers. Replaying it here would
            // freeze the actual page animation and scrolling as well.
            // Static declarations must continue describing the held capture,
            // rather than accumulating declarations from the live foreground.
            capturedStaticRegions.clear()
            capturedStaticRegions.addAll(backdrop.staticRegions)
            backdrop.staticRegions.clear()
            try {
                backdrop.onDraw(this)
            } finally {
                backdrop.staticRegions.clear()
                backdrop.staticRegions.addAll(capturedStaticRegions)
            }
            scheduleThawRedraw()
            return
        }
        thawJob?.cancel()
        thawJob = null
        // Cleared every generation, before children (backdropStaticRegion among
        // them) redraw and repopulate it below — see LayerBackdrop.staticRegions.
        backdrop.staticRegions.clear()

        // Record once and composite to the screen; glass samplers reuse this layer.
        recordLayer(this@LayerBackdropNode, backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
        recordedSize = size.toIntSize()
        drawLayer(backdrop.graphicsLayer)
        // Notify sampling glass surfaces that the source pixels changed, so they
        // re-record their own layers. When the source is static they skip it.
        backdrop.contentRecorded()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (backdrop.layerCoordinates !== coordinates) backdrop.geometryChanged()
            backdrop.layerCoordinates = coordinates
        }
    }

    override fun onDetach() {
        thawJob?.cancel()
        thawJob = null
        recordedSize = IntSize.Zero
        capturedStaticRegions.clear()
        backdrop.layerCoordinates = null
    }
}
