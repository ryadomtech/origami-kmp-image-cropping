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

/**
 * Aspect ratio for [OrigamiCropArea]
 *
 * @param isVariable pass true if you want the user to be able to change the ratio using gestures
 * @param aspectRatio fixed width / height ratio when [isVariable] is false, otherwise the ratio
 * the crop area starts at
 */
public data class OrigamiAspectRatio(
    val isVariable: Boolean = false,
    val aspectRatio: Float = 1f
) {

    init {
        require(aspectRatio.isFinite() && aspectRatio > 0f) {
            "aspectRatio must be a finite positive number, was $aspectRatio"
        }
    }

    public companion object {

        /**
         * Crop area the user can reshape freely, starting out covering the whole image.
         */
        public val Free: OrigamiAspectRatio = OrigamiAspectRatio(isVariable = true)

        /**
         * 1:1
         */
        public val Square: OrigamiAspectRatio = of(width = 1, height = 1)

        /**
         * 4:3
         */
        public val Landscape4x3: OrigamiAspectRatio = of(width = 4, height = 3)

        /**
         * 3:4
         */
        public val Portrait3x4: OrigamiAspectRatio = of(width = 3, height = 4)

        /**
         * 16:9
         */
        public val Landscape16x9: OrigamiAspectRatio = of(width = 16, height = 9)

        /**
         * 9:16
         */
        public val Portrait9x16: OrigamiAspectRatio = of(width = 9, height = 16)

        /**
         * Fixed ratio of [width] to [height], e.g. `of(3, 2)` for a 3:2 crop.
         */
        public fun of(width: Int, height: Int): OrigamiAspectRatio {
            require(width > 0 && height > 0) {
                "width and height must be positive, were $width x $height"
            }

            return OrigamiAspectRatio(
                isVariable = false,
                aspectRatio = width.toFloat() / height
            )
        }
    }
}
