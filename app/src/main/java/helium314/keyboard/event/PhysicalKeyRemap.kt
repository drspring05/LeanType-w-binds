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
    /** Frequently used targets, shown first in the settings picker. Any Android key code is supported by the parser. */
    val commonTargetKeyCodes = listOf(
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

    /**
     * Every Android key code that has a proper name (e.g. KEYCODE_GRAVE), so any key can be a remap target.
     * Common targets come first, the rest is sorted by name.
     */
    val allKeyCodes: List<Int> by lazy {
        val others = (1..MAX_KEY_CODE)
            .filter { it !in commonTargetKeyCodes && KeyEvent.keyCodeToString(it).startsWith("KEYCODE_") }
            .sortedBy { keyName(it) }
        commonTargetKeyCodes + others
    }

    private const val MAX_KEY_CODE = 330

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

    /** True if some key is remapped to Caps Lock, in which case the keyboard keeps its own Caps Lock state. */
    fun targetsCapsLock(rules: Map<Int, Int>): Boolean = rules.containsValue(KeyEvent.KEYCODE_CAPS_LOCK)

    /**
     * Returns [event] with the remapping applied, or [event] itself if nothing changes.
     * While Caps Lock is a remapped source key, its lock state is removed from the meta state,
     * so toggling it (which Android still does internally) does not turn typed letters into capitals.
     *
     * Android only toggles Caps Lock for the real Caps Lock key, so when a key is remapped to Caps Lock
     * the system state is ignored and [capsLockOn] (kept by the caller, toggled on each press) is used instead.
     */
    fun apply(event: KeyEvent, rules: Map<Int, Int>, capsLockOn: Boolean = false): KeyEvent {
        if (rules.isEmpty()) return event
        val target = rules[event.keyCode] ?: event.keyCode
        val capsLockIsTarget = targetsCapsLock(rules)
        val stripCapsLock = capsLockIsTarget
            || (rules.containsKey(KeyEvent.KEYCODE_CAPS_LOCK) && rules[KeyEvent.KEYCODE_CAPS_LOCK] != KeyEvent.KEYCODE_CAPS_LOCK)
        var meta = if (stripCapsLock) event.metaState and KeyEvent.META_CAPS_LOCK_ON.inv() else event.metaState
        if (capsLockIsTarget && capsLockOn) meta = meta or KeyEvent.META_CAPS_LOCK_ON
        if (target == event.keyCode && meta == event.metaState) return event
        return KeyEvent(event.downTime, event.eventTime, event.action, target, event.repeatCount, meta,
            event.deviceId, event.scanCode, event.flags, event.source)
    }
}
