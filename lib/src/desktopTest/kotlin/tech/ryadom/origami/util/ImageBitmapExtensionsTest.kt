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

package tech.ryadom.origami.util

import androidx.compose.ui.unit.IntRect
import tech.ryadom.origami.util.extensions.cropTo
import tech.ryadom.origami.util.extensions.flipped
import tech.ryadom.origami.util.extensions.rotated
import tech.ryadom.origami.util.extensions.scaledToFit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageBitmapCropTest {

    @Test
    fun copies_exactly_the_requested_region() {
        val cropped = TestBitmaps.quadrants(width = 4, height = 4)
            .cropTo(IntRect(left = 2, top = 0, right = 4, bottom = 2))

        assertEquals(2, cropped.width)
        assertEquals(2, cropped.height)
        cropped.assertPixel(0, 0, TestBitmaps.TopRight)
        cropped.assertPixel(1, 1, TestBitmaps.TopRight)
    }

    @Test
    fun clamps_an_out_of_bounds_rect_instead_of_throwing() {
        // Crop rects come from rounded layout coordinates and can overshoot by a pixel.
        val source = TestBitmaps.quadrants(width = 4, height = 4)
        val cropped = source.cropTo(IntRect(left = -5, top = -5, right = 9, bottom = 9))

        assertEquals(4, cropped.width)
        assertEquals(4, cropped.height)
    }

    @Test
    fun never_produces_an_empty_bitmap() {
        val cropped = TestBitmaps.quadrants(width = 4, height = 4)
            .cropTo(IntRect(left = 2, top = 2, right = 2, bottom = 2))

        assertTrue(cropped.width >= 1 && cropped.height >= 1, "${cropped.width}x${cropped.height}")
    }

    @Test
    fun leaves_the_source_untouched() {
        val source = TestBitmaps.quadrants(width = 4, height = 4)
        source.cropTo(IntRect(0, 0, 2, 2))

        assertEquals(4, source.width)
        source.assertPixel(3, 3, TestBitmaps.BottomRight)
    }
}

class ImageBitmapRotationTest {

    @Test
    fun a_quarter_turn_moves_each_quadrant_clockwise() {
        val rotated = TestBitmaps.quadrants(width = 4, height = 4).rotated(90)

        rotated.assertPixel(3, 0, TestBitmaps.TopLeft, "top left should land top right")
        rotated.assertPixel(3, 3, TestBitmaps.TopRight, "top right should land bottom right")
        rotated.assertPixel(0, 3, TestBitmaps.BottomRight, "bottom right should land bottom left")
        rotated.assertPixel(0, 0, TestBitmaps.BottomLeft, "bottom left should land top left")
    }

    @Test
    fun a_quarter_turn_swaps_the_dimensions() {
        val rotated = TestBitmaps.quadrants(width = 8, height = 4).rotated(90)

        assertEquals(4, rotated.width)
        assertEquals(8, rotated.height)
    }

    @Test
    fun a_half_turn_keeps_the_dimensions() {
        val rotated = TestBitmaps.quadrants(width = 8, height = 4).rotated(180)

        assertEquals(8, rotated.width)
        assertEquals(4, rotated.height)
        rotated.assertPixel(7, 3, TestBitmaps.TopLeft)
    }

    @Test
    fun four_quarter_turns_come_back_to_the_original() {
        val source = TestBitmaps.quadrants(width = 4, height = 4)
        val roundTrip = source.rotated(90).rotated(90).rotated(90).rotated(90)

        assertEquals(source.width, roundTrip.width)
        assertEquals(source.height, roundTrip.height)
        roundTrip.assertPixel(0, 0, TestBitmaps.TopLeft)
        roundTrip.assertPixel(3, 0, TestBitmaps.TopRight)
        roundTrip.assertPixel(0, 3, TestBitmaps.BottomLeft)
        roundTrip.assertPixel(3, 3, TestBitmaps.BottomRight)
    }

    @Test
    fun a_counter_clockwise_turn_is_the_inverse_of_a_clockwise_one() {
        val source = TestBitmaps.quadrants(width = 4, height = 4)
        val roundTrip = source.rotated(90).rotated(270)

        roundTrip.assertPixel(0, 0, TestBitmaps.TopLeft)
        roundTrip.assertPixel(3, 3, TestBitmaps.BottomRight)
    }

    @Test
    fun a_zero_rotation_is_free() {
        val source = TestBitmaps.quadrants()

        assertSame(source, source.rotated(0))
        assertSame(source, source.rotated(360))
        assertSame(source, source.rotated(-360))
    }

    @Test
    fun normalizes_a_negative_rotation() {
        val source = TestBitmaps.quadrants(width = 4, height = 4)

        // -90 is the same as 270.
        source.rotated(-90).assertPixel(0, 0, TestBitmaps.TopRight)
        source.rotated(270).assertPixel(0, 0, TestBitmaps.TopRight)
    }
}

class ImageBitmapFlipTest {

    @Test
    fun a_horizontal_flip_mirrors_left_and_right() {
        val flipped = TestBitmaps.quadrants(width = 4, height = 4).flipped(horizontal = true)

        flipped.assertPixel(3, 0, TestBitmaps.TopLeft)
        flipped.assertPixel(0, 0, TestBitmaps.TopRight)
        flipped.assertPixel(3, 3, TestBitmaps.BottomLeft)
        flipped.assertPixel(0, 3, TestBitmaps.BottomRight)
    }

    @Test
    fun a_vertical_flip_mirrors_top_and_bottom() {
        val flipped = TestBitmaps.quadrants(width = 4, height = 4).flipped(horizontal = false)

        flipped.assertPixel(0, 3, TestBitmaps.TopLeft)
        flipped.assertPixel(0, 0, TestBitmaps.BottomLeft)
    }

    @Test
    fun flipping_twice_is_the_identity() {
        val roundTrip = TestBitmaps.quadrants(width = 4, height = 4)
            .flipped(horizontal = true)
            .flipped(horizontal = true)

        roundTrip.assertPixel(0, 0, TestBitmaps.TopLeft)
        roundTrip.assertPixel(3, 3, TestBitmaps.BottomRight)
    }

    @Test
    fun keeps_the_dimensions() {
        val flipped = TestBitmaps.quadrants(width = 8, height = 4).flipped(horizontal = true)

        assertEquals(8, flipped.width)
        assertEquals(4, flipped.height)
    }
}

class ImageBitmapScalingTest {

    @Test
    fun scales_a_large_bitmap_down_to_the_limit() {
        val scaled = TestBitmaps.quadrants(width = 800, height = 400).scaledToFit(200)

        assertEquals(200, scaled.width)
        assertEquals(100, scaled.height)
    }

    @Test
    fun preserves_the_aspect_ratio_of_a_tall_bitmap() {
        val scaled = TestBitmaps.quadrants(width = 100, height = 400).scaledToFit(200)

        assertEquals(50, scaled.width)
        assertEquals(200, scaled.height)
    }

    @Test
    fun leaves_a_bitmap_that_already_fits_alone() {
        val source = TestBitmaps.quadrants(width = 64, height = 64)

        assertSame(source, source.scaledToFit(200))
        assertSame(source, source.scaledToFit(64))
    }

    @Test
    fun treats_a_non_positive_limit_as_no_limit() {
        val source = TestBitmaps.quadrants(width = 64, height = 64)

        assertSame(source, source.scaledToFit(0))
        assertSame(source, source.scaledToFit(-10))
    }
}
