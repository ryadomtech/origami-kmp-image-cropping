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
 * Aspect ratio for [OrigamiCropArea]
 * @param isVariable pass true if you want the user to be able to change the ratio using gestures
 * @param aspectRatio fixed aspect ratio if [isVariable] is false, else initial aspect ratio
 */
@Serializable
data class OrigamiAspectRatio(
    val isVariable: Boolean = false,
    val aspectRatio: Float = 1f
)