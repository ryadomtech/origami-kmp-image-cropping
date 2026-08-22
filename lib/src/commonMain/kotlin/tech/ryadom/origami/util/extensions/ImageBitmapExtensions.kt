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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/**
 * [Paint] for 1:1 and right-angle blits, where filtering would only soften edges.
 */
private fun exactPaint() = Paint().apply {
    isAntiAlias = false
    filterQuality = FilterQuality.None
}

/**
 * Copies the part of this bitmap covered by [rect] into a new bitmap.
 *
 * [rect] comes from rounded layout coordinates, so it is clamped into the bitmap rather than
 * asserted on.
 *
 * @return a new [ImageBitmap] of the clamped rect size
 */
internal fun ImageBitmap.cropTo(rect: IntRect): ImageBitmap {
    val left = rect.left.coerceIn(0, width)
    val top = rect.top.coerceIn(0, height)
    val right = rect.right.coerceIn(left, width)
    val bottom = rect.bottom.coerceIn(top, height)

    val size = IntSize(
        width = (right - left).coerceAtLeast(1),
        height = (bottom - top).coerceAtLeast(1)
    )

    return ImageBitmap(size.width, size.height).also { target ->
        Canvas(target).drawImageRect(
            image = this,
            srcOffset = IntOffset(left, top),
            srcSize = size,
            dstSize = size,
            paint = exactPaint()
        )
    }
}

/**
 * Rotates this bitmap by [degrees] clockwise.
 *
 * Only right angles are supported; anything else is rounded down to the nearest quarter turn so
 * the result stays a lossless pixel permutation.
 *
 * @return a new rotated [ImageBitmap], or this one when the rotation is a no-op
 */
internal fun ImageBitmap.rotated(degrees: Int): ImageBitmap {
    val quarterTurns = ((degrees / 90) % 4 + 4) % 4
    if (quarterTurns == 0) return this

    val swapsAxes = quarterTurns % 2 == 1
    val targetWidth = if (swapsAxes) height else width
    val targetHeight = if (swapsAxes) width else height

    return ImageBitmap(targetWidth, targetHeight).also { target ->
        val canvas = Canvas(target)
        // Transforms post-concatenate, so these apply bottom up: recenter the source on the
        // origin, spin it, then move it into the middle of the target.
        canvas.translate(targetWidth / 2f, targetHeight / 2f)
        canvas.rotate((quarterTurns * 90).toFloat())
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawImage(this, Offset.Zero, exactPaint())
    }
}

/**
 * Mirrors this bitmap along its vertical axis when [horizontal] is true, along its horizontal
 * axis otherwise.
 *
 * @return a new mirrored [ImageBitmap]
 */
internal fun ImageBitmap.flipped(horizontal: Boolean): ImageBitmap {
    return ImageBitmap(width, height).also { target ->
        val canvas = Canvas(target)

        if (horizontal) {
            canvas.translate(width.toFloat(), 0f)
            canvas.scale(-1f, 1f)
        } else {
            canvas.translate(0f, height.toFloat())
            canvas.scale(1f, -1f)
        }

        canvas.drawImage(this, Offset.Zero, exactPaint())
    }
}
