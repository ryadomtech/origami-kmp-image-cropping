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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import tech.ryadom.origami.style.OrigamiHighlightedShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Counts how often a shape is asked to rebuild its path.
 */
private class CountingShape : OrigamiHighlightedShape {
    var calls = 0
        private set

    override fun getPath(rect: Rect): Path {
        calls++
        return Path().apply { addRect(rect) }
    }
}

class OrigamiOverlayCacheTest {

    @Test
    fun rebuilds_the_path_only_when_the_crop_area_moves() {
        val shape = CountingShape()
        val cache = OrigamiOverlayCache(shape)
        val rect = Rect(0f, 0f, 10f, 10f)

        val first = cache.pathFor(rect)
        val second = cache.pathFor(rect)

        assertSame(first, second)
        assertEquals(1, shape.calls)
    }

    @Test
    fun rebuilds_the_path_for_a_new_crop_area() {
        val shape = CountingShape()
        val cache = OrigamiOverlayCache(shape)

        val first = cache.pathFor(Rect(0f, 0f, 10f, 10f))
        val second = cache.pathFor(Rect(0f, 0f, 20f, 20f))

        assertNotSame(first, second)
        assertEquals(2, shape.calls)
    }

    @Test
    fun reuses_a_stroke_of_the_same_width() {
        val cache = OrigamiOverlayCache(CountingShape())

        assertSame(cache.strokeOf(2f), cache.strokeOf(2f))
        assertNotSame(cache.strokeOf(2f), cache.strokeOf(3f))
    }

    @Test
    fun resolves_guideline_width_once_per_density() {
        val cache = OrigamiOverlayCache(CountingShape())

        assertEquals(4f, cache.guidelinesWidthPx(2.dp, Density(2f)))
        assertEquals(4f, cache.guidelinesWidthPx(2.dp, Density(2f)))
        assertEquals(6f, cache.guidelinesWidthPx(2.dp, Density(3f)))
        assertEquals(9f, cache.guidelinesWidthPx(3.dp, Density(3f)))
    }
}
