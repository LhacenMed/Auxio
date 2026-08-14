/*
 * Copyright (c) 2026 Auxio Project
 * LyricsExtractor.kt is part of Auxio.
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

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.metadata.MetadataResult
import org.oxycblt.musikr.metadata.TagLibJNI

/**
 * Reads the lyrics of a single song straight from it's audio file.
 *
 * Lyrics are deliberately kept out of the indexing pipeline. They are large, only ever needed for
 * whatever is playing right now, and caching them for an entire library would cost far more memory
 * than re-reading a few kilobytes of tags on each song change.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
interface LyricsExtractor {
    /** Read the lyrics embedded in [song]'s audio file, or null if it has none. */
    suspend fun extract(song: Song): Lyrics?

    companion object {
        fun from(context: Context): LyricsExtractor = LyricsExtractorImpl(context.contentResolver)
    }
}

private class LyricsExtractorImpl(private val contentResolver: ContentResolver) : LyricsExtractor {
    override suspend fun extract(song: Song): Lyrics? =
        withContext(Dispatchers.IO) {
            // TagLib dispatches on the file extension, so a nameless path can't be read.
            val name = song.path.name ?: return@withContext null
            val result =
                try {
                    contentResolver.openFileDescriptor(song.uri, "r")?.use { fd ->
                        val fis = FileInputStream(fd.fileDescriptor)
                        TagLibJNI.open(name, fis).also { fis.close() }
                    }
                } catch (e: Exception) {
                    // A missing or unreadable file must degrade to "no lyrics", never crash
                    // the playback UI that is waiting on this.
                    Log.d(TAG, "Unable to read lyrics from $name", e)
                    null
                }
            (result as? MetadataResult.Success)?.metadata?.let(LyricsParser::parse)
        }

    private companion object {
        const val TAG = "LyricsExtractor"
    }
}
