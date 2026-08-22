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
import tech.ryadom.origami.style.OrigamiCompression

/**
 * Platform hook for keeping bitmaps within platform limits and re-encoding a cropped result
 * down to a size budget.
 */
public interface ImageCompressor {

    /**
     * Scales [image] down to the largest size the platform can hold safely, or returns it
     * untouched when it already fits.
     */
    public fun scaleToPlatformLimits(image: ImageBitmap): ImageBitmap

    /**
     * Re-encodes [image] until it fits [OrigamiCompression.maxSize], lowering the quality one
     * [OrigamiCompression.qualityDowngradeStep] at a time.
     *
     * Runs off the main thread and honours cancellation. The round trip is lossy and drops the
     * alpha channel.
     */
    public suspend fun compress(image: ImageBitmap, compression: OrigamiCompression): ImageBitmap

    /**
     * Pass-through implementation.
     */
    public class Original : ImageCompressor {
        override fun scaleToPlatformLimits(image: ImageBitmap): ImageBitmap {
            return image
        }

        override suspend fun compress(
            image: ImageBitmap,
            compression: OrigamiCompression
        ): ImageBitmap {
            return image
        }
    }
}

public expect fun createImageCompressor(): ImageCompressor