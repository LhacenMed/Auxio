/*
 * Copyright (c) 2026 Auxio Project
 * LyricsOverlay.kt is part of Auxio.
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

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import org.oxycblt.auxio.databinding.ViewLyricsOverlayBinding

/**
 * The reading view that takes the place of the cover art and the lyrics strip.
 *
 * It is the same [LyricsView] at a larger density, wrapped with the actions that only make sense
 * when lyrics have the whole area to themselves. Timing and scrolling are entirely the inner view's
 * job, so this stays in sync with the strip for free.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
class LyricsOverlay
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
    private val binding = ViewLyricsOverlayBinding.inflate(LayoutInflater.from(context), this)

    init {
        // Tapping the lyrics themselves collapses back to the cover art, the same way tapping
        // the strip opened this. The surrounding actions consume their own clicks.
        binding.lyricsList.setOnClickListener { performClick() }
    }

    /** @see LyricsView.update */
    fun update(state: LyricsState, positionMs: Long) {
        val isEmpty = state is LyricsState.Empty
        // The inner view has it's own placeholder, so hide it wholesale and offer the actions
        // that can fill the gap instead.
        binding.lyricsList.isInvisible = isEmpty
        binding.lyricsEmptyGroup.isInvisible = !isEmpty
        binding.lyricsList.update(state, positionMs)
    }

    /** @see LyricsView.seekTo */
    fun seekTo(positionMs: Long) {
        binding.lyricsList.seekTo(positionMs)
    }
}
