package dev.namdev.rotary.compose

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_NAVIGATE_NEXT
import android.view.KeyEvent.KEYCODE_NAVIGATE_PREVIOUS
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import dev.namdev.rotary.input.GestureType
import dev.namdev.rotary.input.HardKey
import dev.namdev.rotary.input.HmiAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RotaryHostTest {

    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<HmiAction>()
    private val records = mutableListOf<InputRecord>()
    private var railClicks = 0
    private val injector = RotaryInputInjector()
    private lateinit var navigator: RotaryNavigator
    private lateinit var focusManager: FocusManager

    /** [rail: r0 r1 r2]  [content: c0 c1 c2 c3 c4] */
    private fun showScreen(focusOnStart: Boolean = true) {
        compose.setContent {
            navigator = rememberRotaryNavigator()
            focusManager = LocalFocusManager.current
            RotaryHost(
                navigator = navigator,
                injector = injector,
                defaultArea = "rail",
                focusOnStart = focusOnStart,
                onAction = { actions += it },
                onInput = { records += it },
            ) {
                Row {
                    Column(Modifier.focusArea("rail")) {
                        Item("r0") { railClicks++ }
                        Item("r1")
                        Item("r2")
                    }
                    Column(Modifier.focusArea("content")) {
                        (0..4).forEach { Item("c$it") }
                    }
                }
            }
        }
    }

    /** A focusable item; [onClick] makes it clickable instead (clickable is focusable by itself). */
    @Composable
    private fun Item(tag: String, onClick: (() -> Unit)? = null) {
        val interaction = if (onClick == null) Modifier.focusable() else Modifier.clickable(onClick = onClick)
        Box(Modifier.size(60.dp).testTag(tag).then(interaction))
    }

    private fun send(keyCode: Int, action: Int, timeMs: Long, repeat: Int = 0, downTimeMs: Long = timeMs) {
        val native = android.view.KeyEvent(downTimeMs, timeMs, action, keyCode, repeat)
        compose.onRoot().performKeyPress(KeyEvent(native))
        compose.waitForIdle()
    }

    private fun tap(keyCode: Int, timeMs: Long) {
        send(keyCode, ACTION_DOWN, timeMs)
        send(keyCode, ACTION_UP, timeMs + 60, downTimeMs = timeMs)
    }

    private fun assertFocused(tag: String) = compose.onNodeWithTag(tag).assertIsFocused()

    @Test
    fun `focus starts in the default area`() {
        showScreen()

        assertFocused("r0")
    }

    @Test
    fun `without focusOnStart, the first rotation focuses the default area`() {
        showScreen(focusOnStart = false)
        // Only the host's parking node holds focus; no item shows focus yet.
        compose.onAllNodes(isFocused() and hasTestTag()).assertCountEquals(0)

        tap(KEYCODE_NAVIGATE_NEXT, 1_000)

        assertFocused("r0")
    }

    @Test
    fun `rotation moves within the area and stops at its edge`() {
        showScreen()

        tap(KEYCODE_NAVIGATE_NEXT, 1_000)
        assertFocused("r1")
        tap(KEYCODE_NAVIGATE_NEXT, 2_000)
        assertFocused("r2")
        tap(KEYCODE_NAVIGATE_NEXT, 3_000) // the next item in traversal order is c0, in another area
        assertFocused("r2")
        tap(KEYCODE_NAVIGATE_PREVIOUS, 4_000)
        assertFocused("r1")
    }

    @Test
    fun `nudging changes area and restores the item last focused there`() {
        showScreen()

        tap(KEYCODE_DPAD_RIGHT, 1_000)
        assertFocused("c0")
        tap(KEYCODE_NAVIGATE_NEXT, 2_000)
        tap(KEYCODE_NAVIGATE_NEXT, 3_000)
        assertFocused("c2")

        tap(KEYCODE_DPAD_LEFT, 4_000)
        assertFocused("r0")
        tap(KEYCODE_DPAD_RIGHT, 5_000)
        assertFocused("c2")
    }

    @Test
    fun `fast rotation accelerates`() {
        showScreen()
        tap(KEYCODE_DPAD_RIGHT, 1_000)

        send(KEYCODE_NAVIGATE_NEXT, ACTION_DOWN, 2_000) // 1 step
        send(KEYCODE_NAVIGATE_NEXT, ACTION_DOWN, 2_040) // 40 ms later: 2 steps

        assertFocused("c3")
    }

    @Test
    fun `steering wheel media key - tap and hold`() {
        showScreen()

        tap(KEYCODE_MEDIA_NEXT, 1_000)
        send(KEYCODE_MEDIA_NEXT, ACTION_DOWN, 2_000)
        send(KEYCODE_MEDIA_NEXT, ACTION_DOWN, 2_600, repeat = 1, downTimeMs = 2_000) // auto-repeat while held
        send(KEYCODE_MEDIA_NEXT, ACTION_UP, 2_650, downTimeMs = 2_000)

        assertEquals(listOf(HmiAction.NextTrack, HmiAction.SeekForward), actions)
        assertEquals(
            listOf(GestureType.Press, GestureType.LongPress),
            records.filter { it.gesture.key == HardKey.MediaNext }.map { it.gesture.type },
        )
    }

    @Test
    fun `select is left to Compose, so the focused item is clicked`() {
        showScreen()

        tap(KEYCODE_DPAD_CENTER, 1_000)

        assertEquals(1, railClicks)
        assertEquals(emptyList<HmiAction>(), actions)
    }

    @Test
    fun `keys that are not intercepted are not handled`() {
        showScreen()

        tap(KEYCODE_VOLUME_UP, 1_000)

        assertEquals(emptyList<HmiAction>(), actions)
        assertEquals(emptyList<InputRecord>(), records)
    }

    private fun hasTestTag() = SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)

    @Test
    fun `after a touch clears focus, the next rotation reveals the area the user was in`() {
        showScreen()
        tap(KEYCODE_DPAD_RIGHT, 1_000)
        assertEquals("content", navigator.focusedAreaId)

        compose.runOnIdle { focusManager.clearFocus(force = true) } // what entering touch mode does to clickables
        compose.waitForIdle()
        assertEquals(null, navigator.focusedAreaId)

        tap(KEYCODE_NAVIGATE_NEXT, 2_000) // reaches the host because it re-parked focus
        assertEquals("content", navigator.focusedAreaId)
    }

    @Test
    fun `injected input goes through the same pipeline as hardware keys`() {
        showScreen()

        compose.runOnIdle { injector.tap(HardKey.RotateClockwise) }
        compose.runOnIdle { injector.tap(HardKey.MediaNext) }

        assertFocused("r1")
        assertEquals(listOf(HmiAction.NextTrack), actions)
    }
}
