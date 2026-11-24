package dev.namdev.rotary.compose

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RotaryClickableTest {

    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0

    private fun show() {
        compose.setContent {
            val interaction = remember { MutableInteractionSource() }
            Box(Modifier.size(80.dp).testTag("item").rotaryClickable(interaction) { clicks++ })
        }
    }

    @Test
    fun `is a button that reacts to taps`() {
        show()

        compose.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `clicks on DPAD_CENTER release when focused`() {
        show()
        val item = compose.onNodeWithTag("item")
        item.requestFocus()

        item.performKeyPress(KeyEvent(android.view.KeyEvent(ACTION_DOWN, KEYCODE_DPAD_CENTER)))
        assertEquals(0, clicks)
        item.performKeyPress(KeyEvent(android.view.KeyEvent(ACTION_UP, KEYCODE_DPAD_CENTER)))

        assertEquals(1, clicks)
    }
}
