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

package com.bobek.tuner.domain

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val PITCH_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** A note at a fixed octave, e.g. E2. */
data class Pitch(val name: String, val octave: Int) {

    val midi: Int = (octave + 1) * 12 + PITCH_NAMES.indexOf(name)

    fun frequency(referencePitch: Double = DEFAULT_REFERENCE_PITCH.toDouble()): Double =
        referencePitch * 2.0.pow((midi - 69) / 12.0)

    override fun toString() = "$name$octave"

    companion object {
        /** Parses "E2", "F#3", "A#0". */
        fun parse(text: String): Pitch {
            val octaveStart = text.indexOfFirst { it.isDigit() }
            val name = text.substring(0, octaveStart)
            require(name in PITCH_NAMES) { "Unknown note: $text" }
            return Pitch(name, text.substring(octaveStart).toInt())
        }
    }
}

/**
 * One string, or one course of strings played together (viola caipira pairs tuned in octaves), from the lowest-sounding
 * side of the instrument. [pitches] holds every pitch the course may sound; the first is the main one.
 */
data class InstrumentString(val pitches: List<Pitch>) {
    val main: Pitch get() = pitches.first()
}

enum class Instrument {
    GUITAR, BANJO, UKULELE, VIOLIN, VIOLA, CELLO, DOUBLE_BASS, ELECTRIC_BASS, VIOLA_CAIPIRA, CAVAQUINHO, MANDOLIN
}

/** Name of a tuning, mapped to a translated label by the UI. */
enum class TuningVariant {
    STANDARD, DROP_D, HALF_STEP_DOWN, WHOLE_STEP_DOWN, DROP_C, OPEN_G, OPEN_D, OPEN_E, DADGAD, SEVEN_STRINGS,
    LOW_G, D_TUNING, BARITONE, BASS, DOUBLE_C, TENOR, PLECTRUM, SOLO, FOUR_STRINGS_DROP_D, FOUR_STRINGS_HALF_STEP_DOWN,
    FIVE_STRINGS, FIVE_STRINGS_HIGH_C, SIX_STRINGS, NATURAL, CEBOLAO_E, CEBOLAO_D, RIO_ABAIXO
}

data class Tuning(
    val id: String,
    val instrument: Instrument,
    val variant: TuningVariant,
    val strings: List<InstrumentString>
) {
    /**
     * The note to show for [frequency]: the pitch of the string at [lockedString], or else of the string closest to
     * [frequency], with the cents deviation from it (clamped to ±50, like [DetectedNote.fromFrequency]).
     */
    fun noteFor(frequency: Float, referencePitch: Double, lockedString: Int? = null): DetectedNote {
        val candidates = if (lockedString != null && lockedString in strings.indices) {
            strings[lockedString].pitches.map { lockedString to it }
        } else {
            strings.flatMapIndexed { index, string -> string.pitches.map { index to it } }
        }
        val (index, pitch) = candidates.minBy { (_, pitch) -> abs(centsBetween(frequency, pitch, referencePitch)) }
        return DetectedNote(
            name = pitch.name,
            octave = pitch.octave,
            frequency = frequency,
            cents = centsBetween(frequency, pitch, referencePitch).roundToInt().coerceIn(-50, 50),
            stringIndex = index
        )
    }

    private fun centsBetween(frequency: Float, pitch: Pitch, referencePitch: Double): Double =
        1200.0 * log2(frequency / pitch.frequency(referencePitch))
}

private fun strings(vararg courses: String): List<InstrumentString> =
    courses.map { course -> InstrumentString(course.split("/").map(Pitch::parse)) }

private fun tuning(instrument: Instrument, variant: TuningVariant, vararg courses: String) = Tuning(
    id = "${instrument.name}_${variant.name}".lowercase(),
    instrument = instrument,
    variant = variant,
    strings = strings(*courses)
)

/** Standard tunings, grouped by instrument in display order. Strings run from the lowest-sounding side. */
val TUNINGS: List<Tuning> = listOf(
    tuning(Instrument.GUITAR, TuningVariant.STANDARD, "E2", "A2", "D3", "G3", "B3", "E4"),
    tuning(Instrument.GUITAR, TuningVariant.DROP_D, "D2", "A2", "D3", "G3", "B3", "E4"),
    tuning(Instrument.GUITAR, TuningVariant.HALF_STEP_DOWN, "D#2", "G#2", "C#3", "F#3", "A#3", "D#4"),
    tuning(Instrument.GUITAR, TuningVariant.WHOLE_STEP_DOWN, "D2", "G2", "C3", "F3", "A3", "D4"),
    tuning(Instrument.GUITAR, TuningVariant.DROP_C, "C2", "G2", "C3", "F3", "A3", "D4"),
    tuning(Instrument.GUITAR, TuningVariant.OPEN_G, "D2", "G2", "D3", "G3", "B3", "D4"),
    tuning(Instrument.GUITAR, TuningVariant.OPEN_D, "D2", "A2", "D3", "F#3", "A3", "D4"),
    tuning(Instrument.GUITAR, TuningVariant.OPEN_E, "E2", "B2", "E3", "G#3", "B3", "E4"),
    tuning(Instrument.GUITAR, TuningVariant.DADGAD, "D2", "A2", "D3", "G3", "A3", "D4"),
    tuning(Instrument.GUITAR, TuningVariant.SEVEN_STRINGS, "B1", "E2", "A2", "D3", "G3", "B3", "E4"),

    // 5-string banjo: the short 5th string (G4) is listed first, as it sits on the bass side.
    tuning(Instrument.BANJO, TuningVariant.OPEN_G, "G4", "D3", "G3", "B3", "D4"),
    tuning(Instrument.BANJO, TuningVariant.DOUBLE_C, "G4", "C3", "G3", "C4", "D4"),
    tuning(Instrument.BANJO, TuningVariant.TENOR, "C3", "G3", "D4", "A4"),
    tuning(Instrument.BANJO, TuningVariant.PLECTRUM, "C3", "G3", "B3", "D4"),

    // Re-entrant: the 4th string (G4) is higher than the 3rd.
    tuning(Instrument.UKULELE, TuningVariant.STANDARD, "G4", "C4", "E4", "A4"),
    tuning(Instrument.UKULELE, TuningVariant.LOW_G, "G3", "C4", "E4", "A4"),
    tuning(Instrument.UKULELE, TuningVariant.D_TUNING, "A4", "D4", "F#4", "B4"),
    tuning(Instrument.UKULELE, TuningVariant.BARITONE, "D3", "G3", "B3", "E4"),
    tuning(Instrument.UKULELE, TuningVariant.BASS, "E1", "A1", "D2", "G2"),

    tuning(Instrument.VIOLIN, TuningVariant.STANDARD, "G3", "D4", "A4", "E5"),
    tuning(Instrument.VIOLA, TuningVariant.STANDARD, "C3", "G3", "D4", "A4"),
    tuning(Instrument.CELLO, TuningVariant.STANDARD, "C2", "G2", "D3", "A3"),

    tuning(Instrument.DOUBLE_BASS, TuningVariant.STANDARD, "E1", "A1", "D2", "G2"),
    tuning(Instrument.DOUBLE_BASS, TuningVariant.SOLO, "F#1", "B1", "E2", "A2"),

    tuning(Instrument.ELECTRIC_BASS, TuningVariant.STANDARD, "E1", "A1", "D2", "G2"),
    tuning(Instrument.ELECTRIC_BASS, TuningVariant.FOUR_STRINGS_DROP_D, "D1", "A1", "D2", "G2"),
    tuning(Instrument.ELECTRIC_BASS, TuningVariant.FOUR_STRINGS_HALF_STEP_DOWN, "D#1", "G#1", "C#2", "F#2"),
    tuning(Instrument.ELECTRIC_BASS, TuningVariant.FIVE_STRINGS, "B0", "E1", "A1", "D2", "G2"),
    tuning(Instrument.ELECTRIC_BASS, TuningVariant.FIVE_STRINGS_HIGH_C, "E1", "A1", "D2", "G2", "C3"),
    tuning(Instrument.ELECTRIC_BASS, TuningVariant.SIX_STRINGS, "B0", "E1", "A1", "D2", "G2", "C3"),

    // Viola caipira: five pairs; the three lower pairs are tuned in octaves.
    tuning(Instrument.VIOLA_CAIPIRA, TuningVariant.CEBOLAO_E, "B2/B3", "E3/E4", "G#3/G#4", "B3", "E4"),
    tuning(Instrument.VIOLA_CAIPIRA, TuningVariant.CEBOLAO_D, "A2/A3", "D3/D4", "F#3/F#4", "A3", "D4"),
    tuning(Instrument.VIOLA_CAIPIRA, TuningVariant.RIO_ABAIXO, "G2/G3", "D3/D4", "G3/G4", "B3", "D4"),
    tuning(Instrument.VIOLA_CAIPIRA, TuningVariant.NATURAL, "A2/A3", "D3/D4", "G3/G4", "B3", "E4"),

    tuning(Instrument.CAVAQUINHO, TuningVariant.STANDARD, "D4", "G4", "B4", "D5"),
    tuning(Instrument.CAVAQUINHO, TuningVariant.NATURAL, "D4", "G4", "B4", "E5"),

    // Mandolin: four pairs tuned in unison, like a violin.
    tuning(Instrument.MANDOLIN, TuningVariant.STANDARD, "G3", "D4", "A4", "E5"),
)

fun tuningForId(id: String?): Tuning? = TUNINGS.find { it.id == id }
