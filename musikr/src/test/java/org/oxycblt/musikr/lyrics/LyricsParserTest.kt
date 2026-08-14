/*
 * Copyright (c) 2026 Auxio Project
 * LyricsParserTest.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.musikr.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.oxycblt.musikr.metadata.Metadata
import org.oxycblt.musikr.metadata.Properties

class LyricsParserTest {
    @Test
    fun parse_lrc_timestampFormats() {
        val lyrics =
            parseXiph(
                """
                [00:01.50]Centiseconds
                [00:02.250]Milliseconds
                [00:03]No fraction
                [01:04.00]Past a minute
                [100:00.00]Past a hundred minutes
                """
                    .trimIndent()
            )
        assertEquals(
            listOf(
                TimedLine(1500, "Centiseconds"),
                TimedLine(2250, "Milliseconds"),
                TimedLine(3000, "No fraction"),
                TimedLine(64_000, "Past a minute"),
                TimedLine(6_000_000, "Past a hundred minutes"),
            ),
            synced(lyrics).timedLines,
        )
    }

    @Test
    fun parse_lrc_sortsAndExpandsRepeats() {
        val lyrics =
            parseXiph(
                """
                [00:20.00]Later
                [ar:Some Artist]
                [00:05.00][00:30.00]Chorus
                [00:10.00]
                """
                    .trimIndent()
            )
        assertEquals(
            listOf(
                TimedLine(5000, "Chorus"),
                TimedLine(10_000, ""),
                TimedLine(20_000, "Later"),
                TimedLine(30_000, "Chorus"),
            ),
            synced(lyrics).timedLines,
        )
    }

    @Test
    fun parse_lrc_rejectsMalformedStamps() {
        // None of these are timestamps, so the whole field falls back to plain lyrics.
        val lyrics =
            parseXiph(
                """
                [Chorus]
                [00:60.00]Bad seconds
                [00:5.00]Short seconds
                [xx:yy.zz]Not digits
                [00:01.0000]Too much precision
                [00:01.00 trailing]Junk in the stamp
                """
                    .trimIndent()
            )
        assertTrue(lyrics is Lyrics.Plain)
        assertEquals(6, lyrics!!.lines.size)
    }

    @Test
    fun parse_prefersSyncedOverPlain() {
        // USLT is read before the Xiph field, but the synced field must still win.
        val metadata =
            createMetadata(
                id3v2 = mapOf("USLT" to listOf("Plain line")),
                xiph = mapOf("LYRICS" to listOf("[00:01.00]Timed line")),
            )
        assertEquals(
            listOf(TimedLine(1000, "Timed line")),
            synced(LyricsParser.parse(metadata)).timedLines,
        )
    }

    @Test
    fun parse_fieldSources() {
        assertEquals(
            listOf(TimedLine(0, "From SYLT")),
            synced(
                    LyricsParser.parse(
                        createMetadata(id3v2 = mapOf("SYLT" to listOf("[00:00.00]From SYLT")))
                    )
                )
                .timedLines,
        )
        assertEquals(
            listOf("From ©lyr"),
            LyricsParser.parse(createMetadata(mp4 = mapOf("©lyr" to listOf("From ©lyr"))))?.lines,
        )
        assertEquals(
            listOf("From TXXX"),
            LyricsParser.parse(createMetadata(id3v2 = mapOf("TXXX:LYRICS" to listOf("From TXXX"))))
                ?.lines,
        )
    }

    @Test
    fun parse_emptySources() {
        assertNull(LyricsParser.parse(createMetadata()))
        assertNull(LyricsParser.parse(createMetadata(xiph = mapOf("LYRICS" to listOf("   \n  ")))))
    }

    private fun parseXiph(raw: String) =
        LyricsParser.parse(createMetadata(xiph = mapOf("LYRICS" to listOf(raw))))

    private fun synced(lyrics: Lyrics?) = lyrics as Lyrics.Synced

    private fun createMetadata(
        id3v2: Map<String, List<String>> = mapOf(),
        xiph: Map<String, List<String>> = mapOf(),
        mp4: Map<String, List<String>> = mapOf(),
    ) =
        Metadata(
            id3v2 = id3v2,
            xiph = xiph,
            mp4 = mp4,
            cover = null,
            properties = Properties("audio/mpeg", 0, 0, 0),
        )
}
