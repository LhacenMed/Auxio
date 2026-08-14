/*
 * Copyright (c) 2026 Auxio Project
 * UpdateDialog.kt is part of Auxio.
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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.oxycblt.auxio.R
import org.oxycblt.auxio.databinding.DialogUpdateBinding
import org.oxycblt.auxio.ui.ViewBindingMaterialDialogFragment
import org.oxycblt.auxio.util.collectImmediately

/**
 * Prompt that walks the user through downloading and installing a newer build, mirroring the live
 * [UpdateState] as it goes.
 *
 * Dismissing during a download deliberately leaves the foreground service running — the notification
 * carries the progress from there, and re-opening the dialog re-reads the same flow, resuming where
 * it left off.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
class UpdateDialog : ViewBindingMaterialDialogFragment<DialogUpdateBinding>() {
    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.lbl_update_available)
            // Both listeners are null so the dialog does not auto-dismiss on click: the primary
            // action changes the state in place rather than closing the prompt. Rebound in onStart.
            .setPositiveButton(R.string.lbl_update, null)
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onCreateBinding(inflater: LayoutInflater) = DialogUpdateBinding.inflate(inflater)

    override fun onBindingCreated(binding: DialogUpdateBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // Asking for the prompt has been answered by showing it; clearing here keeps a later
        // manual check able to ask again.
        UpdateRegistry.clearPromptRequest()

        collectImmediately(UpdateRegistry.available, ::updateAvailable)
        collectImmediately(UpdateRegistry.state, ::updateState)
    }

    override fun onStart() {
        super.onStart()
        // The builder's null listener leaves the button inert until it is bound here, which is the
        // only point at which the AlertDialog's buttons exist.
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            onPrimaryAction()
        }
        // For the same reason the state applied while binding could not reach the buttons, which
        // matters when re-opening onto a download that has already finished.
        updateState(UpdateRegistry.state.value)
    }

    private fun updateAvailable(update: AppUpdate?) {
        val binding = binding ?: return
        // Nothing left to offer — the update was installed, or was never there.
        if (update == null) {
            dismiss()
            return
        }
        binding.updateNotes.text =
            getString(R.string.fmt_update_available, update.versionName, update.notes)
    }

    private fun updateState(state: UpdateState) {
        val binding = binding ?: return
        val positive = (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)

        when (state) {
            is UpdateState.Downloading -> binding.updateStatus.text = state.log
            UpdateState.Connecting ->
                binding.updateStatus.setText(R.string.lbl_update_connecting)
            is UpdateState.Downloaded -> binding.updateStatus.setText(R.string.lbl_update_ready)
            is UpdateState.Error -> binding.updateStatus.setText(R.string.err_update_failed)
            UpdateState.Idle -> binding.updateStatus.text = ""
        }

        // A total size is only known once the response headers land, so the bar spins until then.
        // Every other state settles it back to determinate rather than leaving it spinning unseen.
        val downloading = state as? UpdateState.Downloading
        val indeterminate =
            state is UpdateState.Connecting || (downloading != null && downloading.progress == null)
        val progress = binding.updateProgress
        if (progress.isIndeterminate != indeterminate) {
            // Material refuses to switch modes on a showing indicator, so hide it across the swap.
            progress.visibility = View.INVISIBLE
            progress.isIndeterminate = indeterminate
        }
        downloading?.progress?.let { progress.setProgressCompat((it * 100).toInt(), true) }
        // Applied last, and only ever VISIBLE/INVISIBLE: the row keeps its space in every state so
        // the dialog never resizes underneath the user.
        progress.visibility = if (UpdateRegistry.isActive) View.VISIBLE else View.INVISIBLE

        positive?.apply {
            isEnabled = !UpdateRegistry.isActive
            setText(
                if (state is UpdateState.Downloaded) R.string.lbl_install else R.string.lbl_update
            )
        }
    }

    /** Download the update, or install it once it is on disk. */
    private fun onPrimaryAction() {
        val context = requireContext()
        when (val state = UpdateRegistry.state.value) {
            is UpdateState.Downloaded -> {
                if (UpdateInstaller.canInstall(context)) {
                    startActivity(UpdateInstaller.installIntent(context, state.apk))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // The grant is a one-off; the install is retried by pressing the button again.
                    startActivity(UpdateInstaller.requestPermissionIntent(context))
                }
            }
            else -> UpdateService.start(context)
        }
    }
}
