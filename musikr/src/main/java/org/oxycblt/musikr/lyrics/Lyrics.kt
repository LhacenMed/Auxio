/*
 * Copyright (c) 2026 Auxio Project
 * Lyrics.kt is part of Auxio.
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

/**
 * The lyrics embedded in a song's audio file.
 *
 * Both variants expose their content as [lines] so that consumers can render synced and plain
 * lyrics with the same machinery, and only opt into timing when it is actually available.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
sealed interface Lyrics {
    /** Every lyric line, in display order. */
    val lines: List<String>

    /**
     * Time-synchronized lyrics that can be followed alongside playback.
     *
     * @param timedLines Every lyric line paired with the position it becomes active at, sorted
     *   ascending by that position.
     */
    data class Synced(val timedLines: List<TimedLine>) : Lyrics {
        override val lines = timedLines.map(TimedLine::text)
    }

    /** Plain lyrics carrying no timing information. */
    data class Plain(override val lines: List<String>) : Lyrics
}

/**
 * A single time-anchored lyric line.
 *
 * @param startMs The playback position at which this line becomes active, in milliseconds.
 * @param text The line's text. Empty for the instrumental breaks that LRC files use as spacers.
 */
data class TimedLine(val startMs: Long, val text: String)
