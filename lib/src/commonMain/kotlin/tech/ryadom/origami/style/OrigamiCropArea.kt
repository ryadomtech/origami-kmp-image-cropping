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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

/**
 * Origami crop area.
 *
 * The area whose contents will be returned when [tech.ryadom.origami.Origami.crop] is called.
 *
 * @param highlightedShape [OrigamiHighlightedShape] that will fit into the crop area and highlight
 * the content inside it. Note that shape is for preview only and does not affect the crop area.
 *
 * @param edges [OrigamiEdges] form of crop area edges
 *
 * @param guidelinesWidth width of the guidelines. Please, specify this to 0.dp if you want
 * guidelines to be invisible
 *
 * @param guidelinesCount count of the vertical and horizontal guidelines. You can also pass 0
 * to hide guidelines
 *
 * @param initialPaddings [OrigamiCropAreaPadding]
 */
@Serializable
data class OrigamiCropArea(
    val highlightedShape: OrigamiHighlightedShape = OrigamiHighlightedShape.Default,
    val edges: OrigamiEdges? = OrigamiEdges.Circle(6.dp),
    val guidelinesWidth: Dp = 2.dp,
    val guidelinesCount: Int = 2,
    val initialPaddings: OrigamiCropAreaPadding = OrigamiCropAreaPadding.createDefault()
)