package dev.namdev.rotary.sample

import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_E
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_NAVIGATE_NEXT
import android.view.KeyEvent.KEYCODE_VOICE_ASSIST
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import dev.namdev.hmi.theme.HmiThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = SAMPLE_QUALIFIERS)
class SampleFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = SampleState()
    private val keys = Keys(compose)

    private fun launch() {
        compose.setContent { SampleApp(state, HmiThemeMode.Night, messageMs = Long.MAX_VALUE) }
        compose.waitForIdle()
    }

    private fun focused(tag: String) = compose.onNodeWithTag(tag).assertIsFocused()

    @Test
    fun `turn and select in the rail opens a section`() {
        launch()
        focused("rail-Media")

        keys.tap(KEYCODE_NAVIGATE_NEXT)
        focused("rail-Navigation")
        keys.tap(KEYCODE_DPAD_CENTER)

        assertEquals(Section.Navigation, state.section)
        compose.onNodeWithText("Riverside Charging Hub").assertExists()
    }

    @Test
    fun `nudge into the list and select an item`() {
        launch()

        keys.tap(KEYCODE_DPAD_RIGHT)
        focused("item-Morning commute")
        keys.tap(KEYCODE_NAVIGATE_NEXT)
        keys.tap(KEYCODE_DPAD_CENTER)

        compose.onNodeWithText("Playing \"Road trip classics\"").assertExists()
    }

    @Test
    fun `steering wheel - tap next skips, hold next seeks`() {
        launch()

        keys.tap(KEYCODE_MEDIA_NEXT)
        assertEquals(1, state.trackIndex)

        keys.hold(KEYCODE_MEDIA_NEXT, repeats = 2) // long press + 2 repeats = 3 seeks of 10 s
        compose.onNodeWithTag("now-playing").assertTextContains("0:30", substring = true)
        assertEquals("Media Next  ·  repeat 2  →  seek forward", state.log.first())
    }

    @Test
    fun `voice key, volume key and the emulator keyboard alias`() {
        launch()

        keys.tap(KEYCODE_VOICE_ASSIST)
        keys.tap(KEYCODE_VOLUME_UP)
        keys.tap(KEYCODE_E) // laptop keyboard: E turns the knob clockwise

        compose.onNodeWithText("Listening… say a destination or a song").assertExists()
        assertEquals(7, state.volume)
        focused("rail-Navigation")
    }

    @Test
    fun `nudge down reaches the player, back returns home`() {
        launch()
        keys.tap(KEYCODE_NAVIGATE_NEXT)
        keys.tap(KEYCODE_DPAD_CENTER)

        keys.tap(KEYCODE_DPAD_RIGHT)
        keys.tap(KEYCODE_DPAD_DOWN)
        focused("player-Previous")

        // BACK reaches BackHandler through the Activity, not Compose, so use the on-screen Back key here.
        compose.onNodeWithTag("key-Back").performTouchInput { down(center); up() }
        assertEquals(Section.Media, state.section)
    }

    @Test
    fun `the on-screen controller works by touch and focus survives the touch`() {
        launch()

        // Two separate taps, half a second apart (Robolectric's clock only moves when told to);
        // taps in quick succession would accelerate.
        compose.onNodeWithTag("key-Knob →").performTouchInput { down(center); up() }
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        compose.onNodeWithTag("key-Knob →").performTouchInput { down(center); up() }

        focused("rail-Phone")
    }
}
