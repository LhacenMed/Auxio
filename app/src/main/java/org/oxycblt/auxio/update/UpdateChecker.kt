/*
 * Copyright (c) 2026 Auxio Project
 * UpdateChecker.kt is part of Auxio.
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

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.auxio.BuildConfig

/**
 * Fetches the remote version manifest and decides whether a newer build exists. Best-effort: any
 * network or parse failure returns null so a launch is never blocked by a failed update check.
 */
object UpdateChecker {
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/LhacenMed/Auxio/main/version.json"

    /** Returns the available update when the manifest's versionCode exceeds the installed one. */
    suspend fun check(): AppUpdate? =
        withContext(Dispatchers.IO) {
            runCatching {
                    val conn =
                        (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15_000
                            readTimeout = 15_000
                        }
                    val json =
                        try {
                            conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                        } finally {
                            conn.disconnect()
                        }
                    AppUpdate.fromJson(json).takeIf { it.versionCode > BuildConfig.VERSION_CODE }
                }
                .getOrNull()
        }
}
