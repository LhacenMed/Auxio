/*
 * Copyright (c) 2026 Auxio Project
 * LyricsState.kt is part of Auxio.
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
 
package org.oxycblt.auxio.playback.ui.lyrics

import org.oxycblt.musikr.lyrics.Lyrics

/**
 * The lyrics display state of the currently playing song.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
sealed interface LyricsState {
    /**
     * Lyrics are still being read from the song's file. Renders blank rather than as [Empty], so
     * that a song with lyrics never flashes a "no lyrics" placeholder before they arrive.
     */
    data object Loading : LyricsState

    /** Nothing is playing, or the playing song has no usable lyrics. */
    data object Empty : LyricsState

    /** Lyrics were found for the playing song. */
    data class Loaded(val lyrics: Lyrics) : LyricsState
}
