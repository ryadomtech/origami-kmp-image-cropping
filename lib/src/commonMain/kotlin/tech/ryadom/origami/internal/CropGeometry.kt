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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Geometry of the crop area.
 *
 * Two coordinate spaces are used:
 * - *container space* — pixels, relative to the parent of the source image. Crop rects live here.
 * - *source space* — pixels of the backing [androidx.compose.ui.graphics.ImageBitmap].
 */
internal object CropGeometry {

    /**
     * Rect actually covered by the image once it is fitted into [container] with
     * `ContentScale.Fit` semantics (uniform scale, centered, no cropping).
     *
     * @return the drawn image rect, or [Rect.Zero] when nothing can be drawn
     */
    fun imageBoundsIn(container: Rect, sourceWidth: Int, sourceHeight: Int): Rect {
        if (sourceWidth <= 0 || sourceHeight <= 0) return Rect.Zero
        if (container.width <= 0f || container.height <= 0f) return Rect.Zero

        val scale = min(
            container.width / sourceWidth,
            container.height / sourceHeight
        )

        val width = sourceWidth * scale
        val height = sourceHeight * scale

        return Rect(
            offset = Offset(
                x = container.left + (container.width - width) / 2f,
                y = container.top + (container.height - height) / 2f
            ),
            size = Size(width, height)
        )
    }

    /**
     * Largest crop rect of [aspectRatio] that fits into [bounds], centered inside it.
     *
     * @param resolveWidth receives the widest width of [aspectRatio] that fits into [bounds] and
     * returns the width to actually use, letting the caller apply its padding policy
     * @param minSize lower bound for both sides, itself capped by [bounds]
     */
    fun initialCropRect(
        bounds: Rect,
        aspectRatio: Float,
        minSize: Float,
        resolveWidth: (maxWidth: Float) -> Float
    ): Rect {
        if (bounds.width <= 0f || bounds.height <= 0f) return Rect.Zero

        val ratio = sanitizeAspectRatio(aspectRatio)

        // Widest rect of `ratio` that still fits into bounds.
        val maxWidth = min(bounds.width, bounds.height * ratio)

        // Both sides have to clear minSize.
        val minWidth = min(max(minSize, minSize * ratio), maxWidth)

        val requested = resolveWidth(maxWidth)
        val width = if (requested.isFinite()) {
            requested.fastCoerceIn(minWidth, maxWidth)
        } else {
            maxWidth
        }

        val size = Size(width, width / ratio)

        return Rect(
            offset = Offset(
                x = bounds.left + (bounds.width - size.width) / 2f,
                y = bounds.top + (bounds.height - size.height) / 2f
            ),
            size = size
        )
    }

    /**
     * Translates [rect] by [delta], keeping it fully inside [bounds].
     *
     * A rect larger than [bounds] is pinned to the top left corner rather than allowed to drift.
     */
    fun move(rect: Rect, delta: Offset, bounds: Rect): Rect {
        val maxLeft = (bounds.right - rect.width).fastCoerceAtLeast(bounds.left)
        val maxTop = (bounds.bottom - rect.height).fastCoerceAtLeast(bounds.top)

        return Rect(
            offset = Offset(
                x = (rect.left + delta.x).fastCoerceIn(bounds.left, maxLeft),
                y = (rect.top + delta.y).fastCoerceIn(bounds.top, maxTop)
            ),
            size = rect.size
        )
    }

    /**
     * Drags the [edge] corner of [rect] by [delta] with a free aspect ratio.
     *
     * The opposite corner stays anchored and the dragged corner keeps its orientation relative
     * to it, so dragging a handle past the anchor stops at [minSize].
     */
    fun resizeFree(
        rect: Rect,
        edge: Edge,
        delta: Offset,
        bounds: Rect,
        minSize: Float
    ): Rect {
        val anchor = rect.oppositeCornerOf(edge)
        val dragged = rect.cornerOf(edge) + delta
        val limit = minSize.fastCoerceIn(0f, min(bounds.width, bounds.height).fastCoerceAtLeast(0f))

        val movesLeftSide = edge == Edge.TopLeft || edge == Edge.BottomLeft
        val movesTopSide = edge == Edge.TopLeft || edge == Edge.TopRight

        val x = clampMovingSide(dragged.x, anchor.x, limit, bounds.left, bounds.right, movesLeftSide)
        val y = clampMovingSide(dragged.y, anchor.y, limit, bounds.top, bounds.bottom, movesTopSide)

        return Rect(
            left = min(anchor.x, x),
            top = min(anchor.y, y),
            right = max(anchor.x, x),
            bottom = max(anchor.y, y)
        )
    }

    /**
     * Drags the [edge] corner of [rect] by [delta] while preserving [aspectRatio].
     *
     * The dragged corner is projected onto the diagonal that starts at the anchored corner and
     * has the requested ratio. Clamping the projection length, rather than the resulting rect,
     * keeps the anchor stable while the drag runs into an edge.
     */
    fun resizeFixedRatio(
        rect: Rect,
        edge: Edge,
        delta: Offset,
        bounds: Rect,
        aspectRatio: Float,
        minSize: Float
    ): Rect {
        val ratio = sanitizeAspectRatio(aspectRatio)
        val anchor = rect.oppositeCornerOf(edge)
        val dragged = rect.cornerOf(edge) + delta

        // Direction from the anchor towards the dragged corner. The projection below is the
        // height of the resulting rect, so its width is always `ratio` times that.
        val direction = Offset(
            x = if (edge == Edge.TopRight || edge == Edge.BottomRight) ratio else -ratio,
            y = if (edge == Edge.BottomLeft || edge == Edge.BottomRight) 1f else -1f
        )

        val vector = dragged - anchor
        val projection = (vector.x * direction.x + vector.y * direction.y) /
                (direction.x * direction.x + direction.y * direction.y)

        val availableX = if (direction.x > 0f) bounds.right - anchor.x else anchor.x - bounds.left
        val availableY = if (direction.y > 0f) bounds.bottom - anchor.y else anchor.y - bounds.top

        val maxHeight = min(availableX / ratio, availableY).fastCoerceAtLeast(0f)
        val minHeight = min(max(minSize, minSize / ratio), maxHeight)

        val height = projection.fastCoerceIn(minHeight, maxHeight)
        val corner = anchor + Offset(direction.x * height, direction.y * height)

        return Rect(
            left = min(anchor.x, corner.x),
            top = min(anchor.y, corner.y),
            right = max(anchor.x, corner.x),
            bottom = max(anchor.y, corner.y)
        )
    }

    /**
     * Re-projects [rect] from [from] onto [to], preserving its relative position and size.
     */
    fun remap(rect: Rect, from: Rect, to: Rect): Rect {
        if (from.width <= 0f || from.height <= 0f) return rect
        if (to.width <= 0f || to.height <= 0f) return Rect.Zero

        val scaleX = to.width / from.width
        val scaleY = to.height / from.height

        return Rect(
            left = to.left + (rect.left - from.left) * scaleX,
            top = to.top + (rect.top - from.top) * scaleY,
            right = to.left + (rect.right - from.left) * scaleX,
            bottom = to.top + (rect.bottom - from.top) * scaleY
        )
    }

    /**
     * Maps [cropRect] from container space into source pixels.
     *
     * The result is always a valid, non-empty sub-rect of `0..sourceWidth x 0..sourceHeight`,
     * so it can be handed to a bitmap crop without a bounds check. Returns `null` when
     * [cropRect] and [imageBounds] do not overlap at all.
     */
    fun toSourceRect(
        cropRect: Rect,
        imageBounds: Rect,
        sourceWidth: Int,
        sourceHeight: Int
    ): IntRect? {
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        if (imageBounds.width <= 0f || imageBounds.height <= 0f) return null

        val overlap = cropRect.intersect(imageBounds)
        if (overlap.width <= 0f || overlap.height <= 0f) return null

        val scaleX = sourceWidth / imageBounds.width
        val scaleY = sourceHeight / imageBounds.height

        val left = ((overlap.left - imageBounds.left) * scaleX).roundToInt()
        val top = ((overlap.top - imageBounds.top) * scaleY).roundToInt()
        val right = ((overlap.right - imageBounds.left) * scaleX).roundToInt()
        val bottom = ((overlap.bottom - imageBounds.top) * scaleY).roundToInt()

        val (safeLeft, safeRight) = clampSpan(left, right, sourceWidth)
        val (safeTop, safeBottom) = clampSpan(top, bottom, sourceHeight)

        return IntRect(left = safeLeft, top = safeTop, right = safeRight, bottom = safeBottom)
    }

    /**
     * Forces a span into `0..limit`, keeping it at least one pixel wide.
     */
    private fun clampSpan(start: Int, end: Int, limit: Int): Pair<Int, Int> {
        var low = start.coerceIn(0, limit)
        var high = end.coerceIn(0, limit)

        if (high <= low) {
            if (low >= limit) low = limit - 1
            high = low + 1
        }

        return low to high
    }

    /**
     * Clamps one moving side of a free resize into `lo..hi`, keeping it at least [minExtent]
     * away from [anchor] on the side [isBefore] describes.
     *
     * A side that cannot honour [minExtent] is pinned to the edge of [lo]`..`[hi].
     */
    private fun clampMovingSide(
        value: Float,
        anchor: Float,
        minExtent: Float,
        lo: Float,
        hi: Float,
        isBefore: Boolean
    ): Float {
        return if (isBefore) {
            value.fastCoerceIn(lo, (anchor - minExtent).fastCoerceAtLeast(lo))
        } else {
            value.fastCoerceIn((anchor + minExtent).fastCoerceAtMost(hi), hi)
        }
    }

}

/**
 * Replaces a zero, negative or non-finite ratio with 1, keeping NaN out of the math.
 */
internal fun sanitizeAspectRatio(aspectRatio: Float): Float {
    return if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f
}

internal fun Rect.cornerOf(edge: Edge): Offset = when (edge) {
    Edge.TopLeft -> topLeft
    Edge.TopRight -> topRight
    Edge.BottomRight -> bottomRight
    Edge.BottomLeft -> bottomLeft
}

internal fun Rect.oppositeCornerOf(edge: Edge): Offset = when (edge) {
    Edge.TopLeft -> bottomRight
    Edge.TopRight -> bottomLeft
    Edge.BottomRight -> topLeft
    Edge.BottomLeft -> topRight
}
