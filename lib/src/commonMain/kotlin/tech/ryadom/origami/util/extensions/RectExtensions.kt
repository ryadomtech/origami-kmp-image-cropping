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

package tech.ryadom.origami.util.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import tech.ryadom.origami.util.Edge

internal val Rect.offset get() = topLeft

internal fun Rect.findEdgeContaining(point: Offset, tolerance: Float): Edge? {
    return when {
        isPointInRegion(point, topLeft, tolerance) -> Edge.TopLeft
        isPointInRegion(point, topRight, tolerance) -> Edge.TopRight
        isPointInRegion(point, bottomLeft, tolerance) -> Edge.BottomLeft
        isPointInRegion(point, bottomRight, tolerance) -> Edge.BottomRight
        else -> null
    }
}

private fun isPointInRegion(
    point: Offset,
    region: Offset,
    tolerance: Float = 0f
): Boolean {
    return point.x in (region.x - tolerance)..(region.x + tolerance) &&
            point.y in (region.y - tolerance)..(region.y + tolerance)
}

internal fun Rect.copy(offset: Offset = this.offset, size: Size = this.size): Rect {
    return Rect(offset = offset, size = size)
}