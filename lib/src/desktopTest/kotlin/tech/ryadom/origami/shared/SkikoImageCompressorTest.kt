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

package tech.ryadom.origami.shared

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.asSkiaBitmap
import tech.ryadom.origami.style.OrigamiCompression
import tech.ryadom.origami.util.TestBitmaps
import tech.ryadom.origami.util.extensions.NO_DIMENSION_LIMIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Size of [image] once encoded at [quality], which is what the compression loop is measuring.
 */
private fun encodedSize(image: ImageBitmap, quality: Int): Int {
    val data = Image.makeFromBitmap(image.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.JPEG, quality)

    return data?.use { it.size } ?: error("no JPEG encoder available")
}

class SkikoImageCompressorTest {

    private val compressor = SkikoImageCompressor(maxDimension = NO_DIMENSION_LIMIT)

    @Test
    fun compresses_down_to_the_requested_budget() = runTest {
        val source = TestBitmaps.noise(size = 96)
        val budget = encodedSize(source, quality = 90) / 3

        val compressed = compressor.compress(
            image = source,
            compression = OrigamiCompression(
                maxSize = budget.toLong(),
                startQuality = 90,
                qualityDowngradeStep = 10
            )
        )

        assertTrue(
            encodedSize(compressed, quality = 90) < encodedSize(source, quality = 90),
            "compression did not shrink the image"
        )
        assertEquals(source.width, compressed.width)
        assertEquals(source.height, compressed.height)
    }

    @Test
    fun returns_a_decodable_image() = runTest {
        val compressed = compressor.compress(
            image = TestBitmaps.noise(size = 64),
            compression = OrigamiCompression(maxSize = 1_024, startQuality = 80)
        )

        assertEquals(64, compressed.width)
        assertEquals(64, compressed.height)
    }

    @Test
    fun a_single_pass_is_enough_when_the_size_is_unconstrained() = runTest {
        val compressed = compressor.compress(
            image = TestBitmaps.noise(size = 32),
            compression = OrigamiCompression()
        )

        assertEquals(32, compressed.width)
    }

    @Test
    fun scales_a_source_above_the_platform_limit() {
        val scaled = SkikoImageCompressor(maxDimension = 128)
            .scaleToPlatformLimits(TestBitmaps.quadrants(width = 512, height = 256))

        assertEquals(128, scaled.width)
        assertEquals(64, scaled.height)
    }

    @Test
    fun leaves_a_source_within_the_platform_limit_alone() {
        val source = TestBitmaps.quadrants(width = 64, height = 64)

        assertSame(source, compressor.scaleToPlatformLimits(source))
        assertSame(source, SkikoImageCompressor(maxDimension = 64).scaleToPlatformLimits(source))
    }
}
