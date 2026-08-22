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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import tech.ryadom.origami.shared.ImageCompressor
import tech.ryadom.origami.style.OrigamiAspectRatio
import tech.ryadom.origami.style.OrigamiColors
import tech.ryadom.origami.style.OrigamiCropArea
import tech.ryadom.origami.util.TestBitmaps
import tech.ryadom.origami.util.assertPixel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val TOLERANCE = 0.5f

/**
 * Drives [Origami] through the same internal callbacks [OrigamiImage] uses, so the state machine
 * can be exercised without standing up a Compose UI.
 */
private class OrigamiHarness(
    bitmap: ImageBitmap = TestBitmaps.quadrants(width = 200, height = 200),
    aspectRatio: OrigamiAspectRatio = OrigamiAspectRatio(),
    cropArea: OrigamiCropArea = OrigamiCropArea()
) {
    val origami = Origami(
        imageBitmap = bitmap,
        colors = OrigamiColors.createDefault(),
        cropArea = cropArea,
        aspectRatio = aspectRatio,
        // The platform compressor would rescale the source and shift every expected number
        compressor = ImageCompressor.Original()
    )

    init {
        origami.onDensityChanged(Density(density = 1f))
    }

    fun layout(width: Int = 400, height: Int = 400, left: Float = 0f, top: Float = 0f) {
        origami.onGloballyPositioned(
            topLeft = Offset(left, top),
            size = IntSize(width, height)
        )
    }

    /**
     * Replays a press, a straight drag and a release one step at a time.
     */
    fun drag(from: Offset, to: Offset, steps: Int = 8) {
        origami.onDragStart(from)

        repeat(steps) { index ->
            val progress = (index + 1).toFloat() / steps
            origami.onDrag(from + (to - from) * progress)
        }

        origami.onDragEnd()
    }
}

class OrigamiLayoutTest {

    @Test
    fun has_no_crop_area_before_the_first_layout_pass() {
        val harness = OrigamiHarness()

        assertEquals(Rect.Zero, harness.origami.cropRect)
    }

    @Test
    fun returns_the_source_untouched_when_cropped_before_layout() {
        val bitmap = TestBitmaps.quadrants(width = 200, height = 200)
        val harness = OrigamiHarness(bitmap)

        assertSame(bitmap, harness.origami.crop())
    }

    @Test
    fun centers_the_crop_area_using_the_configured_padding() {
        val harness = OrigamiHarness()
        harness.layout(width = 400, height = 400)

        // Default padding leaves 15% free, so the area starts at 85% of a 400x400 image.
        val rect = harness.origami.cropRect
        assertEquals(30f, rect.left, TOLERANCE)
        assertEquals(30f, rect.top, TOLERANCE)
        assertEquals(340f, rect.width, TOLERANCE)
        assertEquals(340f, rect.height, TOLERANCE)
    }

    @Test
    fun fits_the_crop_area_to_the_letterboxed_image_not_the_container() {
        // A 200x100 image in a 400x400 box is drawn as 400x200, centered vertically.
        val harness = OrigamiHarness(TestBitmaps.quadrants(width = 200, height = 100))
        harness.layout(width = 400, height = 400)

        val rect = harness.origami.cropRect
        assertTrue(rect.top >= 100f - TOLERANCE, "crop area drifted above the image: $rect")
        assertTrue(rect.bottom <= 300f + TOLERANCE, "crop area drifted below the image: $rect")
    }

    @Test
    fun keeps_the_selection_when_the_container_is_resized() {
        val harness = OrigamiHarness()
        harness.layout(width = 400, height = 400)
        harness.drag(from = Offset(200f, 200f), to = Offset(160f, 160f))

        val before = harness.origami.cropRect
        harness.layout(width = 800, height = 800)
        val after = harness.origami.cropRect

        // Everything doubled, so the selection should have doubled with it rather than reset.
        assertEquals(before.left * 2f, after.left, TOLERANCE)
        assertEquals(before.width * 2f, after.width, TOLERANCE)
    }

    @Test
    fun ignores_a_layout_pass_that_changed_nothing() {
        val harness = OrigamiHarness()
        harness.layout()
        harness.drag(from = Offset(200f, 200f), to = Offset(150f, 150f))

        val dragged = harness.origami.cropRect
        harness.layout()

        assertEquals(dragged, harness.origami.cropRect)
    }
}

class OrigamiDragTest {

    @Test
    fun a_drag_inside_the_area_moves_it_without_resizing() {
        val harness = OrigamiHarness()
        harness.layout()

        val before = harness.origami.cropRect
        harness.drag(from = Offset(200f, 200f), to = Offset(180f, 190f))
        val after = harness.origami.cropRect

        assertEquals(before.left - 20f, after.left, TOLERANCE)
        assertEquals(before.top - 10f, after.top, TOLERANCE)
        assertEquals(before.width, after.width, TOLERANCE)
        assertEquals(before.height, after.height, TOLERANCE)
    }

    @Test
    fun a_drag_never_pushes_the_area_off_the_image() {
        val harness = OrigamiHarness()
        harness.layout()

        harness.drag(from = Offset(200f, 200f), to = Offset(5000f, 5000f))
        val rect = harness.origami.cropRect

        assertTrue(rect.right <= 400f + TOLERANCE, "escaped right: $rect")
        assertTrue(rect.bottom <= 400f + TOLERANCE, "escaped bottom: $rect")
    }

    @Test
    fun a_drag_that_leaves_and_returns_ends_where_the_finger_is() {
        // Running into an edge and coming back must not leave the area behind the pointer.
        val harness = OrigamiHarness()
        harness.layout()

        val before = harness.origami.cropRect

        harness.origami.onDragStart(Offset(200f, 200f))
        harness.origami.onDrag(Offset(5000f, 200f))
        harness.origami.onDrag(Offset(200f, 200f))
        harness.origami.onDragEnd()

        assertEquals(before.left, harness.origami.cropRect.left, TOLERANCE)
    }

    @Test
    fun a_drag_outside_the_area_changes_nothing() {
        val harness = OrigamiHarness()
        harness.layout()

        // Well clear of the area and of every corner handle.
        val before = harness.origami.cropRect
        harness.drag(from = Offset(5f, 200f), to = Offset(100f, 200f))

        assertEquals(before, harness.origami.cropRect)
    }

    @Test
    fun a_corner_drag_resizes_and_keeps_a_locked_ratio() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Square)
        harness.layout()

        val corner = harness.origami.cropRect.bottomRight
        harness.drag(from = corner, to = corner - Offset(60f, 20f))

        val rect = harness.origami.cropRect
        assertEquals(1f, rect.width / rect.height, 0.01f, "ratio drifted: $rect")
        assertTrue(rect.width < 340f, "area did not shrink: $rect")
    }

    @Test
    fun a_corner_drag_with_a_free_ratio_reshapes_the_area() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Free)
        harness.layout()

        val corner = harness.origami.cropRect.bottomRight
        harness.drag(from = corner, to = corner - Offset(100f, 0f))

        val rect = harness.origami.cropRect
        assertNotEquals(1f, rect.width / rect.height, "free ratio stayed square: $rect")
    }

    @Test
    fun a_corner_dragged_past_its_anchor_stops_at_the_minimum_size() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Free)
        harness.layout()

        val corner = harness.origami.cropRect.topLeft
        harness.drag(from = corner, to = corner + Offset(5000f, 5000f))

        val rect = harness.origami.cropRect
        assertTrue(rect.width > 0f && rect.height > 0f, "area inverted: $rect")
        // Default minSize is 56.dp, which is 56px at a density of 1.
        assertEquals(56f, rect.width, TOLERANCE)
        assertEquals(56f, rect.height, TOLERANCE)
    }

    @Test
    fun a_cancelled_drag_does_not_stay_armed() {
        val harness = OrigamiHarness()
        harness.layout()

        harness.origami.onDragStart(Offset(200f, 200f))
        harness.origami.onDragEnd()

        val before = harness.origami.cropRect
        harness.origami.onDrag(Offset(50f, 50f))

        assertEquals(before, harness.origami.cropRect)
    }
}

class OrigamiCropTest {

    @Test
    fun crops_the_region_the_selection_covers() {
        val harness = OrigamiHarness()
        harness.layout(width = 400, height = 400)

        // 340 screen pixels of a 200px bitmap drawn at 400px is 170 source pixels.
        val cropped = harness.origami.crop()

        assertEquals(170, cropped.width)
        assertEquals(170, cropped.height)
    }

    @Test
    fun crops_the_whole_image_when_the_selection_covers_it() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Free)
        harness.layout(width = 400, height = 400)

        val corner = harness.origami.cropRect.bottomRight
        harness.drag(from = corner, to = Offset(5000f, 5000f))
        harness.drag(from = harness.origami.cropRect.topLeft, to = Offset(-5000f, -5000f))

        val cropped = harness.origami.crop()

        assertEquals(200, cropped.width)
        assertEquals(200, cropped.height)
    }

    @Test
    fun cropping_at_the_image_edge_does_not_throw() {
        // A selection flush against the image edge must not round past the bitmap.
        val harness = OrigamiHarness(
            bitmap = TestBitmaps.quadrants(width = 199, height = 101),
            aspectRatio = OrigamiAspectRatio.Free
        )

        listOf(377, 401, 433, 512).forEach { size ->
            harness.layout(width = size, height = size)
            harness.drag(
                from = harness.origami.cropRect.bottomRight,
                to = Offset(size * 2f, size * 2f)
            )

            val cropped = harness.origami.crop()
            assertTrue(cropped.width in 1..199, "bad width ${cropped.width} at $size")
            assertTrue(cropped.height in 1..101, "bad height ${cropped.height} at $size")
        }
    }

    @Test
    fun crops_the_correct_pixels() {
        val harness = OrigamiHarness(
            bitmap = TestBitmaps.quadrants(width = 200, height = 200),
            aspectRatio = OrigamiAspectRatio.Free
        )
        harness.layout(width = 200, height = 200)

        // Shrink the selection into the bottom right quadrant.
        harness.drag(
            from = harness.origami.cropRect.topLeft,
            to = Offset(120f, 120f)
        )
        harness.drag(
            from = harness.origami.cropRect.bottomRight,
            to = Offset(200f, 200f)
        )

        val cropped = harness.origami.crop()

        assertEquals(80, cropped.width)
        cropped.assertPixel(1, 1, TestBitmaps.BottomRight)
        cropped.assertPixel(78, 78, TestBitmaps.BottomRight)
    }
}

class OrigamiTransformTest {

    @Test
    fun a_rotation_swaps_the_displayed_dimensions() {
        val harness = OrigamiHarness(TestBitmaps.quadrants(width = 200, height = 100))
        harness.layout()

        harness.origami.rotateClockwise()

        val rotated = harness.origami.transformedBitmap
        assertNotNull(rotated)
        assertEquals(100, rotated.width)
        assertEquals(200, rotated.height)
        assertEquals(90, harness.origami.rotationDegrees)
    }

    @Test
    fun four_rotations_come_back_to_zero_degrees() {
        val harness = OrigamiHarness()
        harness.layout()

        repeat(4) { harness.origami.rotateClockwise() }

        assertEquals(0, harness.origami.rotationDegrees)
    }

    @Test
    fun a_counter_clockwise_rotation_reports_270_degrees() {
        val harness = OrigamiHarness()
        harness.layout()

        harness.origami.rotateCounterClockwise()

        assertEquals(270, harness.origami.rotationDegrees)
    }

    @Test
    fun a_rotation_refits_the_crop_area_to_the_new_shape() {
        val harness = OrigamiHarness(
            bitmap = TestBitmaps.quadrants(width = 200, height = 100),
            aspectRatio = OrigamiAspectRatio.Free
        )
        harness.layout(width = 400, height = 400)

        val before = harness.origami.cropRect
        harness.origami.rotateClockwise()
        harness.layout(width = 400, height = 400)

        val after = harness.origami.cropRect
        assertNotEquals(before, after)
        // The landscape selection should have become a portrait one.
        assertTrue(before.width > before.height, "expected landscape, got \$before")
        assertTrue(after.height > after.width, "expected portrait, got \$after")
    }

    @Test
    fun a_flip_keeps_the_dimensions_and_mirrors_the_pixels() {
        val harness = OrigamiHarness(TestBitmaps.quadrants(width = 4, height = 4))
        harness.layout()

        harness.origami.flipHorizontally()

        val flipped = harness.origami.transformedBitmap
        assertNotNull(flipped)
        assertEquals(4, flipped.width)
        assertEquals(4, flipped.height)
        assertEquals(0, harness.origami.rotationDegrees)
    }

    @Test
    fun reset_drops_every_transform_and_restores_the_initial_area() {
        val harness = OrigamiHarness()
        harness.layout()

        val initial = harness.origami.cropRect
        harness.origami.rotateClockwise()
        harness.origami.flipVertically()
        harness.drag(from = Offset(200f, 200f), to = Offset(150f, 150f))

        harness.origami.reset()
        harness.layout()

        assertNull(harness.origami.transformedBitmap)
        assertEquals(0, harness.origami.rotationDegrees)
        assertEquals(initial, harness.origami.cropRect)
    }
}

class OrigamiAspectRatioChangeTest {

    @Test
    fun starts_locked_to_the_configured_ratio() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Landscape16x9)

        assertEquals(16f / 9f, harness.origami.aspectRatio)
    }

    @Test
    fun starts_unlocked_for_a_variable_ratio() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Free)

        assertNull(harness.origami.aspectRatio)
    }

    @Test
    fun changing_the_ratio_reshapes_the_crop_area() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Square)
        harness.layout()

        harness.origami.setAspectRatio(OrigamiAspectRatio.Landscape16x9)

        val rect = harness.origami.cropRect
        assertEquals(16f / 9f, rect.width / rect.height, 0.01f, "got $rect")
    }

    @Test
    fun unlocking_the_ratio_lets_the_area_cover_the_image() {
        val harness = OrigamiHarness(TestBitmaps.quadrants(width = 200, height = 100))
        harness.layout(width = 400, height = 400)

        harness.origami.setAspectRatio(OrigamiAspectRatio.Free)

        val rect = harness.origami.cropRect
        assertEquals(2f, rect.width / rect.height, 0.01f, "should match the image: $rect")
    }

    @Test
    fun ignores_a_nonsense_ratio() {
        val harness = OrigamiHarness(aspectRatio = OrigamiAspectRatio.Square)
        harness.layout()

        harness.origami.aspectRatio = 0f
        harness.origami.aspectRatio = Float.NaN
        harness.origami.aspectRatio = -3f

        assertEquals(1f, harness.origami.aspectRatio)
    }
}

class OrigamiSavedStateTest {

    @Test
    fun a_selection_survives_a_save_and_restore_into_the_same_layout() {
        val harness = OrigamiHarness()
        harness.layout()
        harness.drag(from = Offset(200f, 200f), to = Offset(170f, 210f))

        val saved = harness.origami.normalizedSelection()
        assertNotNull(saved)

        val restored = OrigamiHarness()
        restored.origami.restoreState(selection = saved, rotation = 0, ratio = 1f)
        restored.layout()

        assertEquals(harness.origami.cropRect.left, restored.origami.cropRect.left, TOLERANCE)
        assertEquals(harness.origami.cropRect.width, restored.origami.cropRect.width, TOLERANCE)
    }

    @Test
    fun a_selection_scales_when_restored_into_a_different_layout() {
        val harness = OrigamiHarness()
        harness.layout(width = 400, height = 400)
        harness.drag(from = Offset(200f, 200f), to = Offset(170f, 210f))

        val saved = harness.origami.normalizedSelection()
        assertNotNull(saved)

        val restored = OrigamiHarness()
        restored.origami.restoreState(selection = saved, rotation = 0, ratio = 1f)
        restored.layout(width = 800, height = 800)

        assertEquals(harness.origami.cropRect.left * 2f, restored.origami.cropRect.left, TOLERANCE)
        assertEquals(harness.origami.cropRect.width * 2f, restored.origami.cropRect.width, TOLERANCE)
    }

    @Test
    fun a_rotation_is_replayed_on_restore() {
        val restored = OrigamiHarness(TestBitmaps.quadrants(width = 200, height = 100))
        restored.origami.restoreState(selection = null, rotation = 90, ratio = null)
        restored.layout()

        assertEquals(90, restored.origami.rotationDegrees)

        val bitmap = restored.origami.transformedBitmap
        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    @Test
    fun restoring_nothing_falls_back_to_the_initial_area() {
        val fresh = OrigamiHarness()
        fresh.layout()

        val restored = OrigamiHarness()
        restored.origami.restoreState(selection = null, rotation = 0, ratio = 1f)
        restored.layout()

        assertEquals(fresh.origami.cropRect, restored.origami.cropRect)
    }
}
