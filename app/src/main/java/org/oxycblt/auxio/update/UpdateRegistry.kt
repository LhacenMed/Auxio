/*
 * Copyright (c) 2026 Auxio Project
 * UpdateRegistry.kt is part of Auxio.
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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide source of truth for the app-update flow. Holds the [available] update (set by
 * [UpdateChecker]) and the live [state] of its download (written by [UpdateService], observed by
 * the UI). Living outside any Activity means a configuration change — or dismissing the dialog —
 * never loses an in-flight APK download: re-opening simply re-reads the same flow.
 */
object UpdateRegistry {
    private val _available = MutableStateFlow<AppUpdate?>(null)
    val available: StateFlow<AppUpdate?> = _available.asStateFlow()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _promptRequest = MutableStateFlow(false)

    /**
     * Set when something has asked for the update dialog outright — the settings entry's manual
     * check. It overrides the automatic-check preference being off, and re-opens a prompt already
     * dismissed this session, so asking always gets an answer. Cleared once the dialog is shown.
     */
    val promptRequest: StateFlow<Boolean> = _promptRequest.asStateFlow()

    /** Records that a newer version exists; the UI shows the prompt off this. */
    fun setAvailable(update: AppUpdate) {
        _available.value = update
    }

    fun requestPrompt() {
        _promptRequest.value = true
    }

    fun clearPromptRequest() {
        _promptRequest.value = false
    }

    fun update(state: UpdateState) {
        _state.value = state
    }

    fun stateOf(): UpdateState = _state.value

    /** True while the APK is connecting or transferring. */
    val isActive: Boolean
        get() =
            when (_state.value) {
                UpdateState.Connecting,
                is UpdateState.Downloading -> true
                else -> false
            }
}
