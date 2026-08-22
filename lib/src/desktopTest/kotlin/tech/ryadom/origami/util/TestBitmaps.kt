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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import kotlin.test.assertEquals

/**
 * Bitmap helpers for tests that need a real Skia backend.
 */
internal object TestBitmaps {

    val TopLeft: Color = Color.Red
    val TopRight: Color = Color.Green
    val BottomLeft: Color = Color.Blue
    val BottomRight: Color = Color.Yellow

    /**
     * Bitmap split into four solid quadrants, so a transform can be verified by where each
     * color ended up.
     */
    fun quadrants(width: Int = 4, height: Int = 4): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)

        val halfWidth = width / 2
        val halfHeight = height / 2

        canvas.fill(0, 0, halfWidth, halfHeight, TopLeft)
        canvas.fill(halfWidth, 0, width, halfHeight, TopRight)
        canvas.fill(0, halfHeight, halfWidth, height, BottomLeft)
        canvas.fill(halfWidth, halfHeight, width, height, BottomRight)

        return bitmap
    }

    /**
     * Bitmap of noisy pixels, which resists being compressed down to nothing.
     */
    fun noise(size: Int): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        var seed = 42

        repeat(size) { y ->
            repeat(size) { x ->
                seed = seed * 1_103_515_245 + 12_345
                val channel = { shift: Int -> ((seed shr shift) and 0xFF) / 255f }

                canvas.fill(
                    x, y, x + 1, y + 1,
                    Color(red = channel(16), green = channel(8), blue = channel(0), alpha = 1f)
                )
            }
        }

        return bitmap
    }

    private fun Canvas.fill(left: Int, top: Int, right: Int, bottom: Int, color: Color) {
        drawRect(
            left = left.toFloat(),
            top = top.toFloat(),
            right = right.toFloat(),
            bottom = bottom.toFloat(),
            paint = Paint().apply {
                this.color = color
                isAntiAlias = false
            }
        )
    }
}

internal fun ImageBitmap.pixelAt(x: Int, y: Int): Int {
    val buffer = IntArray(1)
    readPixels(buffer, startX = x, startY = y, width = 1, height = 1)
    return buffer[0]
}

internal fun ImageBitmap.assertPixel(x: Int, y: Int, expected: Color, message: String = "") {
    assertEquals(
        expected.toArgb(),
        pixelAt(x, y),
        "$message pixel at ($x, $y)"
    )
}
