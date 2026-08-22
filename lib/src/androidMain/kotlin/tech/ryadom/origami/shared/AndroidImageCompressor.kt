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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import tech.ryadom.origami.style.OrigamiCompression
import java.io.ByteArrayOutputStream
import kotlin.math.min

private const val MaxBitmapSize = 2048

/**
 * Below this the artefacts cost more than the bytes saved.
 */
private const val MinQuality = 10

private class AndroidImageCompressor : ImageCompressor {

    /**
     * Scales an [ImageBitmap] down to the maximum supported texture size to prevent
     * potential OutOfMemoryError (OOM) exceptions when handling large images.
     *
     * If the image dimensions are already within the platform limits, the original
     * [ImageBitmap] is returned without modification. Otherwise, it's scaled down
     * while maintaining its aspect ratio.
     *
     * @param image The [ImageBitmap] to scale.
     * @return A new, scaled [ImageBitmap], or the original if it's already within limits.
     */
    override fun scaleToPlatformLimits(image: ImageBitmap): ImageBitmap {
        if (image.width <= MaxBitmapSize && image.height <= MaxBitmapSize) {
            return image
        }

        val bitmap = image.asAndroidBitmap()
        val scale = min(
            a = MaxBitmapSize.toFloat() / bitmap.width,
            b = MaxBitmapSize.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        @SuppressLint("UseKtx")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            .asImageBitmap()
    }

    override suspend fun compress(
        image: ImageBitmap,
        compression: OrigamiCompression
    ): ImageBitmap {
        return withContext(Dispatchers.IO) {
            val bitmap = scaleToPlatformLimits(image)
                .asAndroidBitmap()

            var quality = compression.startQuality
            var compressed: ByteArray

            while (true) {
                ensureActive()

                compressed = ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    outputStream.toByteArray()
                }

                if (compressed.size <= compression.maxSize || quality <= MinQuality) break
                quality = (quality - compression.qualityDowngradeStep)
                    .coerceAtLeast(MinQuality)
            }

            compressed.decodeToImageBitmap()
        }
    }
}

public actual fun createImageCompressor(): ImageCompressor {
    return AndroidImageCompressor()
}