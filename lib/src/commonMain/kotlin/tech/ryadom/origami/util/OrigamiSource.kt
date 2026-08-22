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

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import tech.ryadom.origami.Origami

/**
 * [Origami] image source
 */
public interface OrigamiSource {

    /**
     * Composable content for displaying source
     * @param modifier [Modifier]
     */
    @Composable
    public fun Content(modifier: Modifier)

    /**
     * Transform your source to [ImageBitmap].
     *
     * Called lazily. The result is expected to match what [Content] draws, since [Origami.crop]
     * maps the on screen crop area onto this bitmap.
     */
    public fun getImageBitmap(): ImageBitmap
}

/**
 * Bitmap source drawing bitmap
 * @param imageBitmap [ImageBitmap]
 */
internal data class BitmapSource(
    private val imageBitmap: ImageBitmap
) : OrigamiSource {

    @Composable
    override fun Content(modifier: Modifier) {
        Image(
            modifier = modifier,
            bitmap = imageBitmap,
            contentDescription = null
        )
    }

    override fun getImageBitmap(): ImageBitmap {
        return imageBitmap
    }
}