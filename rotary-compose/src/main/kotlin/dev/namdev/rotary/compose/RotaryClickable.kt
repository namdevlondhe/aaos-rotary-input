package dev.namdev.rotary.compose

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Like `clickable`, but the element stays focusable in touch mode.
 *
 * Compose's `clickable` is focusable only outside touch mode, so touching the screen clears its focus (AAOS hides
 * rotary focus on touch in the same way). Use this instead for controls that must keep their focus position while
 * the screen is touched, e.g. when an on-screen controller or a touchpad drives a rotary UI.
 * Clicks come from a tap, DPAD_CENTER or ENTER; press interactions are emitted for ripples.
 */
@Composable
fun Modifier.rotaryClickable(
    interactionSource: MutableInteractionSource,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    return this
        .semantics {
            this.role = role
            onClick {
                currentOnClick()
                true
            }
        }
        .onKeyEvent { event ->
            val isSelectKey = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
            if (isSelectKey && event.type == KeyEventType.KeyUp) currentOnClick()
            isSelectKey
        }
        .pointerInput(interactionSource) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val released = tryAwaitRelease()
                    val end = if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                    interactionSource.emit(end)
                },
                onTap = { currentOnClick() },
            )
        }
        .focusable(interactionSource = interactionSource)
}
