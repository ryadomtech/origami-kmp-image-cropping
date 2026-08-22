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
 * Origami compression options
 *
 * @property maxSize max size of the encoded image in bytes. Unlimited (original size) by default.
 * @property startQuality the quality, in `1..100`, from which we will start trying to compress
 * the image until its size is not greater than [maxSize]
 * @property qualityDowngradeStep we will downgrade [startQuality] by this step on every
 * iteration. So, the number of compress iterations is at most
 * [startQuality] / [qualityDowngradeStep].
 */
public data class OrigamiCompression(
    val maxSize: Long = UNLIMITED,
    val startQuality: Int = 90,
    val qualityDowngradeStep: Int = 10
) {

    init {
        require(maxSize > 0L) {
            "maxSize must be positive, was $maxSize"
        }

        require(startQuality in MIN_QUALITY..MAX_QUALITY) {
            "startQuality must be in $MIN_QUALITY..$MAX_QUALITY, was $startQuality"
        }

        require(qualityDowngradeStep in 1..MAX_QUALITY) {
            "qualityDowngradeStep must be in 1..$MAX_QUALITY, was $qualityDowngradeStep"
        }
    }

    public companion object {

        /**
         * Do not constrain the encoded size.
         */
        public const val UNLIMITED: Long = Long.MAX_VALUE

        internal const val MIN_QUALITY: Int = 1

        internal const val MAX_QUALITY: Int = 100
    }
}
