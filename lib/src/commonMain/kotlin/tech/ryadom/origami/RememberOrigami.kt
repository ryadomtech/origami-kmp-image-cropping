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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import tech.ryadom.origami.shared.ImageCompressor
import tech.ryadom.origami.shared.createImageCompressor
import tech.ryadom.origami.style.OrigamiAspectRatio
import tech.ryadom.origami.style.OrigamiColors
import tech.ryadom.origami.style.OrigamiCropArea

/**
 * Remembers an [Origami] for [imageBitmap] and keeps the user's selection across configuration
 * changes and process death.
 *
 * The bitmap itself is not saved — reload it the same way you did originally and pass it back
 * in. The crop area is stored in resolution independent coordinates, so it lands in the right
 * place even if the image comes back at a different size or the window changed shape.
 *
 * @param imageBitmap image to crop
 * @param colors [OrigamiColors]
 * @param cropArea [OrigamiCropArea]
 * @param aspectRatio [OrigamiAspectRatio]
 * @param compressor [ImageCompressor]
 */
@Composable
public fun rememberOrigami(
    imageBitmap: ImageBitmap,
    colors: OrigamiColors = OrigamiColors.createDefault(),
    cropArea: OrigamiCropArea = OrigamiCropArea(),
    aspectRatio: OrigamiAspectRatio = OrigamiAspectRatio(),
    compressor: ImageCompressor = createImageCompressor()
): Origami {
    return rememberSaveable(
        imageBitmap,
        colors,
        cropArea,
        aspectRatio,
        saver = origamiSaver(imageBitmap, colors, cropArea, aspectRatio, compressor)
    ) {
        Origami(
            imageBitmap = imageBitmap,
            colors = colors,
            cropArea = cropArea,
            aspectRatio = aspectRatio,
            compressor = compressor
        )
    }
}

private const val FREE_RATIO = Float.NaN

/**
 * [Saver] for the normalized selection, the rotation and the current ratio lock.
 */
private fun origamiSaver(
    imageBitmap: ImageBitmap,
    colors: OrigamiColors,
    cropArea: OrigamiCropArea,
    aspectRatio: OrigamiAspectRatio,
    compressor: ImageCompressor
): Saver<Origami, Any> = listSaver(
    save = { origami ->
        val selection = origami.normalizedSelection() ?: Rect.Zero

        listOf(
            selection.left,
            selection.top,
            selection.right,
            selection.bottom,
            origami.rotationDegrees,
            origami.aspectRatio ?: FREE_RATIO
        )
    },
    restore = { values ->
        val selection = Rect(
            left = values[0] as Float,
            top = values[1] as Float,
            right = values[2] as Float,
            bottom = values[3] as Float
        )

        val ratio = values[5] as Float

        Origami(
            imageBitmap = imageBitmap,
            colors = colors,
            cropArea = cropArea,
            aspectRatio = aspectRatio,
            compressor = compressor
        ).apply {
            restoreState(
                selection = selection.takeIf { it.width > 0f && it.height > 0f },
                rotation = values[4] as Int,
                ratio = ratio.takeIf { !it.isNaN() }
            )
        }
    }
)
