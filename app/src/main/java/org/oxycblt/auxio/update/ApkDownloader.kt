/*
 * Copyright (c) 2026 Auxio Project
 * ApkDownloader.kt is part of Auxio.
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
import android.text.format.Formatter
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import org.oxycblt.auxio.BuildConfig

/** A downloaded APK that is a newer build than the running one, ready to hand to the installer. */
data class StagedApk(val file: File, val versionCode: Int, val versionName: String)

/**
 * Stateless engine that streams one APK to the cache, emitting [UpdateState] as a cold [Flow] —
 * [UpdateService] collects it on a background scope so the download outlives the dialog.
 */
object ApkDownloader {
    /** Where the downloaded APK lands. Stable name so a re-download overwrites the last attempt. */
    fun apkFile(context: Context): File =
        File(context.cacheDir, "updates").apply { mkdirs() }.resolve("auxio-update.apk")

    /**
     * Inspects the staged APK on launch and decides its fate by comparing its packaged versionCode
     * against the running build: a newer, not-yet-installed APK is returned so the install prompt
     * can resume; an already-installed (or stale) one is deleted in place and null returned. Cheap
     * no-op when nothing is staged.
     */
    fun stagedUpdate(context: Context): StagedApk? {
        val apk = apkFile(context)
        if (!apk.exists()) return null
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0)
        val code = info?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L
        if (code <= BuildConfig.VERSION_CODE) {
            apk.delete()
            return null
        }
        return StagedApk(apk, code.toInt(), info?.versionName.orEmpty())
    }

    fun download(context: Context, update: AppUpdate): Flow<UpdateState> =
        flow {
                val ctx = context.applicationContext
                val apk = apkFile(ctx)
                apk.delete()

                emit(UpdateState.Connecting)

                try {
                    val conn = openWithRedirects(update.apkUrl)
                    val totalBytes = conn.contentLengthLong.takeIf { it > 0 }
                    var received = 0L
                    try {
                        conn.inputStream.use { input ->
                            apk.outputStream().use { out ->
                                val buf = ByteArray(65_536)
                                var n: Int
                                while (input.read(buf).also { n = it } != -1) {
                                    yield()
                                    out.write(buf, 0, n)
                                    received += n
                                    emit(
                                        UpdateState.Downloading(
                                            progress =
                                                totalBytes?.let {
                                                    (received.toFloat() / it).coerceIn(0f, 1f)
                                                },
                                            log = formatProgress(ctx, received, totalBytes),
                                        )
                                    )
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: CancellationException) {
                    apk.delete()
                    throw e
                } catch (e: Exception) {
                    apk.delete()
                    emit(UpdateState.Error(e.message.orEmpty()))
                    return@flow
                }

                emit(UpdateState.Downloaded(apk))
            }
            .flowOn(Dispatchers.IO)

    /** "12 MB / 30 MB", or just the received size while the total is unknown. */
    private fun formatProgress(context: Context, received: Long, total: Long?): String {
        val have = Formatter.formatShortFileSize(context, received)
        return if (total != null) "$have / ${Formatter.formatShortFileSize(context, total)}"
        else have
    }

    /**
     * Opens a connection to [url], manually following cross-host redirects (Android's
     * HttpURLConnection only auto-follows same-host ones — GitHub release assets redirect to a
     * CDN).
     */
    private fun openWithRedirects(url: String): HttpURLConnection {
        var location = url
        repeat(5) {
            val conn =
                (URL(location).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30_000
                    readTimeout = 30_000
                }
            conn.connect()
            if (conn.responseCode in 300..399) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next != null) {
                    location = next
                    return@repeat
                }
            }
            return conn
        }
        error("Too many redirects for $url")
    }
}
