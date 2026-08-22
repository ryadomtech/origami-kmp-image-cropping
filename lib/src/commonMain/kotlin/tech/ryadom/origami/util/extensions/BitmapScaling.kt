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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * No dimension limit.
 */
internal const val NO_DIMENSION_LIMIT: Int = Int.MAX_VALUE

/**
 * Scales this bitmap down so that neither side exceeds [maxDimension], preserving the aspect
 * ratio.
 *
 * @return a new, smaller [ImageBitmap], or this one when it already fits
 */
internal fun ImageBitmap.scaledToFit(maxDimension: Int): ImageBitmap {
    if (maxDimension <= 0) return this
    if (width <= maxDimension && height <= maxDimension) return this

    val scale = min(
        maxDimension.toFloat() / width,
        maxDimension.toFloat() / height
    )

    val target = IntSize(
        width = (width * scale).toInt().coerceAtLeast(1),
        height = (height * scale).toInt().coerceAtLeast(1)
    )

    return ImageBitmap(target.width, target.height).also { scaled ->
        Canvas(scaled).drawImageRect(
            image = this,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = target,
            // Downscaling, so filtering keeps the result from aliasing
            paint = Paint().apply {
                isAntiAlias = true
                filterQuality = FilterQuality.Medium
            }
        )
    }
}
