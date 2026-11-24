package dev.namdev.rotary.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.namdev.hmi.theme.HmiTheme
import dev.namdev.hmi.theme.HmiThemeMode
import dev.namdev.rotary.compose.RotaryHost
import dev.namdev.rotary.compose.rememberRotaryInputInjector
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

@Composable
fun SampleApp(
    state: SampleState = remember { SampleState() },
    themeMode: HmiThemeMode = HmiThemeMode.FollowSystem,
    focusOnStart: Boolean = true,
    messageMs: Long = MESSAGE_MS,
) {
    val injector = rememberRotaryInputInjector()

    BackHandler(enabled = state.section != Section.Media) { state.back() }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(messageMs)
            state.dismissMessage()
        }
    }

    HmiTheme(themeMode) {
        RotaryHost(
            defaultArea = AREA_RAIL,
            focusOnStart = focusOnStart,
            interceptedKeys = SampleState.InterceptedKeys,
            keyMapper = SampleState::keyFor,
            injector = injector,
            onInput = state::onInput,
            onAction = state::onAction,
        ) {
            HomeScreen(state, injector)
        }
    }
}

private const val MESSAGE_MS = 3_000L
