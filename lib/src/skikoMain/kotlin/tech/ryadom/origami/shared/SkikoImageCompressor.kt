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
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import tech.ryadom.origami.style.OrigamiCompression
import tech.ryadom.origami.util.extensions.scaledToFit
import kotlin.coroutines.coroutineContext

/**
 * [ImageCompressor] shared by every skiko backed target: desktop, iOS, JS and Wasm.
 *
 * @param maxDimension largest side [scaleToPlatformLimits] will allow through
 */
internal class SkikoImageCompressor(
    private val maxDimension: Int
) : ImageCompressor {

    override fun scaleToPlatformLimits(image: ImageBitmap): ImageBitmap {
        return image.scaledToFit(maxDimension)
    }

    override suspend fun compress(
        image: ImageBitmap,
        compression: OrigamiCompression
    ): ImageBitmap = withContext(Dispatchers.Default) {
        val skiaImage = Image.makeFromBitmap(
            scaleToPlatformLimits(image).asSkiaBitmap()
        )

        try {
            var quality = compression.startQuality
            var encoded: ByteArray? = null

            while (true) {
                coroutineContext.ensureActive()

                // Some targets ship Skia without a JPEG encoder
                val data = skiaImage.encodeToData(EncodedImageFormat.JPEG, quality) ?: break

                encoded = try {
                    data.bytes
                } finally {
                    data.close()
                }

                if (encoded.size <= compression.maxSize || quality <= MIN_QUALITY) break
                quality = (quality - compression.qualityDowngradeStep)
                    .coerceAtLeast(MIN_QUALITY)
            }

            encoded?.decodeToImageBitmap() ?: image
        } finally {
            skiaImage.close()
        }
    }

    private companion object {
        /**
         * Below this the artefacts cost more than the bytes saved.
         */
        const val MIN_QUALITY = 10
    }
}
