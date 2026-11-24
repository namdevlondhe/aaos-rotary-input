package dev.namdev.rotary.compose

import android.view.KeyEvent
import dev.namdev.rotary.input.HardKey

/** The Android key code each control produces. One table, so the two directions cannot drift apart. */
private val keyCodeByHardKey: Map<HardKey, Int> = mapOf(
    HardKey.Up to KeyEvent.KEYCODE_DPAD_UP,
    HardKey.Down to KeyEvent.KEYCODE_DPAD_DOWN,
    HardKey.Left to KeyEvent.KEYCODE_DPAD_LEFT,
    HardKey.Right to KeyEvent.KEYCODE_DPAD_RIGHT,
    HardKey.Center to KeyEvent.KEYCODE_DPAD_CENTER,
    HardKey.Back to KeyEvent.KEYCODE_BACK,
    HardKey.RotateClockwise to KeyEvent.KEYCODE_NAVIGATE_NEXT,
    HardKey.RotateCounterClockwise to KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
    HardKey.MediaNext to KeyEvent.KEYCODE_MEDIA_NEXT,
    HardKey.MediaPrevious to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
    HardKey.PlayPause to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
    HardKey.VolumeUp to KeyEvent.KEYCODE_VOLUME_UP,
    HardKey.VolumeDown to KeyEvent.KEYCODE_VOLUME_DOWN,
    HardKey.Mute to KeyEvent.KEYCODE_VOLUME_MUTE,
    HardKey.Voice to KeyEvent.KEYCODE_VOICE_ASSIST,
    HardKey.Call to KeyEvent.KEYCODE_CALL,
    HardKey.EndCall to KeyEvent.KEYCODE_ENDCALL,
)

private val hardKeyByKeyCode: Map<Int, HardKey> =
    keyCodeByHardKey.entries.associate { (key, code) -> code to key } +
        mapOf(KeyEvent.KEYCODE_ENTER to HardKey.Center, KeyEvent.KEYCODE_HEADSETHOOK to HardKey.PlayPause)

/** Android key codes produced by rotary controllers, D-pads and steering-wheel switches. */
fun hardKeyOf(keyCode: Int): HardKey? = hardKeyByKeyCode[keyCode]

/** The reverse of [hardKeyOf], for code that injects real key events. */
fun keyCodeOf(key: HardKey): Int = keyCodeByHardKey.getValue(key)

/**
 * Keys the host handles by default. Select and Back are left to Compose (clickable reacts to DPAD_CENTER,
 * BackHandler to BACK) and volume to the system, unless an app opts in.
 */
val DefaultInterceptedKeys: Set<HardKey> = HardKey.entries.toSet() -
    setOf(HardKey.Center, HardKey.Back, HardKey.VolumeUp, HardKey.VolumeDown, HardKey.Mute)
