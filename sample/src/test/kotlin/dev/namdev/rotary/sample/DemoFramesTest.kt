package dev.namdev.rotary.sample

import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_NAVIGATE_NEXT
import android.view.KeyEvent.KEYCODE_VOICE_ASSIST
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.namdev.hmi.theme.HmiThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A landscape car display. */
const val SAMPLE_QUALIFIERS = "w1280dp-h720dp-land-mdpi"

/**
 * Drives the sample with real key events and renders one frame per step, on the JVM.
 * `./gradlew :sample:recordRoborazziDebug` writes the frames; `scripts/make_demo_gif.py` turns them into
 * docs/demo.gif. A plain test run checks that the whole scripted drive works.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = SAMPLE_QUALIFIERS)
class DemoFramesTest {

    @get:Rule
    val compose = createComposeRule()

    private val keys = Keys(compose)
    private var frame = 0

    private fun capture() {
        frame++
        compose.onRoot().captureRoboImage("build/demo-frames/frame-%02d.png".format(frame))
    }

    @Test
    fun drive() {
        compose.setContent { SampleApp(SampleState(), HmiThemeMode.Night, messageMs = Long.MAX_VALUE) }
        compose.waitForIdle()
        capture() // focus starts on Media

        keys.tap(KEYCODE_NAVIGATE_NEXT); capture() // turn: Navigation
        keys.tap(KEYCODE_DPAD_CENTER); capture() // select: the list changes
        keys.tap(KEYCODE_DPAD_RIGHT); capture() // nudge right: into the list
        keys.tap(KEYCODE_NAVIGATE_NEXT); capture()
        keys.tap(KEYCODE_NAVIGATE_NEXT); capture()
        keys.spin(KEYCODE_NAVIGATE_NEXT, detents = 2, gapMs = 20); capture() // fast turn accelerates, stops at the end
        keys.tap(KEYCODE_DPAD_DOWN); capture() // nudge down: player
        keys.tap(KEYCODE_NAVIGATE_NEXT); capture()
        keys.tap(KEYCODE_MEDIA_NEXT); capture() // steering wheel: next track
        keys.hold(KEYCODE_MEDIA_NEXT, repeats = 2); capture() // hold: seek
        keys.tap(KEYCODE_VOICE_ASSIST); capture() // voice key
        keys.tap(KEYCODE_DPAD_LEFT); capture() // nudge left: back to the rail, on Navigation
        compose.onRoot().captureRoboImage("../docs/home.png")
    }
}
