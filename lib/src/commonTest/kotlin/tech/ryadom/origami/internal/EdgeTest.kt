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
import kotlin.test.assertNull

class EdgeTest {

    private val rect = Rect(0f, 0f, 100f, 100f)

    @Test
    fun resolves_each_corner_from_a_touch_right_on_it() {
        assertEquals(Edge.TopLeft, rect.findEdgeContaining(Offset(0f, 0f), tolerance = 10f))
        assertEquals(Edge.TopRight, rect.findEdgeContaining(Offset(100f, 0f), tolerance = 10f))
        assertEquals(Edge.BottomLeft, rect.findEdgeContaining(Offset(0f, 100f), tolerance = 10f))
        assertEquals(Edge.BottomRight, rect.findEdgeContaining(Offset(100f, 100f), tolerance = 10f))
    }

    @Test
    fun resolves_a_touch_near_a_corner() {
        assertEquals(Edge.BottomRight, rect.findEdgeContaining(Offset(96f, 103f), tolerance = 10f))
    }

    @Test
    fun returns_null_for_a_touch_in_the_middle() {
        assertNull(rect.findEdgeContaining(Offset(50f, 50f), tolerance = 10f))
    }

    @Test
    fun returns_null_for_a_touch_outside_the_tolerance() {
        assertNull(rect.findEdgeContaining(Offset(-20f, -20f), tolerance = 10f))
    }

    @Test
    fun picks_the_nearest_corner_when_touch_regions_overlap() {
        // Every corner is a candidate once the area is smaller than the touch target.
        val tiny = Rect(0f, 0f, 20f, 20f)

        assertEquals(Edge.TopLeft, tiny.findEdgeContaining(Offset(2f, 2f), tolerance = 40f))
        assertEquals(Edge.TopRight, tiny.findEdgeContaining(Offset(18f, 2f), tolerance = 40f))
        assertEquals(Edge.BottomLeft, tiny.findEdgeContaining(Offset(2f, 18f), tolerance = 40f))
        assertEquals(Edge.BottomRight, tiny.findEdgeContaining(Offset(18f, 18f), tolerance = 40f))
    }

    @Test
    fun returns_null_for_a_zero_tolerance_near_miss() {
        assertNull(rect.findEdgeContaining(Offset(1f, 1f), tolerance = 0f))
    }

    @Test
    fun corners_and_their_opposites_line_up() {
        Edge.entries.forEach { edge ->
            val corner = rect.cornerOf(edge)
            val opposite = rect.oppositeCornerOf(edge)

            assertEquals(100f, corner.x + opposite.x, 0.001f, "$edge x")
            assertEquals(100f, corner.y + opposite.y, 0.001f, "$edge y")
        }
    }
}
