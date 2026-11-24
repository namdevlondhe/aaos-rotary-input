package dev.namdev.rotary.input

/** Physical controls of a vehicle: rotary controller, D-pad/nudge and steering-wheel keys. */
enum class HardKey {
    Up, Down, Left, Right,
    Center, Back,

    /** One detent of the rotary controller (KEYCODE_NAVIGATE_NEXT / _PREVIOUS). */
    RotateClockwise, RotateCounterClockwise,

    MediaNext, MediaPrevious, PlayPause,
    VolumeUp, VolumeDown, Mute,
    Voice, Call, EndCall,
}

enum class Direction { Up, Down, Left, Right }

/** A platform-independent key event. [timeMs] is a monotonic clock (on Android, `KeyEvent.eventTime`). */
data class KeyInput(
    val key: HardKey,
    val pressed: Boolean,
    val timeMs: Long,
    /** Android auto-repeat counter of a held key: 0 for the first DOWN. */
    val repeatCount: Int = 0,
    /** The system cancelled the press (e.g. the window lost focus); no gesture must fire. */
    val canceled: Boolean = false,
)

enum class GestureType { Press, LongPress, Repeat }

/** What the user did with a key: a short press, a long press, or the n-th repeat while still holding it. */
data class KeyGesture(val key: HardKey, val type: GestureType, val repeat: Int = 0)

/** What the HMI should do. Rotation and nudges move focus; the rest are handed to the app. */
sealed interface HmiAction {
    /** Move focus within the current focus area; positive is clockwise/next. */
    data class Rotate(val steps: Int) : HmiAction

    /** Move focus to the neighbouring focus area. */
    data class Nudge(val direction: Direction) : HmiAction

    data object Select : HmiAction
    data object LongSelect : HmiAction
    data object Back : HmiAction
    data object NextTrack : HmiAction
    data object PreviousTrack : HmiAction
    data object SeekForward : HmiAction
    data object SeekBackward : HmiAction
    data object PlayPause : HmiAction
    data class Volume(val delta: Int) : HmiAction
    data object Mute : HmiAction
    data object VoiceAssistant : HmiAction
    data object AnswerCall : HmiAction
    data object Redial : HmiAction
    data object EndCall : HmiAction
}
