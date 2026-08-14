/*
 * Copyright (c) 2026 Auxio Project
 * AppUpdate.kt is part of Auxio.
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

import org.json.JSONObject

/**
 * Parsed remote version manifest (version.json). [versionCode] is compared against the installed
 * `BuildConfig.VERSION_CODE` to decide whether a newer build exists; [apkUrl] points at the GitHub
 * Releases asset to download.
 *
 * Manifest shape:
 * ```json
 * { "versionCode": 40106, "versionName": "4.1.6",
 *   "apkUrl": "https://github.com/.../releases/download/v4.1.6/auxio-4.1.6.apk",
 *   "notes": "What's new…" }
 * ```
 */
data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
) {
    /** Serializes back to the manifest shape so [UpdateStore] can persist it across launches. */
    fun toJson(): String =
        JSONObject()
            .put("versionCode", versionCode)
            .put("versionName", versionName)
            .put("apkUrl", apkUrl)
            .put("notes", notes)
            .toString()

    companion object {
        fun fromJson(json: String): AppUpdate =
            JSONObject(json).run {
                AppUpdate(
                    versionCode = getInt("versionCode"),
                    versionName = getString("versionName"),
                    apkUrl = getString("apkUrl"),
                    notes = optString("notes"),
                )
            }
    }
}
