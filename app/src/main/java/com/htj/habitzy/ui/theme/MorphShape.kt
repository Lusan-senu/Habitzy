package com.htj.habitzy.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Renders a [Morph] between two [RoundedPolygon]s at a given [progress]
 * (0f = start shape, 1f = end shape), scaled to fill the composable.
 *
 * RoundedPolygon/Morph coordinates are normalized to a [-1, 1] box, so we
 * scale by half the container size and re-center — the standard mapping
 * for MaterialShapes-derived polygons.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(morphPathForSize(morph, progress, size))
}

internal fun morphPathForSize(morph: Morph, progress: Float, size: Size): Path {
    val androidPath = android.graphics.Path()
    morph.toPath(progress = progress, path = androidPath)
    val matrix = android.graphics.Matrix().apply {
        setScale(size.width / 2f, size.height / 2f)
        postTranslate(size.width / 2f, size.height / 2f)
    }
    androidPath.transform(matrix)
    return androidPath.asComposePath()
}

/** A static (non-animating) Compose [Shape] for a single [RoundedPolygon]. */
fun RoundedPolygon.asComposeShape(): Shape =
    MorphPolygonShape(Morph(this, this), progress = 0f)