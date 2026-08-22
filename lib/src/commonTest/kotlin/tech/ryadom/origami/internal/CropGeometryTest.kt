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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TOLERANCE = 0.01f

private fun assertRect(expected: Rect, actual: Rect, message: String = "") {
    assertEquals(expected.left, actual.left, TOLERANCE, "$message left")
    assertEquals(expected.top, actual.top, TOLERANCE, "$message top")
    assertEquals(expected.right, actual.right, TOLERANCE, "$message right")
    assertEquals(expected.bottom, actual.bottom, TOLERANCE, "$message bottom")
}

class CropGeometryImageBoundsTest {

    @Test
    fun letterboxes_a_wide_image_inside_a_square_container() {
        val bounds = CropGeometry.imageBoundsIn(
            container = Rect(0f, 0f, 100f, 100f),
            sourceWidth = 200,
            sourceHeight = 100
        )

        assertRect(Rect(0f, 25f, 100f, 75f), bounds)
    }

    @Test
    fun pillarboxes_a_tall_image_inside_a_square_container() {
        val bounds = CropGeometry.imageBoundsIn(
            container = Rect(0f, 0f, 100f, 100f),
            sourceWidth = 100,
            sourceHeight = 200
        )

        assertRect(Rect(25f, 0f, 75f, 100f), bounds)
    }

    @Test
    fun keeps_the_container_offset() {
        val bounds = CropGeometry.imageBoundsIn(
            container = Rect(30f, 40f, 130f, 140f),
            sourceWidth = 200,
            sourceHeight = 100
        )

        assertRect(Rect(30f, 65f, 130f, 115f), bounds)
    }

    @Test
    fun returns_zero_for_a_degenerate_source() {
        assertEquals(
            Rect.Zero,
            CropGeometry.imageBoundsIn(Rect(0f, 0f, 100f, 100f), sourceWidth = 0, sourceHeight = 10)
        )

        assertEquals(
            Rect.Zero,
            CropGeometry.imageBoundsIn(Rect.Zero, sourceWidth = 10, sourceHeight = 10)
        )
    }
}

class CropGeometryInitialRectTest {

    @Test
    fun centers_a_square_crop_area_in_a_square_image() {
        val rect = CropGeometry.initialCropRect(
            bounds = Rect(0f, 0f, 100f, 100f),
            aspectRatio = 1f,
            minSize = 0f
        ) { maxWidth -> maxWidth * 0.8f }

        assertRect(Rect(10f, 10f, 90f, 90f), rect)
    }

    @Test
    fun honours_the_requested_ratio_inside_a_wide_image() {
        val rect = CropGeometry.initialCropRect(
            bounds = Rect(0f, 0f, 400f, 100f),
            aspectRatio = 1f,
            minSize = 0f
        ) { maxWidth -> maxWidth }

        // The widest 1:1 rect fitting a 400x100 box is 100x100, centered.
        assertRect(Rect(150f, 0f, 250f, 100f), rect)
        assertEquals(1f, rect.width / rect.height, TOLERANCE)
    }

    @Test
    fun never_returns_a_crop_area_smaller_than_the_minimum() {
        val rect = CropGeometry.initialCropRect(
            bounds = Rect(0f, 0f, 100f, 100f),
            aspectRatio = 1f,
            minSize = 40f
        ) { 1f }

        assertEquals(40f, rect.width, TOLERANCE)
        assertEquals(40f, rect.height, TOLERANCE)
    }

    @Test
    fun caps_a_minimum_that_does_not_fit_the_image() {
        val rect = CropGeometry.initialCropRect(
            bounds = Rect(0f, 0f, 50f, 50f),
            aspectRatio = 1f,
            minSize = 500f
        ) { 1f }

        assertTrue(rect.width <= 50f, "crop area escaped the bounds: $rect")
        assertTrue(rect.height <= 50f, "crop area escaped the bounds: $rect")
    }

    @Test
    fun survives_a_bogus_ratio_instead_of_producing_NaN() {
        val rect = CropGeometry.initialCropRect(
            bounds = Rect(0f, 0f, 100f, 100f),
            aspectRatio = 0f,
            minSize = 0f
        ) { maxWidth -> maxWidth }

        assertTrue(rect.width.isFinite() && rect.height.isFinite(), "got $rect")
        assertRect(Rect(0f, 0f, 100f, 100f), rect)
    }

    @Test
    fun returns_zero_for_empty_bounds() {
        assertEquals(
            Rect.Zero,
            CropGeometry.initialCropRect(Rect.Zero, aspectRatio = 1f, minSize = 0f) { it }
        )
    }
}

class CropGeometryMoveTest {

    private val bounds = Rect(0f, 0f, 100f, 100f)

    @Test
    fun translates_freely_while_inside_the_image() {
        val moved = CropGeometry.move(Rect(10f, 10f, 30f, 30f), Offset(5f, 7f), bounds)
        assertRect(Rect(15f, 17f, 35f, 37f), moved)
    }

    @Test
    fun stops_at_the_image_edges_without_shrinking() {
        val moved = CropGeometry.move(Rect(10f, 10f, 30f, 30f), Offset(1000f, 1000f), bounds)

        assertRect(Rect(80f, 80f, 100f, 100f), moved)
        assertEquals(20f, moved.width, TOLERANCE)
        assertEquals(20f, moved.height, TOLERANCE)
    }

    @Test
    fun stops_at_the_top_left_corner() {
        val moved = CropGeometry.move(Rect(10f, 10f, 30f, 30f), Offset(-1000f, -1000f), bounds)
        assertRect(Rect(0f, 0f, 20f, 20f), moved)
    }

    @Test
    fun pins_an_oversized_crop_area_instead_of_producing_an_invalid_range() {
        val moved = CropGeometry.move(Rect(0f, 0f, 300f, 300f), Offset(50f, 50f), bounds)
        assertRect(Rect(0f, 0f, 300f, 300f), moved)
    }
}

class CropGeometryResizeFreeTest {

    private val bounds = Rect(0f, 0f, 100f, 100f)

    @Test
    fun moves_only_the_dragged_corner() {
        val resized = CropGeometry.resizeFree(
            rect = Rect(20f, 20f, 80f, 80f),
            edge = Edge.TopLeft,
            delta = Offset(-10f, -5f),
            bounds = bounds,
            minSize = 10f
        )

        assertRect(Rect(10f, 15f, 80f, 80f), resized)
    }

    @Test
    fun stops_at_the_minimum_instead_of_flipping_past_the_anchor() {
        val resized = CropGeometry.resizeFree(
            rect = Rect(20f, 20f, 80f, 80f),
            edge = Edge.TopLeft,
            delta = Offset(500f, 500f),
            bounds = bounds,
            minSize = 10f
        )

        assertTrue(resized.left < resized.right, "inverted horizontally: $resized")
        assertTrue(resized.top < resized.bottom, "inverted vertically: $resized")
        assertEquals(10f, resized.width, TOLERANCE)
        assertEquals(10f, resized.height, TOLERANCE)
    }

    @Test
    fun clamps_the_dragged_corner_to_the_image() {
        val resized = CropGeometry.resizeFree(
            rect = Rect(20f, 20f, 80f, 80f),
            edge = Edge.BottomRight,
            delta = Offset(500f, 500f),
            bounds = bounds,
            minSize = 10f
        )

        assertRect(Rect(20f, 20f, 100f, 100f), resized)
    }

    @Test
    fun keeps_the_anchor_corner_fixed_for_every_edge() {
        val rect = Rect(20f, 20f, 80f, 80f)

        Edge.entries.forEach { edge ->
            val anchor = rect.oppositeCornerOf(edge)
            val resized = CropGeometry.resizeFree(rect, edge, Offset(-13f, 7f), bounds, 10f)

            assertTrue(
                anchor.x in listOf(resized.left, resized.right) &&
                        anchor.y in listOf(resized.top, resized.bottom),
                "anchor $anchor moved for $edge: $resized"
            )
        }
    }
}

class CropGeometryResizeFixedRatioTest {

    private val bounds = Rect(0f, 0f, 200f, 200f)

    @Test
    fun preserves_the_ratio_while_growing() {
        val resized = CropGeometry.resizeFixedRatio(
            rect = Rect(50f, 50f, 100f, 100f),
            edge = Edge.BottomRight,
            delta = Offset(40f, 40f),
            bounds = bounds,
            aspectRatio = 1f,
            minSize = 10f
        )

        assertEquals(1f, resized.width / resized.height, TOLERANCE)
        assertRect(Rect(50f, 50f, 140f, 140f), resized)
    }

    @Test
    fun preserves_a_non_square_ratio() {
        val resized = CropGeometry.resizeFixedRatio(
            rect = Rect(0f, 0f, 40f, 20f),
            edge = Edge.BottomRight,
            delta = Offset(40f, 0f),
            bounds = bounds,
            aspectRatio = 2f,
            minSize = 10f
        )

        assertEquals(2f, resized.width / resized.height, TOLERANCE)
    }

    @Test
    fun keeps_the_ratio_when_the_drag_runs_off_the_image() {
        val resized = CropGeometry.resizeFixedRatio(
            rect = Rect(50f, 50f, 100f, 100f),
            edge = Edge.BottomRight,
            delta = Offset(5000f, 5000f),
            bounds = bounds,
            aspectRatio = 1f,
            minSize = 10f
        )

        assertEquals(1f, resized.width / resized.height, TOLERANCE)
        assertTrue(bounds.contains(resized.topLeft), "escaped bounds: $resized")
        assertTrue(resized.right <= bounds.right + TOLERANCE, "escaped bounds: $resized")
        assertTrue(resized.bottom <= bounds.bottom + TOLERANCE, "escaped bounds: $resized")
        // The anchor is at (50, 50) and the image ends at 200, so 150 is all the room there is.
        assertEquals(150f, resized.width, TOLERANCE)
    }

    @Test
    fun respects_the_minimum_size_when_shrunk_to_nothing() {
        val resized = CropGeometry.resizeFixedRatio(
            rect = Rect(50f, 50f, 100f, 100f),
            edge = Edge.TopLeft,
            delta = Offset(5000f, 5000f),
            bounds = bounds,
            aspectRatio = 1f,
            minSize = 20f
        )

        assertEquals(20f, resized.width, TOLERANCE)
        assertEquals(20f, resized.height, TOLERANCE)
    }

    @Test
    fun anchors_the_opposite_corner_for_every_edge() {
        val rect = Rect(60f, 60f, 120f, 120f)

        Edge.entries.forEach { edge ->
            val anchor = rect.oppositeCornerOf(edge)
            val resized = CropGeometry.resizeFixedRatio(
                rect = rect,
                edge = edge,
                delta = Offset(10f, 10f),
                bounds = bounds,
                aspectRatio = 1f,
                minSize = 10f
            )

            assertTrue(
                anchor.x in listOf(resized.left, resized.right) &&
                        anchor.y in listOf(resized.top, resized.bottom),
                "anchor $anchor moved for $edge: $resized"
            )
            assertEquals(1f, resized.width / resized.height, TOLERANCE, "ratio drifted for $edge")
        }
    }
}

class CropGeometryRemapTest {

    @Test
    fun scales_the_selection_with_the_container() {
        val remapped = CropGeometry.remap(
            rect = Rect(10f, 10f, 50f, 50f),
            from = Rect(0f, 0f, 100f, 100f),
            to = Rect(0f, 0f, 200f, 200f)
        )

        assertRect(Rect(20f, 20f, 100f, 100f), remapped)
    }

    @Test
    fun follows_a_translated_container() {
        val remapped = CropGeometry.remap(
            rect = Rect(10f, 10f, 50f, 50f),
            from = Rect(0f, 0f, 100f, 100f),
            to = Rect(500f, 300f, 600f, 400f)
        )

        assertRect(Rect(510f, 310f, 550f, 350f), remapped)
    }

    @Test
    fun is_a_no_op_for_an_unchanged_container() {
        val rect = Rect(10f, 10f, 50f, 50f)
        val bounds = Rect(0f, 0f, 100f, 100f)

        assertRect(rect, CropGeometry.remap(rect, bounds, bounds))
    }
}

class CropGeometryToSourceRectTest {

    @Test
    fun maps_a_full_selection_onto_the_whole_bitmap() {
        val bounds = Rect(0f, 0f, 100f, 50f)
        val sourceRect = CropGeometry.toSourceRect(bounds, bounds, 400, 200)

        assertNotNull(sourceRect)
        assertEquals(0, sourceRect.left)
        assertEquals(0, sourceRect.top)
        assertEquals(400, sourceRect.right)
        assertEquals(200, sourceRect.bottom)
    }

    @Test
    fun scales_a_partial_selection() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        val sourceRect = CropGeometry.toSourceRect(Rect(25f, 50f, 75f, 100f), bounds, 400, 400)

        assertNotNull(sourceRect)
        assertEquals(100, sourceRect.left)
        assertEquals(200, sourceRect.top)
        assertEquals(300, sourceRect.right)
        assertEquals(400, sourceRect.bottom)
    }

    @Test
    fun accounts_for_the_image_offset_inside_the_container() {
        val bounds = Rect(50f, 20f, 150f, 120f)
        val sourceRect = CropGeometry.toSourceRect(Rect(50f, 20f, 100f, 70f), bounds, 200, 200)

        assertNotNull(sourceRect)
        assertEquals(0, sourceRect.left)
        assertEquals(0, sourceRect.top)
        assertEquals(100, sourceRect.right)
        assertEquals(100, sourceRect.bottom)
    }

    @Test
    fun clips_a_selection_that_hangs_over_the_image() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        val sourceRect = CropGeometry.toSourceRect(Rect(-50f, -50f, 50f, 50f), bounds, 100, 100)

        assertNotNull(sourceRect)
        assertEquals(0, sourceRect.left)
        assertEquals(0, sourceRect.top)
        assertEquals(50, sourceRect.right)
        assertEquals(50, sourceRect.bottom)
    }

    @Test
    fun returns_null_when_the_selection_misses_the_image() {
        val bounds = Rect(0f, 0f, 100f, 100f)

        assertNull(CropGeometry.toSourceRect(Rect(200f, 200f, 300f, 300f), bounds, 100, 100))
    }

    @Test
    fun returns_null_for_a_degenerate_source() {
        val bounds = Rect(0f, 0f, 100f, 100f)

        assertNull(CropGeometry.toSourceRect(bounds, bounds, 0, 100))
        assertNull(CropGeometry.toSourceRect(bounds, Rect.Zero, 100, 100))
    }

    @Test
    fun never_rounds_a_side_outside_the_bitmap() {
        // A fractional edge must never round outward past the bitmap.
        val sourceWidth = 1000
        val sourceHeight = 777

        repeat(400) { step ->
            val size = 37f + step * 0.137f
            val bounds = Rect(step * 0.011f, step * 0.037f, step * 0.011f + size, step * 0.037f + size)
            val sourceRect = CropGeometry.toSourceRect(bounds, bounds, sourceWidth, sourceHeight)

            assertNotNull(sourceRect, "no rect for step $step")
            assertTrue(sourceRect.left >= 0, "left escaped at step $step: $sourceRect")
            assertTrue(sourceRect.top >= 0, "top escaped at step $step: $sourceRect")
            assertTrue(sourceRect.right <= sourceWidth, "right escaped at step $step: $sourceRect")
            assertTrue(sourceRect.bottom <= sourceHeight, "bottom escaped at step $step: $sourceRect")
            assertTrue(sourceRect.width >= 1, "empty width at step $step: $sourceRect")
            assertTrue(sourceRect.height >= 1, "empty height at step $step: $sourceRect")
        }
    }

    @Test
    fun always_yields_at_least_one_pixel() {
        val bounds = Rect(0f, 0f, 1000f, 1000f)
        val sourceRect = CropGeometry.toSourceRect(Rect(0f, 0f, 0.4f, 0.4f), bounds, 10, 10)

        assertNotNull(sourceRect)
        assertTrue(sourceRect.width >= 1 && sourceRect.height >= 1, "empty: $sourceRect")
    }
}
