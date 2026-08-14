/*
 * Copyright (c) 2026 Auxio Project
 * LyricsDensity.kt is part of Auxio.
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

import androidx.annotation.DimenRes
import androidx.annotation.LayoutRes
import org.oxycblt.auxio.R

/**
 * The two sizes a [LyricsView] renders at. Everything that differs between the strip under the
 * cover art and the reading view that replaces it lives here, so both share one timing and
 * scrolling implementation.
 *
 * The ordinals must match the `lyricsDensity` attribute's enum values.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
internal enum class LyricsDensity(
    @LayoutRes val lineLayout: Int,
    /** The height of one unwrapped line, used to blank out space above and below the list. */
    @DimenRes val lineHeight: Int,
    @DimenRes val fadeLength: Int,
) {
    /** The three-line strip shown under the cover art. */
    COMPACT(R.layout.item_lyric_line, R.dimen.size_lyric_line, R.dimen.size_lyric_fade),

    /** The full-size reading view shown in place of the cover art. */
    EXPANDED(
        R.layout.item_lyric_line_large,
        R.dimen.size_lyric_line_large,
        R.dimen.size_lyric_fade_large,
    ),
}
