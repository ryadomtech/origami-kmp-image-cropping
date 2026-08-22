![image](origami_logo.png)
# Origami — simple image cropping composable for Kotlin Multiplatform

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/tech.ryadom/origami?color=blue)](https://central.sonatype.com/artifact/tech.ryadom/origami)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](https://img.shields.io/badge/platform-desktop-3474eb.svg?style=flat)
![badge-js](https://img.shields.io/badge/platform-js-fcba03.svg?style=flat)
![badge-wasm](https://img.shields.io/badge/platform-wasm-331f06.svg?style=flat)

With this tool you can crop images in Compose Multiplatform.

## Supported targets

| Target          | Implemented | Tested |
|-----------------|-------------|--------|
| **Android**     | ☑           | ☑      |
| **iOS**         | ☑           | ☑      |
| **JVM Desktop** | ☑           | ☑      |
| **JS**          | ☑           | ☑      |
| **WasmJS**      | ☑           | ☑      |

### Implementation

In your shared module's build.gradle.kts add:

```Gradle Kotlin DSL
kotlin.sourceSets.commonMain.dependencies {
  implementation("tech.ryadom:origami:2.0.0")
}
```

### Usage

Create the state holder with `rememberOrigami`, hand it to `OrigamiImage`, and call `crop()`
when you want the result.

```Kotlin
val origami = rememberOrigami(imageBitmap = myBitmap)

OrigamiImage(origami = origami)

// Returns cropped image
origami.crop()
```

`rememberOrigami` keeps the selection across recompositions **and** across configuration changes
and process death — the crop area is stored in resolution independent coordinates, so it comes
back in the right place even if the window changed shape. Pass the bitmap back in yourself; it is
not saved.

You can also build one by hand, which is what you want for a custom `OrigamiSource`:

```Kotlin
val source = createYourSource()
val colors = OrigamiColors.createDefault(guidelinesColor = Color.White)
val cropArea = OrigamiCropArea()
val aspectRatio = OrigamiAspectRatio()

val origami = remember { Origami(source, colors, cropArea, aspectRatio) }
```

> Always keep the instance in `remember`. Constructing an `Origami` inside a composable body
> throws away the user's selection on every recomposition and re-runs the source scaling.

#### Editing

```Kotlin
origami.rotateClockwise()        // quarter turns, lossless
origami.rotateCounterClockwise()
origami.flipHorizontally()
origami.flipVertically()

origami.setAspectRatio(OrigamiAspectRatio.Landscape16x9)  // or a preset
origami.aspectRatio = 3f / 2f                             // or a raw ratio
origami.aspectRatio = null                                // free form

origami.reset()                  // back to the initial crop area, no transforms

origami.cropRect                 // observable selection, follows the user's drag
origami.rotationDegrees          // 0, 90, 180 or 270
```

#### Cropping with a size budget

`crop(OrigamiCompression)` runs off the main thread, honours cancellation, and re-encodes the
result until it fits:

```Kotlin
scope.launch {
    val result = origami.crop(
        OrigamiCompression(maxSize = 500 * 1024)
    )
}
```

Compression is a lossy JPEG round trip, so it drops the alpha channel.

### Customization

#### 1. Colors

With `OrigamiColors` you can customize `backgroundColor`, `guidelinesColor` and `edgesColor`:

```Kotlin
interface OrigamiColors {
    val backgroundColor: Color

    val guidelinesColor: Color

    val edgesColor: Color
}
```

#### 2. Crop area

With `OrigamiCropArea` you can customize the crop area:

2.1. Number and width of guidelines

2.2. Shape edges via `OrigamiEdges`. You can use the default `Circle` or `Rectangle` shape, or
create your own using `DrawScope`

2.3. The highlighted area of the shape via `OrigamiHighlightedShape`. You can use `Circle`,
`Rectangle` (default) or `RoundedRectangle` by default or create your own shape

2.4. Crop area initial paddings via `OrigamiCropAreaPadding`

```Kotlin
data class OrigamiCropArea(
    val highlightedShape: OrigamiHighlightedShape = OrigamiHighlightedShape.Default,
    val edges: OrigamiEdges? = OrigamiEdges.Circle(6.dp),
    val guidelinesWidth: Dp = 2.dp,
    val guidelinesCount: Int = 2,
    val initialPaddings: OrigamiCropAreaPadding = OrigamiCropAreaPadding.createDefault()
)
```

2.5. Minimum crop size and how large the corner handles are to the touch, via `minSize`
and `handleTouchTarget`. Both are `Dp`, so they hold up across densities.

```Kotlin
data class OrigamiCropArea(
    val highlightedShape: OrigamiHighlightedShape = OrigamiHighlightedShape.Default,
    val edges: OrigamiEdges? = OrigamiEdges.Circle(6.dp),
    val guidelinesWidth: Dp = 2.dp,
    val guidelinesCount: Int = 2,
    val initialPaddings: OrigamiCropAreaPadding = OrigamiCropAreaPadding.createDefault(),
    val minSize: Dp = 56.dp,
    val handleTouchTarget: Dp = 32.dp
)
```

#### 3. Aspect ratio

With `OrigamiAspectRatio` you can specify any width / height ratio of crop area you need.

```Kotlin
data class OrigamiAspectRatio(
    val isVariable: Boolean = false,
    val aspectRatio: Float = 1f
)
```

Presets cover the usual cases:

```Kotlin
OrigamiAspectRatio.Free            // user reshapes freely, starts covering the whole image
OrigamiAspectRatio.Square          // 1:1
OrigamiAspectRatio.Landscape4x3
OrigamiAspectRatio.Portrait3x4
OrigamiAspectRatio.Landscape16x9
OrigamiAspectRatio.Portrait9x16
OrigamiAspectRatio.of(width = 3, height = 2)
```

### License

```
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
```

### Support

If you find a bug or want to contribute an improvement, please create an Issue or email
opensource@ryadom.tech.
Any support will be appreciated.
