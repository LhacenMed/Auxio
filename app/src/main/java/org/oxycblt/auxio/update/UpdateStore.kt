/*
 * Copyright (c) 2026 Auxio Project
 * UpdateStore.kt is part of Auxio.
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

package org.oxycblt.auxio.update

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the discovered [AppUpdate] across launches so the prompt survives a cold start. Once a
 * newer build is found online its manifest is saved here; on the next launch [UpdateManager]
 * re-hydrates [UpdateRegistry] from it — so a user who first saw the prompt online still sees it
 * after relaunching offline. Cleared once the running build has caught up to the saved version.
 *
 * Deliberately its own preferences file rather than the shared settings store: this is internal
 * bookkeeping, not user configuration, and it should never appear alongside real settings.
 */
object UpdateStore {
    private const val PREFS = "auxio_update"
    private const val KEY_UPDATE = "available"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, update: AppUpdate) {
        prefs(context).edit { putString(KEY_UPDATE, update.toJson()) }
    }

    fun load(context: Context): AppUpdate? =
        prefs(context).getString(KEY_UPDATE, null)?.let {
            runCatching { AppUpdate.fromJson(it) }.getOrNull()
        }

    fun clear(context: Context) {
        prefs(context).edit { remove(KEY_UPDATE) }
    }
}
