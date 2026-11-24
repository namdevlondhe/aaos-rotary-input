package dev.namdev.rotary.sample

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.namdev.hmi.focus.hmiFocusRing
import dev.namdev.hmi.theme.HmiTheme
import dev.namdev.rotary.compose.RotaryInputInjector
import dev.namdev.rotary.compose.rotaryClickable
import dev.namdev.rotary.input.HardKey

/**
 * A focusable tile built from the aaos-hmi-components tokens and focus ring. It uses [rotaryClickable] so it
 * keeps focus while the on-screen controller is being touched.
 */
@Composable
fun RotaryTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
) {
    val colors = HmiTheme.colors
    val dimens = HmiTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hmiFocusRing(interaction)
            .heightIn(min = dimens.touchTarget)
            .clip(RoundedCornerShape(dimens.corner))
            .background(if (selected) colors.primary else colors.surface)
            .indication(interaction, ripple())
            .rotaryClickable(interaction, onClick = onClick)
            .padding(horizontal = dimens.spacingM, vertical = dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingM, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(dimens.iconSize))
        }
        Column(Modifier.weight(1f, fill = icon != null)) {
            Text(
                title,
                style = HmiTheme.typography.label,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (icon == null) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            if (supporting != null) {
                Text(
                    supporting,
                    style = HmiTheme.typography.caption,
                    color = if (selected) content else colors.onSurfaceMuted,
                )
            }
        }
    }
}

/**
 * Stand-in for a rotary controller and steering-wheel keys when the device has none. Not focusable: it feeds
 * the [RotaryInputInjector], the same pipeline hardware keys go through. Holding a key long-presses it.
 */
@Composable
fun OnScreenController(injector: RotaryInputInjector, state: SampleState) {
    val view = LocalView.current
    // Select and Back are not intercepted by the host (Compose clicks on DPAD_CENTER), so send them as key events.
    val select = {
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        Unit
    }
    Panel("On-screen controller") {
        KeyRow {
            VirtualKey("Knob ←", HardKey.RotateCounterClockwise, injector)
            VirtualButton("Select", onPress = select)
            VirtualKey("Knob →", HardKey.RotateClockwise, injector)
        }
        KeyRow {
            VirtualKey("←", HardKey.Left, injector)
            VirtualKey("↑", HardKey.Up, injector)
            VirtualKey("↓", HardKey.Down, injector)
            VirtualKey("→", HardKey.Right, injector)
            VirtualButton("Back", onPress = { state.back() })
        }
        Text("Steering wheel", style = HmiTheme.typography.caption, color = HmiTheme.colors.onSurfaceMuted)
        KeyRow {
            VirtualKey("⏮", HardKey.MediaPrevious, injector)
            VirtualKey("⏯", HardKey.PlayPause, injector)
            VirtualKey("⏭", HardKey.MediaNext, injector)
            VirtualKey("Vol−", HardKey.VolumeDown, injector)
            VirtualKey("Vol+", HardKey.VolumeUp, injector)
        }
        KeyRow {
            VirtualKey("Voice", HardKey.Voice, injector)
            VirtualKey("Call", HardKey.Call, injector)
            VirtualKey("End", HardKey.EndCall, injector)
        }
    }
}

@Composable
fun InputLog(state: SampleState, modifier: Modifier = Modifier) {
    Panel("Input log", modifier) {
        if (state.log.isEmpty()) {
            Text(
                "Turn the knob or press a key. Emulator keyboard: arrows nudge, Q/E turn, Enter selects, " +
                    "N/P media, Space play, V voice.",
                style = HmiTheme.typography.caption,
                color = HmiTheme.colors.onSurfaceMuted,
            )
        }
        state.log.forEachIndexed { index, line ->
            Text(
                line,
                style = HmiTheme.typography.caption,
                color = if (index == 0) HmiTheme.colors.onSurface else HmiTheme.colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("log-$index"),
            )
        }
    }
}

@Composable
private fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dimens = HmiTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.corner + dimens.spacingS))
            .background(HmiTheme.colors.surface)
            .padding(dimens.spacingM),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingS),
    ) {
        Text(title, style = HmiTheme.typography.label, color = HmiTheme.colors.onSurfaceMuted)
        content()
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(HmiTheme.dimens.spacingXs)) { content() }
}

@Composable
private fun VirtualKey(label: String, key: HardKey, injector: RotaryInputInjector) {
    VirtualButton(label, onPress = { injector.press(key) }, onRelease = { injector.release(key) })
}

/** Pressed with a finger, never focused. */
@Composable
private fun VirtualButton(label: String, onPress: () -> Unit, onRelease: () -> Unit = {}) {
    val dimens = HmiTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = dimens.touchTarget * 0.8f)
            .clip(RoundedCornerShape(dimens.corner))
            .background(HmiTheme.colors.surfaceRaised)
            .indication(interaction, ripple())
            .pointerInput(onPress, onRelease) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interaction.emit(press)
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                        interaction.emit(PressInteraction.Release(press))
                    },
                )
            }
            .testTag("key-$label")
            .padding(horizontal = dimens.spacingM, vertical = dimens.spacingS),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = HmiTheme.typography.label, color = HmiTheme.colors.onSurface)
    }
}
