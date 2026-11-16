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

package com.bobek.tuner.ui.tunings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bobek.tuner.R
import com.bobek.tuner.domain.Instrument
import com.bobek.tuner.domain.InstrumentString
import com.bobek.tuner.domain.Tuning
import com.bobek.tuner.domain.TuningVariant
import com.bobek.tuner.domain.noteDisplayName

@Composable
fun Instrument.label(): String = stringResource(
    when (this) {
        Instrument.GUITAR -> R.string.instrument_guitar
        Instrument.BANJO -> R.string.instrument_banjo
        Instrument.UKULELE -> R.string.instrument_ukulele
        Instrument.VIOLIN -> R.string.instrument_violin
        Instrument.VIOLA -> R.string.instrument_viola
        Instrument.CELLO -> R.string.instrument_cello
        Instrument.DOUBLE_BASS -> R.string.instrument_double_bass
        Instrument.ELECTRIC_BASS -> R.string.instrument_electric_bass
        Instrument.VIOLA_CAIPIRA -> R.string.instrument_viola_caipira
        Instrument.CAVAQUINHO -> R.string.instrument_cavaquinho
        Instrument.MANDOLIN -> R.string.instrument_mandolin
    }
)

@Composable
fun Tuning.variantLabel(): String = when (variant) {
    TuningVariant.STANDARD -> stringResource(R.string.tuning_standard)
    TuningVariant.DROP_D -> stringResource(R.string.tuning_drop_d)
    TuningVariant.HALF_STEP_DOWN -> stringResource(R.string.tuning_half_step_down)
    TuningVariant.WHOLE_STEP_DOWN -> stringResource(R.string.tuning_whole_step_down)
    TuningVariant.DROP_C -> stringResource(R.string.tuning_drop_c)
    TuningVariant.OPEN_G -> stringResource(R.string.tuning_open_g)
    TuningVariant.OPEN_D -> stringResource(R.string.tuning_open_d)
    TuningVariant.OPEN_E -> stringResource(R.string.tuning_open_e)
    TuningVariant.DADGAD -> stringResource(R.string.tuning_dadgad)
    TuningVariant.SEVEN_STRINGS -> stringResource(R.string.tuning_n_strings, 7)
    TuningVariant.LOW_G -> stringResource(R.string.tuning_low_g)
    TuningVariant.D_TUNING -> stringResource(R.string.tuning_d_tuning)
    TuningVariant.BARITONE -> stringResource(R.string.tuning_baritone)
    TuningVariant.BASS -> stringResource(R.string.tuning_bass)
    TuningVariant.DOUBLE_C -> stringResource(R.string.tuning_double_c)
    TuningVariant.TENOR -> stringResource(R.string.tuning_tenor)
    TuningVariant.PLECTRUM -> stringResource(R.string.tuning_plectrum)
    TuningVariant.SOLO -> stringResource(R.string.tuning_solo)
    TuningVariant.FOUR_STRINGS_DROP_D -> combined(4, stringResource(R.string.tuning_drop_d))
    TuningVariant.FOUR_STRINGS_HALF_STEP_DOWN -> combined(4, stringResource(R.string.tuning_half_step_down))
    TuningVariant.FIVE_STRINGS -> stringResource(R.string.tuning_n_strings, 5)
    TuningVariant.FIVE_STRINGS_HIGH_C -> combined(5, stringResource(R.string.tuning_high_c))
    TuningVariant.SIX_STRINGS -> stringResource(R.string.tuning_n_strings, 6)
    TuningVariant.NATURAL -> stringResource(R.string.tuning_natural)
    TuningVariant.CEBOLAO_E -> stringResource(R.string.tuning_cebolao_e)
    TuningVariant.CEBOLAO_D -> stringResource(R.string.tuning_cebolao_d)
    TuningVariant.RIO_ABAIXO -> stringResource(R.string.tuning_rio_abaixo)
}

@Composable
private fun combined(strings: Int, label: String): String =
    stringResource(R.string.tuning_combined, stringResource(R.string.tuning_n_strings, strings), label)

/** "Guitar / Acoustic guitar · Drop D" */
@Composable
fun Tuning.fullLabel(): String = stringResource(R.string.tuning_combined, instrument.label(), variantLabel())

/** "E2 A2 D3 G3 B3 E4"; octave courses as "A2/A3". */
fun Tuning.notesLabel(solfegeNotation: Boolean): String =
    strings.joinToString("  ") { it.label(solfegeNotation) }

fun InstrumentString.label(solfegeNotation: Boolean): String =
    pitches.joinToString("/") { "${noteDisplayName(it.name, solfegeNotation)}${it.octave}" }
