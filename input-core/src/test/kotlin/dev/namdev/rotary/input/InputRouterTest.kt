package dev.namdev.rotary.input

import dev.namdev.rotary.input.HmiAction.Rotate
import org.junit.Assert.assertEquals
import org.junit.Test

class InputRouterTest {

    private val router = InputRouter(longPressMs = 500, repeatIntervalMs = 100)

    private fun tap(key: HardKey, at: Long): List<HmiAction> =
        router.onKey(KeyInput(key, true, at)) + router.onKey(KeyInput(key, false, at + 80))

    @Test
    fun `steering wheel media key - tap skips, hold seeks repeatedly`() {
        assertEquals(listOf(HmiAction.NextTrack), tap(HardKey.MediaNext, 0))

        router.onKey(KeyInput(HardKey.MediaNext, true, 1_000))
        assertEquals(listOf(HmiAction.SeekForward), router.onTick(1_500))
        assertEquals(listOf(HmiAction.SeekForward), router.onTick(1_600))
        assertEquals(emptyList<HmiAction>(), router.onKey(KeyInput(HardKey.MediaNext, false, 1_650)))
    }

    @Test
    fun `rotary detents act immediately on down, with acceleration`() {
        assertEquals(listOf(Rotate(1)), router.onKey(KeyInput(HardKey.RotateClockwise, true, 0)))
        assertEquals(emptyList<HmiAction>(), router.onKey(KeyInput(HardKey.RotateClockwise, false, 5)))
        assertEquals(listOf(Rotate(3)), router.onKey(KeyInput(HardKey.RotateClockwise, true, 15)))
        assertEquals(listOf(Rotate(-1)), router.onKey(KeyInput(HardKey.RotateCounterClockwise, true, 30)))
    }

    @Test
    fun `nudges, select and back`() {
        assertEquals(listOf(HmiAction.Nudge(Direction.Right)), tap(HardKey.Right, 0))
        assertEquals(listOf(HmiAction.Select), tap(HardKey.Center, 1_000))
        assertEquals(listOf(HmiAction.Back), tap(HardKey.Back, 2_000))
    }

    @Test
    fun `long press of call redials`() {
        router.onKey(KeyInput(HardKey.Call, true, 0))

        assertEquals(listOf(HmiAction.Redial), router.onKey(KeyInput(HardKey.Call, false, 800)))
    }

    @Test
    fun `bindings can be changed at runtime`() {
        router.bindings = KeyBindings.Default
            .with(HardKey.Voice, GestureType.LongPress, HmiAction.Mute)
            .without(HardKey.Voice, GestureType.Press)

        assertEquals(emptyList<HmiAction>(), tap(HardKey.Voice, 0))
        router.onKey(KeyInput(HardKey.Voice, true, 1_000))
        assertEquals(listOf(HmiAction.Mute), router.onTick(1_500))
    }
}
