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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Corners of the crop area that can be dragged to resize it.
 */
internal enum class Edge {
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft
}

/**
 * Corner of this rect closest to [point], or `null` when every corner is further away than
 * [tolerance].
 *
 * The touch regions of several corners overlap once the crop area gets small, so the nearest
 * one wins.
 */
internal fun Rect.findEdgeContaining(point: Offset, tolerance: Float): Edge? {
    var best: Edge? = null
    var bestDistance = Float.MAX_VALUE

    Edge.entries.forEach { edge ->
        val corner = cornerOf(edge)
        val dx = point.x - corner.x
        val dy = point.y - corner.y

        if (dx <= -tolerance || dx >= tolerance || dy <= -tolerance || dy >= tolerance) {
            return@forEach
        }

        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            best = edge
        }
    }

    return best
}
