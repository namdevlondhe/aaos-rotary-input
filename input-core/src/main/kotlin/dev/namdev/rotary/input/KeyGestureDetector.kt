package dev.namdev.rotary.input

/**
 * Turns key DOWN/UP events into short presses, long presses and hold-repeats.
 *
 * Works with both kinds of key source found in cars:
 * - keys that auto-repeat while held (Android's repeated DOWN events), and
 * - keys that only report DOWN and UP (many steering-wheel switches routed through the VHAL). For those the host
 *   calls [onTick] at [nextDeadline], so a long press fires while the key is still held, not only on release.
 *
 * Each key is tracked independently, so holding one key never blocks another. Time comes from the events,
 * which makes the detector deterministic and testable without a real clock.
 */
class KeyGestureDetector(
    private val longPressMs: Long = DEFAULT_LONG_PRESS_MS,
    private val repeatIntervalMs: Long = DEFAULT_REPEAT_INTERVAL_MS,
) {
    private class Held(val downAt: Long) {
        var longFired = false
        var lastFiredAt = 0L
        var repeats = 0
    }

    private val held = LinkedHashMap<HardKey, Held>()

    init {
        require(longPressMs > 0 && repeatIntervalMs > 0) { "Timings must be positive" }
    }

    fun onKey(input: KeyInput): List<KeyGesture> = if (input.pressed) onDown(input) else onUp(input)

    /** Fires long presses and repeats that are due at [nowMs] for keys still held. */
    fun onTick(nowMs: Long): List<KeyGesture> = held.entries.flatMap { (key, state) -> advance(key, state, nowMs) }

    /** When [onTick] could next produce a gesture, or null if no key is held. */
    fun nextDeadline(): Long? = held.values.minOfOrNull {
        if (it.longFired) it.lastFiredAt + repeatIntervalMs else it.downAt + longPressMs
    }

    fun reset() = held.clear()

    private fun onDown(input: KeyInput): List<KeyGesture> {
        val state = held[input.key]
        if (state == null) {
            held[input.key] = Held(input.timeMs)
            return emptyList()
        }
        // An auto-repeated DOWN of a key we already track is just a clock tick for that key.
        return advance(input.key, state, input.timeMs)
    }

    private fun onUp(input: KeyInput): List<KeyGesture> {
        val state = held.remove(input.key)
        return when {
            state == null || input.canceled || state.longFired -> emptyList()
            input.timeMs - state.downAt >= longPressMs -> listOf(KeyGesture(input.key, GestureType.LongPress))
            else -> listOf(KeyGesture(input.key, GestureType.Press))
        }
    }

    private fun advance(key: HardKey, state: Held, nowMs: Long): List<KeyGesture> = when {
        !state.longFired && nowMs - state.downAt >= longPressMs -> {
            state.longFired = true
            state.lastFiredAt = nowMs
            listOf(KeyGesture(key, GestureType.LongPress))
        }
        state.longFired && nowMs - state.lastFiredAt >= repeatIntervalMs -> {
            state.lastFiredAt = nowMs
            state.repeats++
            listOf(KeyGesture(key, GestureType.Repeat, state.repeats))
        }
        else -> emptyList()
    }

    companion object {
        /** Android's default long-press timeout (ViewConfiguration). */
        const val DEFAULT_LONG_PRESS_MS = 500L
        const val DEFAULT_REPEAT_INTERVAL_MS = 150L
    }
}
