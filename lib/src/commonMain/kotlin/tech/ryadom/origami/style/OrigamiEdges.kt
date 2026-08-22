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

package tech.ryadom.origami.style

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

/**
 * Origami edges
 */
public fun interface OrigamiEdges {

    /**
     * Callback for drawing
     * @param scope current [DrawScope]
     * @param rect current [Rect] of [OrigamiCropArea]
     * @param colors current [OrigamiColors]
     */
    public fun onDraw(scope: DrawScope, rect: Rect, colors: OrigamiColors)

    /**
     * Circle edges shape
     *
     * @param radius radius of the circle
     */
    public class Circle(
        private val radius: Dp
    ) : OrigamiEdges {
        override fun onDraw(scope: DrawScope, rect: Rect, colors: OrigamiColors) {
            with(scope) {
                val radiusPx = radius.toPx()

                rect.forEachCorner { x, y ->
                    drawCircle(
                        color = colors.edgesColor,
                        center = Offset(x, y),
                        radius = radiusPx
                    )
                }
            }
        }
    }

    /**
     * Rectangle edges shape
     * @param size width and height of rectangle
     * @param cornerRadius rectangle corner's radius
     */
    public class Rectangle(
        private val size: DpSize,
        private val cornerRadius: Dp
    ) : OrigamiEdges {
        override fun onDraw(scope: DrawScope, rect: Rect, colors: OrigamiColors) {
            with(scope) {
                val handleSize = Size(
                    width = this@Rectangle.size.width.toPx(),
                    height = this@Rectangle.size.height.toPx()
                )

                val radiusPx = cornerRadius.toPx()
                val radius = CornerRadius(x = radiusPx, y = radiusPx)

                // Each handle is centered on its corner
                val halfWidth = handleSize.width / 2f
                val halfHeight = handleSize.height / 2f

                rect.forEachCorner { x, y ->
                    drawRoundRect(
                        color = colors.edgesColor,
                        topLeft = Offset(x - halfWidth, y - halfHeight),
                        size = handleSize,
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

/**
 * Visits the four corners of this rect without allocating.
 */
private inline fun Rect.forEachCorner(action: (x: Float, y: Float) -> Unit) {
    action(left, top)
    action(right, top)
    action(left, bottom)
    action(right, bottom)
}
