package dev.namdev.rotary.compose

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import dev.namdev.rotary.input.GestureType
import dev.namdev.rotary.input.HardKey
import dev.namdev.rotary.input.HmiAction
import dev.namdev.rotary.input.InputRouter
import dev.namdev.rotary.input.KeyGesture
import dev.namdev.rotary.input.KeyInput
import dev.namdev.rotary.input.RoutedGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A recognised gesture and the action it triggered (null when unbound); for logs and diagnostics. */
typealias InputRecord = RoutedGesture

/**
 * Root of a rotary-navigable screen.
 *
 * Intercepts controller and steering-wheel keys before the focused element sees them, runs them through the
 * [InputRouter] (gestures, bindings, acceleration) and then:
 * - [HmiAction.Rotate] and [HmiAction.Nudge] move focus via the [RotaryNavigator];
 * - every other action goes to [onAction].
 *
 * Rotary encoders that report scroll motion (`onRotaryScrollEvent`) are handled as rotation too.
 * Keys outside [interceptedKeys] are left alone, so Compose still clicks the focused item on DPAD_CENTER
 * and the system still handles BACK and volume.
 *
 * Like AAOS, focus is not shown while the user is touching the screen. Until then focus is "parked" on the host
 * itself (AAOS's FocusParkingView), so the first controller key still arrives; that first rotation or nudge
 * switches Compose to keyboard input mode, where clickables become focusable, and reveals focus in [defaultArea].
 * Set [focusOnStart] to show focus immediately instead.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RotaryHost(
    modifier: Modifier = Modifier,
    navigator: RotaryNavigator = rememberRotaryNavigator(),
    router: InputRouter = remember { InputRouter() },
    defaultArea: String? = null,
    focusOnStart: Boolean = false,
    interceptedKeys: Set<HardKey> = DefaultInterceptedKeys,
    keyMapper: (keyCode: Int) -> HardKey? = ::hardKeyOf,
    clock: () -> Long = SystemClock::uptimeMillis,
    injector: RotaryInputInjector? = null,
    onInput: (InputRecord) -> Unit = {},
    onAction: (HmiAction) -> Unit = {},
    content: @Composable () -> Unit,
) {
    navigator.focusManager = LocalFocusManager.current
    navigator.defaultAreaId = defaultArea
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnInput by rememberUpdatedState(onInput)
    val scope = rememberCoroutineScope()
    val inputModeManager = LocalInputModeManager.current
    val enterKeyboardMode = { inputModeManager.requestInputMode(InputMode.Keyboard); Unit }
    val dispatcher = remember(navigator, router) {
        InputDispatcher(
            navigator = navigator,
            router = router,
            scope = scope,
            clock = clock,
            callbacks = HostCallbacks(enterKeyboardMode, { currentOnInput(it) }, { currentOnAction(it) }),
        )
    }
    val parking = remember { FocusRequester() }
    injector?.target = { key, pressed -> dispatcher.onKey(KeyInput(key, pressed, clock())) }

    LaunchedEffect(navigator) {
        if (focusOnStart && navigator.hasAreas) {
            enterKeyboardMode()
            withFrameNanos { } // let clickables become focusable outside touch mode first
            navigator.focusDefaultArea()
        } else {
            parking.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val key = keyMapper(native.keyCode)
                val isKeyAction = native.action == KeyEvent.ACTION_DOWN || native.action == KeyEvent.ACTION_UP
                if (key == null || key !in interceptedKeys || !isKeyAction) {
                    false
                } else {
                    dispatcher.onKey(
                        KeyInput(
                            key = key,
                            pressed = native.action == KeyEvent.ACTION_DOWN,
                            timeMs = native.eventTime,
                            repeatCount = native.repeatCount,
                            canceled = native.isCanceled,
                        ),
                    )
                    true
                }
            }
            .onPreRotaryScrollEvent { event ->
                val clockwise = event.verticalScrollPixels > 0
                event.verticalScrollPixels != 0f && dispatcher.onRotary(clockwise, event.uptimeMillis)
            }
            // Focus parking: holds focus while nothing is shown focused, so key events keep arriving. When a touch
            // clears focus (clickables are not focusable in touch mode), park again on the next frame.
            .onFocusChanged { state ->
                if (!state.hasFocus) {
                    scope.launch {
                        withFrameNanos { }
                        parking.requestFocus()
                    }
                }
            }
            .focusRequester(parking)
            .focusable(),
    ) {
        CompositionLocalProvider(LocalRotaryNavigator provides navigator, content = content)
    }
}

private class HostCallbacks(
    val enterKeyboardMode: () -> Unit,
    val onInput: (InputRecord) -> Unit,
    val onAction: (HmiAction) -> Unit,
)

/** Routes inputs and schedules the long-press/repeat timer for keys that do not auto-repeat. */
private class InputDispatcher(
    private val navigator: RotaryNavigator,
    private val router: InputRouter,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val callbacks: HostCallbacks,
) {
    private var timer: Job? = null

    fun onKey(input: KeyInput) {
        handle(router.route(input))
        scheduleTimer()
    }

    fun onRotary(clockwise: Boolean, timeMs: Long): Boolean {
        val key = if (clockwise) HardKey.RotateClockwise else HardKey.RotateCounterClockwise
        val action = router.onRotary(clockwise, detents = 1, timeMs = timeMs)
        handle(listOf(RoutedGesture(KeyGesture(key, GestureType.Press), action)))
        return true
    }

    private fun handle(routed: List<RoutedGesture>) = routed.forEach { record ->
        callbacks.onInput(record)
        when (val action = record.action) {
            null -> Unit
            is HmiAction.Rotate -> moveFocus { navigator.rotate(action.steps) }
            is HmiAction.Nudge -> moveFocus { navigator.nudge(action.direction) }
            else -> callbacks.onAction(action)
        }
    }

    private fun moveFocus(move: () -> Unit) {
        callbacks.enterKeyboardMode()
        if (navigator.hasAreas && navigator.focusedAreaId == null) {
            // While focus is hidden, a controller input only reveals it: in the area the user was last in, or the
            // default area (as in AAOS). Wait one frame so leaving touch mode has made clickables focusable.
            scope.launch {
                withFrameNanos { }
                navigator.revealFocus()
            }
        } else {
            move()
        }
    }

    private fun scheduleTimer() {
        timer?.cancel()
        val deadline = router.nextDeadline() ?: return
        timer = scope.launch {
            delay((deadline - clock()).coerceAtLeast(0))
            handle(router.routeTick(clock()))
            scheduleTimer()
        }
    }
}

/**
 * Feeds the same pipeline as hardware keys from code: an on-screen controller, a touchpad, automated tests.
 * Pass it to [RotaryHost] and call [press]/[release] (hold) or [tap].
 */
@Stable
class RotaryInputInjector {
    internal var target: ((HardKey, Boolean) -> Unit)? = null

    fun press(key: HardKey) {
        target?.invoke(key, true)
    }

    fun release(key: HardKey) {
        target?.invoke(key, false)
    }

    fun tap(key: HardKey) {
        press(key)
        release(key)
    }
}

@Composable
fun rememberRotaryInputInjector(): RotaryInputInjector = remember { RotaryInputInjector() }
