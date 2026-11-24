package dev.namdev.rotary.input

/** Which [HmiAction] each key gesture triggers. Immutable; derive variants with [with] and [without]. */
class KeyBindings(private val bindings: Map<Pair<HardKey, GestureType>, HmiAction>) {

    operator fun get(gesture: KeyGesture): HmiAction? = bindings[gesture.key to gesture.type]

    fun with(key: HardKey, type: GestureType, action: HmiAction): KeyBindings =
        KeyBindings(bindings + ((key to type) to action))

    fun without(key: HardKey, type: GestureType): KeyBindings = KeyBindings(bindings - (key to type))

    companion object {
        /** A typical steering-wheel layout. */
        val Default: KeyBindings = KeyBindings(
            buildMap {
                fun bind(key: HardKey, action: HmiAction, vararg types: GestureType) =
                    types.forEach { put(key to it, action) }

                val press = GestureType.Press
                val long = GestureType.LongPress
                val repeat = GestureType.Repeat

                // A nudge also fires on a long press, so a slow push still moves; it never repeats.
                bind(HardKey.Up, HmiAction.Nudge(Direction.Up), press, long)
                bind(HardKey.Down, HmiAction.Nudge(Direction.Down), press, long)
                bind(HardKey.Left, HmiAction.Nudge(Direction.Left), press, long)
                bind(HardKey.Right, HmiAction.Nudge(Direction.Right), press, long)
                bind(HardKey.Center, HmiAction.Select, press)
                bind(HardKey.Center, HmiAction.LongSelect, long)
                bind(HardKey.Back, HmiAction.Back, press)

                // Tap to skip, hold to seek, the convention on most steering wheels.
                bind(HardKey.MediaNext, HmiAction.NextTrack, press)
                bind(HardKey.MediaNext, HmiAction.SeekForward, long, repeat)
                bind(HardKey.MediaPrevious, HmiAction.PreviousTrack, press)
                bind(HardKey.MediaPrevious, HmiAction.SeekBackward, long, repeat)
                bind(HardKey.PlayPause, HmiAction.PlayPause, press)

                bind(HardKey.VolumeUp, HmiAction.Volume(+1), press, long, repeat)
                bind(HardKey.VolumeDown, HmiAction.Volume(-1), press, long, repeat)
                bind(HardKey.Mute, HmiAction.Mute, press)

                bind(HardKey.Voice, HmiAction.VoiceAssistant, press)
                bind(HardKey.Call, HmiAction.AnswerCall, press)
                bind(HardKey.Call, HmiAction.Redial, long)
                bind(HardKey.EndCall, HmiAction.EndCall, press)
            },
        )
    }
}

/** A recognised gesture and the action it is bound to (null when unbound). */
data class RoutedGesture(val gesture: KeyGesture, val action: HmiAction?)

/**
 * The whole input pipeline, without any Android dependency:
 * key events -> [KeyGestureDetector] -> [KeyBindings] -> actions, and rotary detents -> [RotaryAccelerator].
 *
 * Rotary detents act on DOWN, immediately and once per click, as rotation should; every other key goes through
 * gesture detection.
 */
class InputRouter(
    var bindings: KeyBindings = KeyBindings.Default,
    longPressMs: Long = KeyGestureDetector.DEFAULT_LONG_PRESS_MS,
    repeatIntervalMs: Long = KeyGestureDetector.DEFAULT_REPEAT_INTERVAL_MS,
    private val accelerator: RotaryAccelerator = RotaryAccelerator(),
) {
    private val detector = KeyGestureDetector(longPressMs, repeatIntervalMs)

    /** Every gesture [input] completes, with its bound action. */
    fun route(input: KeyInput): List<RoutedGesture> = when (input.key) {
        HardKey.RotateClockwise, HardKey.RotateCounterClockwise ->
            if (input.pressed) {
                val clockwise = input.key == HardKey.RotateClockwise
                listOf(RoutedGesture(KeyGesture(input.key, GestureType.Press), onRotary(clockwise, 1, input.timeMs)))
            } else {
                emptyList()
            }
        else -> detector.onKey(input).map { RoutedGesture(it, bindings[it]) }
    }

    /** Long presses and repeats that are due at [nowMs] for held keys, with their bound actions. */
    fun routeTick(nowMs: Long): List<RoutedGesture> = detector.onTick(nowMs).map { RoutedGesture(it, bindings[it]) }

    fun onKey(input: KeyInput): List<HmiAction> = route(input).mapNotNull { it.action }

    fun onTick(nowMs: Long): List<HmiAction> = routeTick(nowMs).mapNotNull { it.action }

    /** For rotary encoders that report scroll motion instead of key events. */
    fun onRotary(clockwise: Boolean, detents: Int, timeMs: Long): HmiAction.Rotate =
        HmiAction.Rotate(accelerator.steps(clockwise, detents, timeMs))

    fun nextDeadline(): Long? = detector.nextDeadline()

    /** Forget held keys and rotation history, e.g. when the window loses focus. */
    fun reset() {
        detector.reset()
        accelerator.reset()
    }
}
