package tech.ryadom.origami.sample.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import origami.sample.common.generated.resources.Res
import origami.sample.common.generated.resources.sample
import tech.ryadom.origami.Origami
import tech.ryadom.origami.OrigamiImage
import tech.ryadom.origami.style.OrigamiAspectRatio
import tech.ryadom.origami.style.OrigamiCropArea
import tech.ryadom.origami.style.OrigamiHighlightedShape

@Composable
fun SampleApp() {
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    if (croppedImage != null) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "The result is below!\nClick on image to reset",
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.size(250.dp)
                    .padding(top = 24.dp)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray
                    )
                    .clickable {
                        croppedImage = null
                    }
            ) {
                Image(
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color.Yellow),
                    bitmap = croppedImage!!,
                    contentDescription = null
                )
            }
        }

        return
    }

    val painter = painterResource(Res.drawable.sample)
    val scope = rememberCoroutineScope()

    val origami = Origami(
        imageBitmap = painter.toImageBitmap(
            density = LocalDensity.current,
            layoutDirection = LocalLayoutDirection.current
        ),
        aspectRatio = OrigamiAspectRatio(false),
        cropArea = OrigamiCropArea(
            highlightedShape = OrigamiHighlightedShape.Circle
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .windowInsetsPadding(
                WindowInsets.systemBars
            ),
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth()
                    .padding(
                        horizontal = 16.dp
                    ),
                onClick = {
                    scope.launch { croppedImage = origami.crop() }
                },
                colors = ButtonDefaults.buttonColors()
                    .copy(
                        containerColor = Color.Blue
                    ),
                shape = RoundedCornerShape(size = 8.dp)
            ) {
                Text(text = "Crop")
            }
        }
    ) {
        OrigamiImage(
            origami = origami,
            modifier = Modifier.fillMaxSize()
                .padding(it)
        )
    }
}

// Convert painter to image bitmap
private fun Painter.toImageBitmap(
    density: Density,
    layoutDirection: LayoutDirection
): ImageBitmap {
    val bitmap = ImageBitmap(intrinsicSize.width.toInt(), intrinsicSize.height.toInt())
    val canvas = Canvas(bitmap)

    CanvasDrawScope().draw(density, layoutDirection, canvas, intrinsicSize) {
        draw(intrinsicSize)
    }

    return bitmap
}