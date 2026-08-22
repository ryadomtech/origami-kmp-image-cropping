/*
   Copyright 2025 Ryadom Tech

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package tech.ryadom.origami.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import tech.ryadom.origami.style.OrigamiHighlightedShape

/**
 * Holds the overlay values that would otherwise be rebuilt on every frame of a drag.
 *
 * Not thread safe — an instance belongs to a single draw node.
 */
internal class OrigamiOverlayCache(
    private val shape: OrigamiHighlightedShape
) {

    private var pathRect: Rect? = null
    private var path: Path? = null

    private var strokeWidth = Float.NaN
    private var stroke: Stroke? = null

    private var guidelinesDp: Dp? = null
    private var guidelinesDensity = Float.NaN
    private var guidelinesPx = 0f

    /**
     * Highlight path for [rect], rebuilt only when the rect moved.
     */
    fun pathFor(rect: Rect): Path {
        val cached = path
        if (cached != null && pathRect == rect) return cached

        return shape.getPath(rect).also {
            path = it
            pathRect = rect
        }
    }

    /**
     * [Stroke] of [width] pixels, rebuilt only when the width changes.
     */
    fun strokeOf(width: Float): Stroke {
        val cached = stroke
        if (cached != null && strokeWidth == width) return cached

        return Stroke(width = width).also {
            stroke = it
            strokeWidth = width
        }
    }

    /**
     * [width] in pixels, re-resolved only when the value or the [density] changes.
     */
    fun guidelinesWidthPx(width: Dp, density: Density): Float {
        if (guidelinesDp == width && guidelinesDensity == density.density) {
            return guidelinesPx
        }

        guidelinesDp = width
        guidelinesDensity = density.density
        guidelinesPx = with(density) { width.toPx() }

        return guidelinesPx
    }
}
