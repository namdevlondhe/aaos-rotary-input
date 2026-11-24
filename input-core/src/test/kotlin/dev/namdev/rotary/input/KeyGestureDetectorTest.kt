package dev.namdev.rotary.input

import dev.namdev.rotary.input.GestureType.LongPress
import dev.namdev.rotary.input.GestureType.Press
import dev.namdev.rotary.input.GestureType.Repeat
import dev.namdev.rotary.input.HardKey.MediaNext
import dev.namdev.rotary.input.HardKey.VolumeUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyGestureDetectorTest {

    private val detector = KeyGestureDetector(longPressMs = 500, repeatIntervalMs = 100)

    private fun down(key: HardKey, at: Long, repeat: Int = 0) = detector.onKey(KeyInput(key, true, at, repeat))
    private fun up(key: HardKey, at: Long, canceled: Boolean = false) =
        detector.onKey(KeyInput(key, false, at, canceled = canceled))

    @Test
    fun `a quick tap is a press, reported on release`() {
        assertEquals(emptyList<KeyGesture>(), down(MediaNext, 1_000))
        assertEquals(listOf(KeyGesture(MediaNext, Press)), up(MediaNext, 1_120))
    }

    @Test
    fun `a key without auto-repeat becomes a long press on release`() {
        down(MediaNext, 1_000)

        assertEquals(listOf(KeyGesture(MediaNext, LongPress)), up(MediaNext, 1_700))
    }

    @Test
    fun `auto-repeated downs fire the long press while held, then repeats, and nothing on release`() {
        down(MediaNext, 0)

        assertEquals(emptyList<KeyGesture>(), down(MediaNext, 400, repeat = 1))
        assertEquals(listOf(KeyGesture(MediaNext, LongPress)), down(MediaNext, 520, repeat = 2))
        assertEquals(emptyList<KeyGesture>(), down(MediaNext, 570, repeat = 3))
        assertEquals(listOf(KeyGesture(MediaNext, Repeat, 1)), down(MediaNext, 620, repeat = 4))
        assertEquals(listOf(KeyGesture(MediaNext, Repeat, 2)), down(MediaNext, 720, repeat = 5))
        assertEquals(emptyList<KeyGesture>(), up(MediaNext, 760))
    }

    @Test
    fun `ticks drive long press and repeat for keys that only report down and up`() {
        down(VolumeUp, 0)
        assertEquals(500L, detector.nextDeadline())

        assertEquals(emptyList<KeyGesture>(), detector.onTick(499))
        assertEquals(listOf(KeyGesture(VolumeUp, LongPress)), detector.onTick(500))
        assertEquals(600L, detector.nextDeadline())
        assertEquals(listOf(KeyGesture(VolumeUp, Repeat, 1)), detector.onTick(600))

        up(VolumeUp, 650)
        assertNull(detector.nextDeadline())
    }

    @Test
    fun `a cancelled press fires nothing`() {
        down(MediaNext, 0)

        assertEquals(emptyList<KeyGesture>(), up(MediaNext, 100, canceled = true))
    }

    @Test
    fun `a release without a press is ignored`() {
        assertEquals(emptyList<KeyGesture>(), up(MediaNext, 100))
    }

    @Test
    fun `keys are tracked independently`() {
        down(VolumeUp, 0)
        down(MediaNext, 100)

        assertEquals(listOf(KeyGesture(MediaNext, Press)), up(MediaNext, 200))
        assertEquals(listOf(KeyGesture(VolumeUp, LongPress)), detector.onTick(500))
        assertEquals(emptyList<KeyGesture>(), up(VolumeUp, 550))
    }
}
