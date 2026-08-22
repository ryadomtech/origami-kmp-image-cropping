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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrigamiAspectRatioTest {

    @Test
    fun rejects_a_non_positive_ratio() {
        assertFailsWith<IllegalArgumentException> { OrigamiAspectRatio(aspectRatio = 0f) }
        assertFailsWith<IllegalArgumentException> { OrigamiAspectRatio(aspectRatio = -1f) }
    }

    @Test
    fun rejects_a_non_finite_ratio() {
        assertFailsWith<IllegalArgumentException> { OrigamiAspectRatio(aspectRatio = Float.NaN) }
        assertFailsWith<IllegalArgumentException> {
            OrigamiAspectRatio(aspectRatio = Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun exposes_the_usual_presets() {
        assertEquals(1f, OrigamiAspectRatio.Square.aspectRatio)
        assertEquals(4f / 3f, OrigamiAspectRatio.Landscape4x3.aspectRatio)
        assertEquals(3f / 4f, OrigamiAspectRatio.Portrait3x4.aspectRatio)
        assertEquals(16f / 9f, OrigamiAspectRatio.Landscape16x9.aspectRatio)
        assertEquals(9f / 16f, OrigamiAspectRatio.Portrait9x16.aspectRatio)

        assertTrue(OrigamiAspectRatio.Free.isVariable)
        assertTrue(OrigamiAspectRatio.Square.isVariable.not())
    }

    @Test
    fun builds_an_arbitrary_ratio() {
        assertEquals(1.5f, OrigamiAspectRatio.of(width = 3, height = 2).aspectRatio)
        assertFailsWith<IllegalArgumentException> { OrigamiAspectRatio.of(width = 0, height = 2) }
        assertFailsWith<IllegalArgumentException> { OrigamiAspectRatio.of(width = 3, height = -2) }
    }
}

class OrigamiCompressionTest {

    @Test
    fun rejects_a_step_that_would_never_terminate() {
        assertFailsWith<IllegalArgumentException> {
            OrigamiCompression(maxSize = 1024, qualityDowngradeStep = 0)
        }

        assertFailsWith<IllegalArgumentException> {
            OrigamiCompression(maxSize = 1024, qualityDowngradeStep = -10)
        }
    }

    @Test
    fun rejects_a_quality_outside_the_encoder_range() {
        assertFailsWith<IllegalArgumentException> { OrigamiCompression(startQuality = 0) }
        assertFailsWith<IllegalArgumentException> { OrigamiCompression(startQuality = 101) }
        assertFailsWith<IllegalArgumentException> { OrigamiCompression(startQuality = -5) }
    }

    @Test
    fun rejects_a_non_positive_max_size() {
        assertFailsWith<IllegalArgumentException> { OrigamiCompression(maxSize = 0) }
        assertFailsWith<IllegalArgumentException> { OrigamiCompression(maxSize = -1) }
    }

    @Test
    fun defaults_do_not_constrain_the_size() {
        assertEquals(OrigamiCompression.UNLIMITED, OrigamiCompression().maxSize)
    }
}

class OrigamiCropAreaTest {

    @Test
    fun rejects_negative_dimensions() {
        assertFailsWith<IllegalArgumentException> { OrigamiCropArea(guidelinesCount = -1) }
        assertFailsWith<IllegalArgumentException> { OrigamiCropArea(guidelinesWidth = (-2).dp) }
        assertFailsWith<IllegalArgumentException> { OrigamiCropArea(minSize = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { OrigamiCropArea(handleTouchTarget = (-1).dp) }
    }

    @Test
    fun defaults_leave_room_for_a_fingertip() {
        val area = OrigamiCropArea()

        assertTrue(area.minSize.value >= 48f, "min size is too small to grab: ${area.minSize}")
        assertTrue(area.handleTouchTarget.value > 0f)
    }
}

class OrigamiCropAreaPaddingTest {

    @Test
    fun factor_padding_leaves_the_requested_share_free() {
        assertEquals(85f, FactorPadding(0.15f).getFactor(100f), 0.001f)
        assertEquals(100f, FactorPadding(0f).getFactor(100f), 0.001f)
    }

    @Test
    fun factor_padding_rejects_a_factor_outside_the_unit_range() {
        assertFailsWith<IllegalArgumentException> { FactorPadding(1.5f) }
        assertFailsWith<IllegalArgumentException> { FactorPadding(-0.1f) }
    }

    @Test
    fun fixed_padding_clamps_instead_of_going_negative() {
        assertEquals(60f, FixedPadding(40f).getFactor(100f), 0.001f)
        assertEquals(0f, FixedPadding(400f).getFactor(100f), 0.001f)
    }

    @Test
    fun fixed_padding_rejects_a_negative_width() {
        assertFailsWith<IllegalArgumentException> { FixedPadding(-1f) }
    }

    @Test
    fun the_default_padding_matches_the_documented_factor() {
        assertEquals(85f, OrigamiCropAreaPadding.createDefault().getFactor(100f), 0.001f)
    }
}

class OrigamiColorsTest {

    @Test
    fun edges_follow_the_guidelines_color_by_default() {
        val colors = OrigamiColors.createDefault()

        assertEquals(colors.guidelinesColor, colors.edgesColor)
    }

    @Test
    fun a_single_color_can_be_overridden() {
        val colors = OrigamiColors.createDefault(guidelinesColor = Color.White)

        assertEquals(Color.White, colors.guidelinesColor)
        assertEquals(Color.White, colors.edgesColor)
        assertEquals(OrigamiColors.createDefault().backgroundColor, colors.backgroundColor)
    }

    @Test
    fun identically_configured_colors_compare_equal() {
        assertEquals(OrigamiColors.createDefault(), OrigamiColors.createDefault())
    }
}
