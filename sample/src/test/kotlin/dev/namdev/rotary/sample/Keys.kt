package dev.namdev.rotary.sample

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress

/** Sends real Android key events with explicit timestamps, as a rotary controller or steering wheel would. */
class Keys(private val compose: ComposeContentTestRule) {
    var now = 10_000L
        private set

    fun send(keyCode: Int, action: Int, repeat: Int = 0, downTime: Long = now) {
        compose.onRoot().performKeyPress(KeyEvent(android.view.KeyEvent(downTime, now, action, keyCode, repeat)))
        compose.waitForIdle()
    }

    /** A short press, then a pause long enough that rotation does not accelerate. */
    fun tap(keyCode: Int) {
        val down = now
        send(keyCode, ACTION_DOWN)
        now += 60
        send(keyCode, ACTION_UP, downTime = down)
        now += 400
    }

    /** Holds a key for [repeats] auto-repeat events after the long-press timeout, then releases it. */
    fun hold(keyCode: Int, repeats: Int) {
        val down = now
        send(keyCode, ACTION_DOWN)
        now += 550
        send(keyCode, ACTION_DOWN, repeat = 1, downTime = down) // long press
        repeat(repeats) { i ->
            now += 160
            send(keyCode, ACTION_DOWN, repeat = i + 2, downTime = down)
        }
        now += 50
        send(keyCode, ACTION_UP, downTime = down)
        now += 400
    }

    /** Detents [gapMs] apart (DOWN only, as rotation acts on DOWN). */
    fun spin(keyCode: Int, detents: Int, gapMs: Long) {
        repeat(detents) {
            send(keyCode, ACTION_DOWN)
            now += gapMs
        }
        now += 400
    }
}
