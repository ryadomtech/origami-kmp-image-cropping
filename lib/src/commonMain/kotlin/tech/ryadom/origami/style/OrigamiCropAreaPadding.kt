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

import androidx.compose.ui.util.fastCoerceIn

/**
 * Padding between [OrigamiCropArea] and source
 */
public fun interface OrigamiCropAreaPadding {

    /**
     * Returns the crop area side to use.
     *
     * @param s max available side size
     * @return the side the crop area should start at, in the same units as [s]
     */
    public fun getFactor(s: Float): Float

    public companion object {
        public fun createDefault(): OrigamiCropAreaPadding = FactorPadding(0.15f)
    }
}

/**
 * Leaves [factor] of the available side free, e.g. `FactorPadding(0.15f)` starts the crop area
 * at 85% of the image.
 */
public class FactorPadding(
    private val factor: Float
) : OrigamiCropAreaPadding {

    init {
        require(factor.isFinite() && factor in 0f..1f) {
            "factor must be in 0f..1f, was $factor"
        }
    }

    override fun getFactor(s: Float): Float {
        return s * (1 - factor)
    }
}

/**
 * Leaves [width] pixels of the available side free.
 */
public class FixedPadding(
    private val width: Float
) : OrigamiCropAreaPadding {

    init {
        require(width.isFinite() && width >= 0f) {
            "width must be a finite non negative number, was $width"
        }
    }

    override fun getFactor(s: Float): Float {
        return (s - width).fastCoerceIn(0f, s)
    }
}
