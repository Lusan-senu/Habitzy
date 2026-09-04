import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * Phase 4 — curated subset of the M3 Expressive shape library
 * (m3.material.io/styles/shape/overview-principles). Screens pull from these
 * named tokens instead of reaching into MaterialShapes.* ad hoc, so Habitzy's
 * decorative-shape vocabulary stays small and intentional.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object HabitzyDecorativeShapes {
    val CirclePolygon: RoundedPolygon = MaterialShapes.Circle
    val SquarePolygon: RoundedPolygon = MaterialShapes.Square
    val SunnyPolygon: RoundedPolygon = MaterialShapes.Sunny
    val Cookie9SidedPolygon: RoundedPolygon = MaterialShapes.Cookie9Sided
    val Clover8LeafPolygon: RoundedPolygon = MaterialShapes.Clover8Leaf
    val GemPolygon: RoundedPolygon = MaterialShapes.Gem
    val SoftBurstPolygon: RoundedPolygon = MaterialShapes.SoftBurst

    /** Rotating set used by the icon/emoji picker so each slot has a shape identity. */
    val PickerAccentPolygons: List<RoundedPolygon> = listOf(
        SunnyPolygon, Cookie9SidedPolygon, GemPolygon, Clover8LeafPolygon, SoftBurstPolygon,
    )

    /** Bigger streaks get a "bigger" shape — the shape itself communicates achievement. */
    fun forStreakMilestone(streak: Int): RoundedPolygon? = when {
        streak >= 90 -> SoftBurstPolygon
        streak >= 30 -> Cookie9SidedPolygon
        streak >= 7 -> SunnyPolygon
        else -> null
    }
}

val HabitzyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Expressive-only extra tokens not part of the base Shapes class
val ShapeLargeIncreased = RoundedCornerShape(20.dp)
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)
val ShapeFull = CircleShape
