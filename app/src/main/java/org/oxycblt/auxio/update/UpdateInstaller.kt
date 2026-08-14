/*
 * Copyright (c) 2026 Auxio Project
 * UpdateInstaller.kt is part of Auxio.
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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File

/**
 * Launches the system package installer for a downloaded APK. On Android O+ the app must first hold
 * the user's "install unknown apps" grant — [canInstall] reports it and [requestPermissionIntent]
 * opens the settings screen where the user grants it once.
 */
object UpdateInstaller {
    /** True when the installer can be launched directly (pre-O, or the grant is already held). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** ACTION_VIEW install intent for [apk], shared through the app's FileProvider. */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Settings screen where the user grants this app permission to install unknown apps. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun requestPermissionIntent(context: Context): Intent =
        Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
