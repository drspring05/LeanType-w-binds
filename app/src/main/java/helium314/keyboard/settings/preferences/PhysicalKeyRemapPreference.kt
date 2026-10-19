// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.event.PhysicalKeyRemap
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.ListPickerDialog

/**
 * Preference that opens a dialog for managing physical key remapping rules (e.g. Caps Lock -> Backspace).
 * Rules only apply to physical keyboards.
 */
@Composable
fun PhysicalKeyRemapPreference(setting: Setting) {
    val prefs = LocalContext.current.prefs()
    var rules by remember { mutableStateOf(PhysicalKeyRemap.parse(prefs.getString(Settings.PREF_PHYSICAL_KEY_REMAPS, Defaults.PREF_PHYSICAL_KEY_REMAPS))) }
    var showRules by remember { mutableStateOf(false) }
    var showCapture by remember { mutableStateOf(false) }
    var capturedKey by remember { mutableStateOf<Int?>(null) }

    fun save(newRules: Map<Int, Int>) {
        rules = newRules
        prefs.edit { putString(Settings.PREF_PHYSICAL_KEY_REMAPS, PhysicalKeyRemap.serialize(newRules)) }
    }

    val summary = if (rules.isEmpty()) stringResource(R.string.physical_key_remap_none)
        else rules.entries.joinToString(", ") { "${PhysicalKeyRemap.keyName(it.key)} → ${PhysicalKeyRemap.keyName(it.value)}" }
    Preference(name = setting.title, description = summary, onClick = { showRules = true })

    if (showRules) {
        ConfirmationDialog(
            onDismissRequest = { showRules = false },
            onConfirmed = { showRules = false },
            title = { Text(setting.title) },
            neutralButtonText = stringResource(R.string.physical_key_remap_add),
            onNeutral = { showCapture = true },
            content = {
                Column {
                    Text(stringResource(R.string.physical_key_remap_summary), modifier = Modifier.padding(bottom = 8.dp))
                    rules.forEach { (src, dst) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${PhysicalKeyRemap.keyName(src)} → ${PhysicalKeyRemap.keyName(dst)}", modifier = Modifier.weight(1f))
                            IconButton(onClick = { save(rules - src) }) {
                                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCapture) {
        val focusRequester = remember { FocusRequester() }
        ConfirmationDialog(
            onDismissRequest = { showCapture = false },
            onConfirmed = { showCapture = false },
            confirmButtonText = null,
            cancelButtonText = stringResource(android.R.string.cancel),
            title = { Text(stringResource(R.string.physical_key_remap_press_key)) },
            content = {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent {
                            val code = it.key.nativeKeyCode
                            if (it.type == KeyEventType.KeyDown && code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_UNKNOWN) {
                                capturedKey = code
                                showCapture = false
                                true
                            } else false
                        }
                ) {
                    Text(stringResource(R.string.physical_key_remap_press_key_hint))
                }
            }
        )
    }

    capturedKey?.let { src ->
        // keep the source key itself out of the list, mapping a key to itself makes no sense
        val targets = PhysicalKeyRemap.targetKeyCodes.filter { it != src }
        ListPickerDialog(
            onDismissRequest = { capturedKey = null },
            items = targets,
            onItemSelected = { dst ->
                save(rules + (src to dst))
                capturedKey = null
            },
            title = { Text(stringResource(R.string.physical_key_remap_choose_target, PhysicalKeyRemap.keyName(src))) },
            getItemName = { PhysicalKeyRemap.keyName(it) },
            showRadioButtons = false,
        )
    }
}
