/*
 * Copyright (c) 2026 Auxio Project
 * UpdatePrefs.kt is part of Auxio.
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
import androidx.preference.PreferenceManager
import org.oxycblt.auxio.R

/**
 * Whether a launch looks for a new version on its own.
 *
 * This governs the automatic check and nothing else — the manual check in settings works either
 * way, and a staged APK still resumes into its install prompt. Reads the same store the settings
 * switch writes to, so the two can never disagree.
 */
object UpdatePrefs {
    fun autoCheckEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.set_key_auto_update), true)
}
