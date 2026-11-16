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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningTest {

    private val guitar = tuningForId("guitar_standard")!!

    @Test
    fun pitchFrequencies() {
        assertEquals(440.0, Pitch.parse("A4").frequency(), 0.001)
        assertEquals(82.407, Pitch.parse("E2").frequency(), 0.001)
        assertEquals(30.868, Pitch.parse("B0").frequency(), 0.001)
        assertEquals(442.0, Pitch.parse("A4").frequency(442.0), 0.001)
    }

    @Test
    fun parsesSharpNotes() {
        assertEquals(Pitch("F#", 3), Pitch.parse("F#3"))
    }

    @Test
    fun picksClosestString() {
        val note = guitar.noteFor(111f, 440.0)
        assertEquals("A", note.name)
        assertEquals(2, note.octave)
        assertEquals(1, note.stringIndex)
        assertTrue(note.cents > 0)
    }

    @Test
    fun lockedStringIsUsedEvenWhenFarOff() {
        val note = guitar.noteFor(111f, 440.0, lockedString = 0)
        assertEquals("E", note.name)
        assertEquals(0, note.stringIndex)
        assertEquals(50, note.cents)
    }

    @Test
    fun octaveCourseMatchesEitherPitch() {
        val violaCaipira = tuningForId("viola_caipira_cebolao_d")!!
        val note = violaCaipira.noteFor(220f, 440.0)
        // A3 belongs both to the octave course 0 (A2/A3) and to course 3 (A3); either is a valid target.
        assertEquals("A", note.name)
        assertEquals(3, note.octave)
    }

    @Test
    fun tuningIdsAreUnique() {
        assertEquals(TUNINGS.size, TUNINGS.map { it.id }.toSet().size)
    }

    @Test
    fun everyInstrumentHasATuning() {
        assertEquals(Instrument.entries.toSet(), TUNINGS.map { it.instrument }.toSet())
    }
}
