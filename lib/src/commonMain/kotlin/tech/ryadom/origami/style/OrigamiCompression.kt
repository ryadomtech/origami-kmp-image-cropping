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

import kotlinx.serialization.Serializable

/**
 * Origami compression options
 * @property maxSize max size of image in bytes. [Unlimited] (original size) by default.
 * @property startQuality the quality from which we will start trying to compress
 * the image until its size is greater than [maxSize]
 * @property qualityDowngradeStep we will downgrade [startQuality] by this step on every iteration.
 * So, number of compress iterations ≈ [startQuality] / [qualityDowngradeStep]
 */
@Serializable
data class OrigamiCompression(
    val maxSize: Long = Unlimited,
    val startQuality: Int = 90,
    val qualityDowngradeStep: Int = 10
)

private const val Unlimited = Long.MAX_VALUE