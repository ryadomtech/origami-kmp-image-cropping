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

import androidx.compose.ui.graphics.Color

/**
 * Origami colors
 *
 * @property backgroundColor background [Color]
 * @property guidelinesColor guidelines [Color]
 * @property edgesColor a [Color] of guideline's edges
 */
public interface OrigamiColors {
    public val backgroundColor: Color

    public val guidelinesColor: Color

    public val edgesColor: Color

    public companion object {
        /**
         * Default values for [OrigamiColors], with any of them overridable:
         * `OrigamiColors.createDefault(guidelinesColor = Color.White)`
         */
        public fun createDefault(
            backgroundColor: Color = Color.Black.copy(alpha = 0.7F),
            guidelinesColor: Color = Color.Gray,
            edgesColor: Color = guidelinesColor
        ): OrigamiColors = DefaultOrigamiColors(
            backgroundColor = backgroundColor,
            guidelinesColor = guidelinesColor,
            edgesColor = edgesColor
        )
    }
}

/**
 * Value based [OrigamiColors], so two identically configured instances compare equal.
 */
private data class DefaultOrigamiColors(
    override val backgroundColor: Color,
    override val guidelinesColor: Color,
    override val edgesColor: Color
) : OrigamiColors
