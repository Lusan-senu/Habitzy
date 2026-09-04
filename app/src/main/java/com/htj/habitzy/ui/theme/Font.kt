package com.htj.habitzy.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontLoader
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.htj.habitzy.R

/**
 * Roboto Flex via Android's Downloadable Fonts (served through Google Play
 * Services on-device — no font bytes bundled in the APK). Phase 3 of the
 * M3 Expressive migration: replaces FontFamily.Default across Type.kt.
 *
 * Requires `implementation("androidx.compose.ui:ui-text-google-fonts")`
 * (added in build.gradle.kts) and res/values/font_certs.xml (see below).
 */
private val robotoFlexProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val robotoFlexGoogleFont = GoogleFont("Roboto Flex")

/** Standard weights used across the type scale's non-emphasized styles. */
val RobotoFlexFontFamily = FontFamily(
    GoogleFontLoader(googleFont = robotoFlexGoogleFont, fontProvider = robotoFlexProvider, weight = FontWeight.Normal),
    GoogleFontLoader(googleFont = robotoFlexGoogleFont, fontProvider = robotoFlexProvider, weight = FontWeight.Medium),
    GoogleFontLoader(googleFont = robotoFlexGoogleFont, fontProvider = robotoFlexProvider, weight = FontWeight.Bold),
)

/**
 * A heavier, slightly wider instance of the same variable font — used for the
 * *Emphasized type-scale slots. This is the "expressive" half of Roboto Flex:
 * a real variation-axis shift, not just swapping to a bolder static weight.
 */
@OptIn(ExperimentalTextApi::class)
val RobotoFlexEmphasizedFontFamily = FontFamily(
    GoogleFontLoader(
        googleFont = robotoFlexGoogleFont,
        fontProvider = robotoFlexProvider,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(110f),
        ),
    ),
)

/**
 * Reusable primitive for one-off "flex heavier" moments the plan calls out
 * (e.g. a streak badge bumping to a new personal best) — pins explicit
 * weight/width axis values rather than picking from the two families above.
 * Not wired into any screen yet; hook this into StreakBadge.kt in Phase 7.
 */
@OptIn(ExperimentalTextApi::class)
fun robotoFlexVariation(weight: Int, width: Int = 100): FontFamily = FontFamily(
    GoogleFontLoader(
        googleFont = robotoFlexGoogleFont,
        fontProvider = robotoFlexProvider,
        weight = FontWeight(weight.coerceIn(1, 1000)),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.width(width.toFloat()),
        ),
    ),
)