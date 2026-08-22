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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.ryadom.origami.internal.CropGeometry
import tech.ryadom.origami.internal.Edge
import tech.ryadom.origami.internal.cornerOf
import tech.ryadom.origami.internal.findEdgeContaining
import tech.ryadom.origami.internal.sanitizeAspectRatio
import tech.ryadom.origami.shared.ImageCompressor
import tech.ryadom.origami.shared.createImageCompressor
import tech.ryadom.origami.style.OrigamiAspectRatio
import tech.ryadom.origami.style.OrigamiColors
import tech.ryadom.origami.style.OrigamiCompression
import tech.ryadom.origami.style.OrigamiCropArea
import tech.ryadom.origami.util.BitmapSource
import tech.ryadom.origami.util.OrigamiSource
import tech.ryadom.origami.util.extensions.cropTo
import tech.ryadom.origami.util.extensions.flipped
import tech.ryadom.origami.util.extensions.rotated

/**
 * State holder for an [OrigamiImage].
 *
 * Hold one instance per image and keep it across recompositions, either with [rememberOrigami]
 * or with a plain `remember`. A new instance on every recomposition loses the current selection
 * and re-runs the source scaling.
 *
 * @param source image [OrigamiSource]
 * @param colors [OrigamiColors]
 * @param cropArea [OrigamiCropArea]
 * @param origamiAspectRatio [OrigamiAspectRatio]
 * @param compressor [ImageCompressor] used by [crop] when compression options are supplied
 */
@Stable
public class Origami(
    internal val source: OrigamiSource,
    internal val colors: OrigamiColors,
    internal val cropArea: OrigamiCropArea,
    internal val origamiAspectRatio: OrigamiAspectRatio,
    private val compressor: ImageCompressor = createImageCompressor()
) {

    /**
     * Current crop area, in the coordinate space of the [OrigamiImage] box.
     *
     * Backed by snapshot state, so reading it from a composable follows the user's dragging —
     * at the cost of recomposing on every frame of that drag.
     */
    public val cropRect: Rect
        get() = cropRectState

    /**
     * Total rotation applied through [rotateClockwise] / [rotateCounterClockwise],
     * normalized to `0`, `90`, `180` or `270`.
     */
    public var rotationDegrees: Int by mutableStateOf(0)
        private set

    /**
     * Aspect ratio (width / height) the crop area is currently locked to, or `null` when the
     * user is free to reshape it.
     *
     * Assigning re-fits the crop area. Values that are not finite and positive are ignored.
     *
     * @see setAspectRatio for assigning an [OrigamiAspectRatio] preset
     */
    public var aspectRatio: Float?
        get() = aspectRatioState
        set(value) {
            if (value != null && (!value.isFinite() || value <= 0f)) return
            if (value == aspectRatioState) return

            aspectRatioState = value
            invalidateSelection()
        }

    private var aspectRatioState: Float? by mutableStateOf(
        if (origamiAspectRatio.isVariable) null
        else sanitizeAspectRatio(origamiAspectRatio.aspectRatio)
    )

    private var cropRectState by mutableStateOf(Rect.Zero)

    /**
     * Bitmap produced by [rotateClockwise] and friends, or `null` while no transform is active.
     *
     * While it is `null` the source is drawn through [OrigamiSource.Content].
     */
    internal var transformedBitmap: ImageBitmap? by mutableStateOf(null)
        private set

    private val baseBitmap: ImageBitmap by lazy(LazyThreadSafetyMode.NONE) {
        source.getImageBitmap()
    }

    private var containerRect = Rect.Zero
    private var imageBounds = Rect.Zero

    private var minCropSizePx = 0f
    private var handleTouchTargetPx = 0f

    // Drags are absolute: the rect captured at drag start plus the total pointer travel.
    private var dragStartRect = Rect.Zero
    private var draggedEdge: Edge? = null
    private var isMovingArea = false
    private var grabOffset = Offset.Zero

    // Set when a transform invalidates the current selection, consumed by the next layout pass.
    private var needsRefit = true

    // Restored selection in 0..1 image coordinates, held until a layout pass can place it.
    private var pendingSelection: Rect? = null

    public constructor(
        imageBitmap: ImageBitmap,
        colors: OrigamiColors = OrigamiColors.createDefault(),
        cropArea: OrigamiCropArea = OrigamiCropArea(),
        aspectRatio: OrigamiAspectRatio = OrigamiAspectRatio(),
        compressor: ImageCompressor = createImageCompressor()
    ) : this(
        source = BitmapSource(
            imageBitmap = compressor.scaleToPlatformLimits(imageBitmap)
        ),
        colors = colors,
        cropArea = cropArea,
        origamiAspectRatio = aspectRatio,
        compressor = compressor
    )

    /**
     * Crops the source down to the current crop area.
     *
     * The returned bitmap is a copy; the source is left untouched. When the component has not
     * been laid out yet, or the crop area does not overlap the image, the source is returned
     * unchanged.
     *
     * Allocates and blits a bitmap on the calling thread. For large images prefer the [crop]
     * overload taking [OrigamiCompression], or wrap this call in your own dispatcher.
     */
    public fun crop(): ImageBitmap {
        val bitmap = currentBitmap()
        val sourceRect = CropGeometry.toSourceRect(
            cropRect = cropRectState,
            imageBounds = imageBounds,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        ) ?: return bitmap

        return bitmap.cropTo(sourceRect)
    }

    /**
     * Crops the source and then compresses the result to fit [compression].
     *
     * Runs off the main thread and honours cancellation.
     *
     * Compression is a lossy JPEG round trip and is a no-op on JS and Wasm, where no encoder is
     * bundled.
     */
    public suspend fun crop(compression: OrigamiCompression): ImageBitmap {
        val cropped = withContext(Dispatchers.Default) { crop() }
        return compressor.compress(cropped, compression)
    }

    /**
     * Restores the initial crop area and drops every rotation and flip.
     */
    public fun reset() {
        rotationDegrees = 0
        transformedBitmap = null
        pendingSelection = null
        aspectRatioState = if (origamiAspectRatio.isVariable) {
            null
        } else {
            sanitizeAspectRatio(origamiAspectRatio.aspectRatio)
        }

        invalidateSelection()
    }

    /**
     * Rotates the image a quarter turn clockwise and re-fits the crop area.
     */
    public fun rotateClockwise(): Unit = rotate(quarterTurns = 1)

    /**
     * Rotates the image a quarter turn counter-clockwise and re-fits the crop area.
     */
    public fun rotateCounterClockwise(): Unit = rotate(quarterTurns = -1)

    /**
     * Mirrors the image along its vertical axis and re-fits the crop area.
     */
    public fun flipHorizontally(): Unit = flip(horizontal = true)

    /**
     * Mirrors the image along its horizontal axis and re-fits the crop area.
     */
    public fun flipVertically(): Unit = flip(horizontal = false)

    /**
     * Locks the crop area to [preset] and re-fits it.
     *
     * `OrigamiAspectRatio.Free` unlocks the ratio.
     */
    public fun setAspectRatio(preset: OrigamiAspectRatio) {
        aspectRatio = if (preset.isVariable) null else preset.aspectRatio
    }

    /**
     * Resolves [OrigamiCropArea.minSize] and [OrigamiCropArea.handleTouchTarget] to pixels.
     *
     * Does not touch the crop area — a density change brings a layout pass that re-fits it.
     */
    internal fun onDensityChanged(density: Density) {
        with(density) {
            minCropSizePx = cropArea.minSize.toPx()
            handleTouchTargetPx = cropArea.handleTouchTarget.toPx()
        }
    }

    /**
     * Image globally positioned callback.
     *
     * @param topLeft position of the source content inside the [OrigamiImage] box
     * @param size measured size of the source content
     */
    internal fun onGloballyPositioned(topLeft: Offset, size: IntSize) {
        val container = Rect(offset = topLeft, size = size.toSize())
        if (container == containerRect && !needsRefit) return

        containerRect = container

        val bitmap = currentBitmap()
        val previousBounds = imageBounds
        imageBounds = CropGeometry.imageBoundsIn(
            container = container,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )

        val shouldRefit = needsRefit ||
                previousBounds.width <= 0f ||
                previousBounds.height <= 0f ||
                cropRectState.width <= 0f

        needsRefit = false

        cropRectState = if (shouldRefit) {
            pendingSelection
                ?.let { denormalize(it) }
                ?.also { pendingSelection = null }
                ?: initialCropRect()
        } else {
            // The container moved or was resized, so carry the selection over.
            CropGeometry.remap(cropRectState, from = previousBounds, to = imageBounds)
        }
    }

    /**
     * Dragging start callback.
     * @param touchPoint initial point
     */
    internal fun onDragStart(touchPoint: Offset) {
        val rect = cropRectState
        dragStartRect = rect

        // Corners win over the interior, so a small crop area keeps reachable handles.
        val edge = rect.findEdgeContaining(touchPoint, handleTouchTargetPx)
        draggedEdge = edge
        isMovingArea = edge == null && rect.contains(touchPoint)

        grabOffset = when {
            edge != null -> rect.cornerOf(edge) - touchPoint
            isMovingArea -> rect.topLeft - touchPoint
            else -> Offset.Zero
        }
    }

    /**
     * Dragging callback.
     * @param dragPoint drag point
     */
    internal fun onDrag(dragPoint: Offset) {
        if (imageBounds.width <= 0f || imageBounds.height <= 0f) return

        val target = dragPoint + grabOffset
        val edge = draggedEdge

        cropRectState = when {
            edge != null -> resizeFrom(dragStartRect, edge, target)
            isMovingArea -> CropGeometry.move(
                rect = dragStartRect,
                delta = target - dragStartRect.topLeft,
                bounds = imageBounds
            )

            else -> return
        }
    }

    /**
     * Dragging end callback.
     */
    internal fun onDragEnd() {
        draggedEdge = null
        isMovingArea = false
        grabOffset = Offset.Zero
    }

    /**
     * Bitmap currently on screen: the transformed one when a rotation or flip is active,
     * otherwise whatever the source provides.
     */
    private fun currentBitmap(): ImageBitmap = transformedBitmap ?: baseBitmap

    /**
     * Current selection expressed in `0..1` image coordinates, or the pending one when the
     * component has not been laid out yet.
     *
     * Resolution independent, so it can be stashed in saved instance state and restored into a
     * differently sized layout.
     */
    internal fun normalizedSelection(): Rect? {
        val bounds = imageBounds
        val rect = cropRectState

        if (bounds.width <= 0f || bounds.height <= 0f || rect.width <= 0f) {
            return pendingSelection
        }

        return Rect(
            left = (rect.left - bounds.left) / bounds.width,
            top = (rect.top - bounds.top) / bounds.height,
            right = (rect.right - bounds.left) / bounds.width,
            bottom = (rect.bottom - bounds.top) / bounds.height
        )
    }

    /**
     * Restores state produced by [normalizedSelection].
     *
     * Flips are not restored — unlike [rotationDegrees] they are not tracked well enough to be
     * replayed out of order.
     */
    internal fun restoreState(selection: Rect?, rotation: Int, ratio: Float?) {
        val normalizedRotation = ((rotation % 360) + 360) % 360
        if (normalizedRotation != 0) {
            rotationDegrees = normalizedRotation
            transformedBitmap = baseBitmap.rotated(normalizedRotation)
        }

        aspectRatioState = ratio
        pendingSelection = selection
        needsRefit = true
    }

    private fun denormalize(selection: Rect): Rect {
        val bounds = imageBounds

        return Rect(
            left = bounds.left + selection.left * bounds.width,
            top = bounds.top + selection.top * bounds.height,
            right = bounds.left + selection.right * bounds.width,
            bottom = bounds.top + selection.bottom * bounds.height
        )
    }

    private fun resizeFrom(rect: Rect, edge: Edge, targetCorner: Offset): Rect {
        val delta = targetCorner - rect.cornerOf(edge)
        val ratio = aspectRatio

        return if (ratio == null) {
            CropGeometry.resizeFree(
                rect = rect,
                edge = edge,
                delta = delta,
                bounds = imageBounds,
                minSize = minCropSizePx
            )
        } else {
            CropGeometry.resizeFixedRatio(
                rect = rect,
                edge = edge,
                delta = delta,
                bounds = imageBounds,
                aspectRatio = ratio,
                minSize = minCropSizePx
            )
        }
    }

    private fun initialCropRect(): Rect {
        val bounds = imageBounds
        if (bounds.width <= 0f || bounds.height <= 0f) return Rect.Zero

        // A free ratio starts out matching the image so nothing is cropped away by default.
        val ratio = aspectRatio ?: (bounds.width / bounds.height)

        return CropGeometry.initialCropRect(
            bounds = bounds,
            aspectRatio = ratio,
            minSize = minCropSizePx
        ) { maxWidth ->
            cropArea.initialPaddings.getFactor(maxWidth)
        }
    }

    private fun rotate(quarterTurns: Int) {
        val degrees = if (quarterTurns > 0) 90 else 270
        rotationDegrees = (rotationDegrees + degrees) % 360
        transformedBitmap = currentBitmap().rotated(degrees)
        invalidateSelection()
    }

    private fun flip(horizontal: Boolean) {
        transformedBitmap = currentBitmap().flipped(horizontal)
        invalidateSelection()
    }

    /**
     * Marks the current selection as stale.
     *
     * A transform changes the image dimensions, so the crop area is re-fitted rather than
     * stretched over a differently shaped image. The flag stays set for the layout pass that
     * follows, which re-fits again against the final bounds.
     */
    private fun invalidateSelection() {
        needsRefit = true
        if (containerRect.width <= 0f || containerRect.height <= 0f) return

        val bitmap = currentBitmap()
        imageBounds = CropGeometry.imageBoundsIn(
            container = containerRect,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )

        cropRectState = initialCropRect()
    }
}
