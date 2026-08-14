/*
 * Copyright (c) 2026 Auxio Project
 * LyricsView.kt is part of Auxio.
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
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import org.oxycblt.auxio.R
import org.oxycblt.auxio.databinding.ViewLyricsBinding
import org.oxycblt.musikr.lyrics.Lyrics
import org.oxycblt.musikr.lyrics.TimedLine

/**
 * The lyrics section of the playback panel.
 *
 * The view is a fixed-height window onto the lyric lines, so it occupies exactly the same space
 * whether the song has synced lyrics, plain lyrics, or none at all. Synced lyrics highlight and
 * auto-center the active line as playback advances; plain lyrics are simply scrollable.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
class LyricsView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
    private val binding = ViewLyricsBinding.inflate(LayoutInflater.from(context), this)
    private val lyricsAdapter = LyricsAdapter()
    private val layoutManager = LinearLayoutManager(context)
    private val lineHeight = context.resources.getDimensionPixelSize(R.dimen.size_lyric_line)

    private var timedLines: List<TimedLine> = emptyList()
    private var activeIndex = LyricsAdapter.NO_ACTIVE

    init {
        binding.lyricsRecycler.apply {
            adapter = lyricsAdapter
            layoutManager = this@LyricsView.layoutManager
            // The panel lives inside a bottom sheet, so leave nested drags to the sheet.
            isNestedScrollingEnabled = false
            // Rows never change size, and animating them would fight the auto-scroll.
            itemAnimator = null
            setHasFixedSize(true)
        }
    }

    /**
     * Display [state], seeding the active line from [positionMs] so that lyrics arriving while
     * paused (or mid-song) are immediately correct rather than waiting for the next tick.
     */
    fun update(state: LyricsState, positionMs: Long) {
        val lyrics = (state as? LyricsState.Loaded)?.lyrics
        timedLines = (lyrics as? Lyrics.Synced)?.timedLines ?: emptyList()
        lyricsAdapter.submit(lyrics?.lines ?: emptyList())

        binding.lyricsRecycler.isInvisible = lyrics == null
        binding.lyricsPlaceholder.isInvisible = state !is LyricsState.Empty

        activeIndex = indexAt(positionMs, LyricsAdapter.NO_ACTIVE)
        lyricsAdapter.setActiveIndex(activeIndex)
        jumpTo(activeIndex.coerceAtLeast(0))
    }

    /** Advance the highlight to whichever line [positionMs] falls in. A no-op for plain lyrics. */
    fun seekTo(positionMs: Long) {
        if (timedLines.isEmpty()) return
        val index = indexAt(positionMs, activeIndex)
        if (index == activeIndex) return
        activeIndex = index
        lyricsAdapter.setActiveIndex(index)
        if (index != LyricsAdapter.NO_ACTIVE) {
            centerOn(index)
        }
    }

    /**
     * Find the last line that has started by [positionMs], or [LyricsAdapter.NO_ACTIVE] if none has
     * yet.
     *
     * @param hint The previously active index. Playback is overwhelmingly monotonic, so checking
     *   the lines around it resolves almost every call without a search.
     */
    private fun indexAt(positionMs: Long, hint: Int): Int {
        if (
            hint > LyricsAdapter.NO_ACTIVE &&
                hint < timedLines.size &&
                timedLines[hint].startMs <= positionMs
        ) {
            val next = hint + 1
            if (next == timedLines.size || timedLines[next].startMs > positionMs) return hint
            if (next + 1 == timedLines.size || timedLines[next + 1].startMs > positionMs)
                return next
        }
        return search(positionMs)
    }

    /** Binary search fallback for seeks and freshly loaded lyrics. */
    private fun search(positionMs: Long): Int {
        var low = 0
        var high = timedLines.size - 1
        var found = LyricsAdapter.NO_ACTIVE
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (timedLines[mid].startMs <= positionMs) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }

    /** Place [index] in the middle of the window without animating. */
    private fun jumpTo(index: Int) {
        val viewport = binding.lyricsRecycler.height
        if (viewport == 0) {
            // Not laid out yet, so the centering offset isn't known. Being visible is enough;
            // the next line change will center it properly.
            layoutManager.scrollToPosition(index)
        } else {
            layoutManager.scrollToPositionWithOffset(index, (viewport - lineHeight) / 2)
        }
    }

    /** Glide [index] into the middle of the window. */
    private fun centerOn(index: Int) {
        // A fresh scroller each time: RecyclerView will not restart one that is still running.
        layoutManager.startSmoothScroll(
            CenterSmoothScroller(context).apply { targetPosition = index }
        )
    }
}

/** A [LinearSmoothScroller] that centers it's target and moves at a reading-friendly pace. */
private class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int,
    ) = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) =
        MILLIS_PER_INCH / displayMetrics.densityDpi

    private companion object {
        /** Well below the 25f default, so lines drift rather than snap. */
        const val MILLIS_PER_INCH = 90f
    }
}
