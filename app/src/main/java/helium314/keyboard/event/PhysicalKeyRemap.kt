// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.event

import android.view.KeyEvent

/**
 * User-defined remapping of physical (hardware) keys, e.g. Caps Lock -> Backspace.
 * Only applies to events coming from a physical keyboard, the on-screen keyboard is not affected.
 *
 * Rules are stored in a preference as a comma separated list of "source>target" Android key codes.
 */
object PhysicalKeyRemap {
    /** Targets offered in the settings UI. Any Android key code is supported by the parser, this is just the picker list. */
    val targetKeyCodes = listOf(
        KeyEvent.KEYCODE_DEL,
        KeyEvent.KEYCODE_FORWARD_DEL,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_TAB,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_CAPS_LOCK,
    )

    fun parse(value: String?): Map<Int, Int> {
        if (value.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<Int, Int>()
        value.split(',').forEach { rule ->
            val parts = rule.split('>')
            if (parts.size != 2) return@forEach
            val src = parts[0].trim().toIntOrNull() ?: return@forEach
            val dst = parts[1].trim().toIntOrNull() ?: return@forEach
            if (src != dst) result[src] = dst
        }
        return result
    }

    fun serialize(rules: Map<Int, Int>): String = rules.entries.joinToString(",") { "${it.key}>${it.value}" }

    fun keyName(keyCode: Int): String =
        KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").lowercase()
            .split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    /**
     * Returns [event] with the remapping applied, or [event] itself if nothing changes.
     * While Caps Lock is a remapped source key, its lock state is removed from the meta state,
     * so toggling it (which Android still does internally) does not turn typed letters into capitals.
     */
    fun apply(event: KeyEvent, rules: Map<Int, Int>): KeyEvent {
        if (rules.isEmpty()) return event
        val target = rules[event.keyCode] ?: event.keyCode
        val stripCapsLock = rules.containsKey(KeyEvent.KEYCODE_CAPS_LOCK) && rules[KeyEvent.KEYCODE_CAPS_LOCK] != KeyEvent.KEYCODE_CAPS_LOCK
        val meta = if (stripCapsLock) event.metaState and KeyEvent.META_CAPS_LOCK_ON.inv() else event.metaState
        if (target == event.keyCode && meta == event.metaState) return event
        return KeyEvent(event.downTime, event.eventTime, event.action, target, event.repeatCount, meta,
            event.deviceId, event.scanCode, event.flags, event.source)
    }
}
