/*
 * This file is part of Tuner.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.tuner.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.bobek.tuner.R
import com.bobek.tuner.data.PreferenceChoice
import com.bobek.tuner.settings.PreferenceConstants

/** Colors of one Omarchy theme, as named in its colors.toml. */
@Immutable
data class OmarchyPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val accent: Color,
    val selection: Color,
    val muted: Color,
    val background: Color,
    val darkBackground: Color,
    val lighterBackground: Color,
    val foreground: Color,
    val darkForeground: Color,
    val lightForeground: Color,
    val red: Color,
    val yellow: Color,
    val green: Color,
    val cyan: Color,
    val blue: Color,
    val magenta: Color
)

fun OmarchyPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = background,
        primaryContainer = selection,
        onPrimaryContainer = foreground,
        inversePrimary = accent,
        secondary = cyan,
        onSecondary = background,
        secondaryContainer = selection,
        onSecondaryContainer = foreground,
        tertiary = yellow,
        onTertiary = background,
        tertiaryContainer = selection,
        onTertiaryContainer = foreground,
        error = red,
        onError = background,
        errorContainer = selection,
        onErrorContainer = red,
        background = background,
        onBackground = foreground,
        surface = background,
        onSurface = foreground,
        surfaceVariant = lighterBackground,
        onSurfaceVariant = lightForeground,
        surfaceTint = accent,
        inverseSurface = foreground,
        inverseOnSurface = background,
        outline = muted,
        outlineVariant = selection,
        scrim = Color.Black,
        surfaceBright = lighterBackground,
        surfaceDim = darkBackground,
        surfaceContainerLowest = darkBackground,
        surfaceContainerLow = background,
        surfaceContainer = background,
        surfaceContainerHigh = lighterBackground,
        surfaceContainerHighest = lighterBackground
    )
}

/** Colors the tuner uses to say how close the note is, independent of the Material roles. */
@Immutable
data class TunerColors(
    val inTune: Color,
    val nearlyInTune: Color,
    val outOfTune: Color,
    val inTuneBorder: Color
)

private val DefaultInTuneBorder = Color(0xFF4CAF50)

fun systemTunerColors(colorScheme: ColorScheme) = TunerColors(
    inTune = colorScheme.primary,
    nearlyInTune = colorScheme.tertiary,
    outOfTune = colorScheme.error,
    inTuneBorder = DefaultInTuneBorder
)

fun OmarchyPalette.tunerColors() = TunerColors(
    inTune = green,
    nearlyInTune = yellow,
    outOfTune = red,
    inTuneBorder = green
)

val LocalTunerColors = staticCompositionLocalOf {
    TunerColors(primaryLight, tertiaryLight, errorLight, DefaultInTuneBorder)
}

object TunerTheme {
    val colors: TunerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTunerColors.current
}

/** An entry of the color scheme setting: the system (Material) colors or one Omarchy palette. */
class ColorSchemeChoice(
    override val preferenceValue: String,
    @StringRes override val labelResourceId: Int,
    val palette: OmarchyPalette?
) : PreferenceChoice {

    @Composable
    fun label(): String = palette?.displayName ?: stringResource(labelResourceId)

    companion object {
        val SYSTEM = ColorSchemeChoice(
            PreferenceConstants.COLOR_SCHEME_VALUE_SYSTEM,
            R.string.color_scheme_system,
            null
        )

        val entries: List<ColorSchemeChoice> =
            listOf(SYSTEM) + OMARCHY_PALETTES.map { ColorSchemeChoice(it.id, R.string.color_scheme_system, it) }

        fun forPreferenceValue(preferenceValue: String): ColorSchemeChoice =
            entries.find { it.preferenceValue == preferenceValue } ?: SYSTEM
    }
}
