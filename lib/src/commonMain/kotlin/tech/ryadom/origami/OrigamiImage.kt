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

package tech.ryadom.origami

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import tech.ryadom.origami.internal.OrigamiOverlayCache
import tech.ryadom.origami.style.OrigamiCropArea

/**
 * Composable for the origami cropping component.
 *
 * @param modifier [Modifier]
 * @param origami [Origami] instance to hold and manage component state. Keep it across
 * recompositions with [rememberOrigami] or `remember`.
 */
@Composable
public fun OrigamiImage(
    modifier: Modifier = Modifier,
    origami: Origami
) {
    val density = LocalDensity.current
    remember(origami, density) { origami.onDensityChanged(density) }

    val overlayCache = remember(origami) {
        OrigamiOverlayCache(origami.cropArea.highlightedShape)
    }

    Box(modifier = modifier) {
        val sourceModifier = Modifier
            .align(Alignment.Center)
            .onGloballyPositioned {
                origami.onGloballyPositioned(
                    topLeft = it.positionInParent(),
                    size = it.size
                )
            }

        // Rotations and flips are baked into a bitmap that has to be drawn here; until then
        // the source renders through its own Content.
        val transformed = origami.transformedBitmap
        if (transformed != null) {
            Image(
                modifier = sourceModifier,
                bitmap = transformed,
                contentDescription = null
            )
        } else {
            origami.source.Content(sourceModifier)
        }

        Spacer(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(origami) {
                    detectDragGestures(
                        onDragStart = origami::onDragStart,
                        onDrag = { pointerInputChange, _ ->
                            pointerInputChange.consume()
                            origami.onDrag(pointerInputChange.position)
                        },
                        onDragEnd = origami::onDragEnd,
                        onDragCancel = origami::onDragEnd
                    )
                }
                // Reading the crop rect here keeps the snapshot observation in the draw phase,
                // so a drag repaints without recomposing.
                .drawBehind {
                    drawOverlay(origami, overlayCache)
                }
        )
    }
}

/**
 * Draws the dimmed background, the crop area and its edges.
 */
private fun DrawScope.drawOverlay(origami: Origami, cache: OrigamiOverlayCache) {
    val cropRect = origami.cropRect
    if (cropRect.width <= 0f || cropRect.height <= 0f) {
        return
    }

    clipPath(
        path = cache.pathFor(cropRect),
        clipOp = ClipOp.Difference
    ) {
        drawRect(color = origami.colors.backgroundColor)
    }

    drawCropArea(
        guidelinesColor = origami.colors.guidelinesColor,
        guidelinesWidthPx = cache.guidelinesWidthPx(origami.cropArea.guidelinesWidth, this),
        guidelinesCount = origami.cropArea.guidelinesCount,
        origamiCropRect = cropRect,
        cache = cache
    )

    origami.cropArea.edges?.onDraw(
        scope = this,
        rect = cropRect,
        colors = origami.colors
    )
}

/**
 * Drawing crop area
 * @param guidelinesColor guidelines color
 * @param guidelinesWidthPx guidelines width, already resolved to pixels
 * @param guidelinesCount guidelines count
 * @param origamiCropRect [Rect]
 *
 * @see [OrigamiCropArea]
 */
private fun DrawScope.drawCropArea(
    guidelinesColor: Color,
    guidelinesWidthPx: Float,
    guidelinesCount: Int,
    origamiCropRect: Rect,
    cache: OrigamiOverlayCache
) {
    // Not drawing an invisible frame or invisible guidelines
    if (guidelinesWidthPx <= 0f || guidelinesColor == Color.Transparent) {
        return
    }

    drawRect(
        color = guidelinesColor,
        topLeft = origamiCropRect.topLeft,
        size = origamiCropRect.size,
        style = cache.strokeOf(guidelinesWidthPx)
    )

    if (guidelinesCount <= 0) {
        return
    }

    drawGuidelines(
        guidelinesCount = guidelinesCount,
        guidelinesColor = guidelinesColor,
        guidelinesWidthPx = guidelinesWidthPx,
        origamiCropRect = origamiCropRect
    )
}

/**
 * Drawing guidelines inside crop area
 * @param guidelinesColor guidelines color
 * @param guidelinesWidthPx guidelines width, already resolved to pixels
 * @param guidelinesCount guidelines count
 * @param origamiCropRect [Rect]
 *
 * @see drawCropArea
 * @see [OrigamiCropArea]
 */
private fun DrawScope.drawGuidelines(
    guidelinesCount: Int,
    guidelinesColor: Color,
    guidelinesWidthPx: Float,
    origamiCropRect: Rect
) = with(origamiCropRect) {
    val verticalStep = size.height / (guidelinesCount + 1)
    val horizontalStep = size.width / (guidelinesCount + 1)

    repeat(guidelinesCount) { index ->
        val lineIndex = index + 1

        val x = topLeft.x + horizontalStep * lineIndex
        drawLine(
            color = guidelinesColor,
            start = Offset(x, topLeft.y),
            end = Offset(x, topLeft.y + size.height),
            strokeWidth = guidelinesWidthPx
        )

        val y = topLeft.y + verticalStep * lineIndex
        drawLine(
            color = guidelinesColor,
            start = Offset(topLeft.x, y),
            end = Offset(topLeft.x + size.width, y),
            strokeWidth = guidelinesWidthPx
        )
    }
}
