package dev.namdev.rotary.sample

import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.namdev.hmi.components.HmiBannerLevel
import dev.namdev.rotary.compose.DefaultInterceptedKeys
import dev.namdev.rotary.compose.hardKeyOf
import dev.namdev.rotary.input.GestureType
import dev.namdev.rotary.input.HardKey
import dev.namdev.rotary.input.HmiAction
import dev.namdev.rotary.input.RoutedGesture

enum class Section(val label: String, val icon: ImageVector, val items: List<Pair<String, String>>) {
    Media(
        "Media",
        Icons.Filled.PlayArrow,
        listOf(
            "Morning commute" to "18 tracks · 52 min",
            "Road trip classics" to "40 tracks · 2 h 10 min",
            "Focus: instrumental" to "25 tracks · 1 h 30 min",
            "Tech news podcast" to "Episode 214",
            "Late night jazz" to "22 tracks · 1 h 45 min",
            "Kids' sing-along" to "30 tracks · 58 min",
        ),
    ),
    Navigation(
        "Navigation",
        Icons.Filled.Place,
        listOf(
            "Home" to "16 min · 8.7 km",
            "Office" to "22 min · 14 km",
            "Riverside Charging Hub" to "7 min · 3.2 km",
            "Central Station" to "12 min · 6.4 km",
            "Airport Terminal 2" to "35 min · 28 km",
        ),
    ),
    Phone(
        "Phone",
        Icons.Filled.Call,
        listOf(
            "Home" to "Mobile",
            "Roadside assistance" to "24 h service",
            "Service centre" to "Work",
            "Voicemail" to "2 new messages",
        ),
    ),
    Settings(
        "Settings",
        Icons.Filled.Settings,
        listOf(
            "Display" to "Brightness, day/night",
            "Sound" to "Balance, equaliser",
            "Driver profiles" to "2 profiles",
            "Connectivity" to "Bluetooth, Wi-Fi",
            "About" to "Software version",
        ),
    ),
}

private val tracks = listOf(
    "Open Road" to "The Commuters",
    "Northern Lights" to "Aurora Trio",
    "City at Dawn" to "Metro Sound",
    "Coastline" to "Blue Harbour",
)

/** The demo app's state: what is shown, and what the controller has done. No Android views, so it is easy to test. */
class SampleState {
    var section by mutableStateOf(Section.Media)
        private set
    var trackIndex by mutableIntStateOf(0)
        private set
    var positionSeconds by mutableIntStateOf(0)
        private set
    var playing by mutableStateOf(true)
        private set
    var volume by mutableIntStateOf(6)
        private set
    var muted by mutableStateOf(false)
        private set
    var message by mutableStateOf<Pair<String, HmiBannerLevel>?>(null)
        private set

    /** Most recent first. */
    val log = mutableStateListOf<String>()

    val track: Pair<String, String> get() = tracks[trackIndex]
    val durationSeconds: Int get() = TRACK_SECONDS

    fun open(section: Section) {
        this.section = section
    }

    fun openItem(title: String) {
        if (section == Section.Media) {
            trackIndex = (trackIndex + 1) % tracks.size
            positionSeconds = 0
            playing = true
            message = "Playing \"$title\"" to HmiBannerLevel.Info
        } else {
            message = "Opening \"$title\"" to HmiBannerLevel.Info
        }
    }

    /** Back: return to Media, the home section. Returns false at home, so the system can handle it. */
    fun back(): Boolean = (section != Section.Media).also { if (it) section = Section.Media }

    fun dismissMessage() {
        message = null
    }

    fun onAction(action: HmiAction) {
        when (action) {
            HmiAction.NextTrack -> changeTrack(+1)
            HmiAction.PreviousTrack -> changeTrack(-1)
            HmiAction.SeekForward -> positionSeconds = (positionSeconds + SEEK_SECONDS).coerceAtMost(TRACK_SECONDS)
            HmiAction.SeekBackward -> positionSeconds = (positionSeconds - SEEK_SECONDS).coerceAtLeast(0)
            HmiAction.PlayPause -> playing = !playing
            is HmiAction.Volume -> {
                volume = (volume + action.delta).coerceIn(0, MAX_VOLUME)
                muted = false
            }
            HmiAction.Mute -> muted = !muted
            HmiAction.Back -> back()
            // Select, rotation and nudges are handled by focus; the rest just show a message.
            else -> messages[action]?.let { message = it to HmiBannerLevel.Info }
        }
    }

    fun onInput(record: RoutedGesture) {
        val gesture = when (record.gesture.type) {
            GestureType.Press -> "tap"
            GestureType.LongPress -> "hold"
            GestureType.Repeat -> "repeat ${record.gesture.repeat}"
        }
        val action = record.action?.let(::describe) ?: "not bound"
        log.add(0, "${record.gesture.key.label}  ·  $gesture  →  $action")
        while (log.size > LOG_SIZE) log.removeAt(log.lastIndex)
    }

    private fun changeTrack(delta: Int) {
        trackIndex = (trackIndex + delta).mod(tracks.size)
        positionSeconds = 0
    }

    companion object {
        const val TRACK_SECONDS = 240
        const val SEEK_SECONDS = 10
        const val MAX_VOLUME = 10
        const val LOG_SIZE = 6

        /** The sample also takes volume keys, to show cabin volume on screen. */
        val InterceptedKeys: Set<HardKey> =
            DefaultInterceptedKeys + setOf(HardKey.VolumeUp, HardKey.VolumeDown, HardKey.Mute)

        private val messages: Map<HmiAction, String> = mapOf(
            HmiAction.VoiceAssistant to "Listening… say a destination or a song",
            HmiAction.AnswerCall to "Call answered",
            HmiAction.Redial to "Redialling the last number",
            HmiAction.EndCall to "Call ended",
            HmiAction.LongSelect to "Long press: item options",
        )

        /** Laptop-keyboard aliases, so the emulator can be driven without a rotary controller. */
        fun keyFor(keyCode: Int): HardKey? = hardKeyOf(keyCode) ?: when (keyCode) {
            KeyEvent.KEYCODE_Q -> HardKey.RotateCounterClockwise
            KeyEvent.KEYCODE_E -> HardKey.RotateClockwise
            KeyEvent.KEYCODE_N -> HardKey.MediaNext
            KeyEvent.KEYCODE_P -> HardKey.MediaPrevious
            KeyEvent.KEYCODE_SPACE -> HardKey.PlayPause
            KeyEvent.KEYCODE_MINUS -> HardKey.VolumeDown
            KeyEvent.KEYCODE_EQUALS -> HardKey.VolumeUp
            KeyEvent.KEYCODE_V -> HardKey.Voice
            KeyEvent.KEYCODE_C -> HardKey.Call
            else -> null
        }

        fun describe(action: HmiAction): String = when (action) {
            is HmiAction.Rotate -> "focus ${if (action.steps > 0) "+" else ""}${action.steps}"
            is HmiAction.Nudge -> "nudge ${action.direction.name.lowercase()}"
            is HmiAction.Volume -> "volume ${if (action.delta > 0) "+" else ""}${action.delta}"
            else -> action.toString().replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()
        }
    }
}

val HardKey.label: String
    get() = when (this) {
        HardKey.RotateClockwise -> "Knob CW"
        HardKey.RotateCounterClockwise -> "Knob CCW"
        HardKey.Up -> "D-pad ↑"
        HardKey.Down -> "D-pad ↓"
        HardKey.Left -> "D-pad ←"
        HardKey.Right -> "D-pad →"
        else -> name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    }
