package dev.namdev.rotary.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RotaryAcceleratorTest {

    private val accelerator = RotaryAccelerator(tripleBelowMs = 25, doubleBelowMs = 50)

    @Test
    fun `slow turns move one item per detent`() {
        assertEquals(1, accelerator.steps(clockwise = true, detents = 1, timeMs = 0))
        assertEquals(1, accelerator.steps(clockwise = true, detents = 1, timeMs = 200))
        assertEquals(-1, accelerator.steps(clockwise = false, detents = 1, timeMs = 400))
    }

    @Test
    fun `fast turns accelerate to two and three items per detent`() {
        accelerator.steps(clockwise = true, detents = 1, timeMs = 0)

        assertEquals(2, accelerator.steps(clockwise = true, detents = 1, timeMs = 40))
        assertEquals(3, accelerator.steps(clockwise = true, detents = 1, timeMs = 60))
        assertEquals(6, accelerator.steps(clockwise = true, detents = 2, timeMs = 70))
    }

    @Test
    fun `changing direction starts again at one`() {
        accelerator.steps(clockwise = true, detents = 1, timeMs = 0)

        assertEquals(-1, accelerator.steps(clockwise = false, detents = 1, timeMs = 10))
        assertEquals(-3, accelerator.steps(clockwise = false, detents = 1, timeMs = 20))
    }
}

class AreaFinderTest {

    // A typical car home screen:  [rail] [content] [panel]
    //                                    [player ]
    private val rail = Bounds(0f, 0f, 200f, 700f)
    private val content = Bounds(220f, 0f, 900f, 500f)
    private val player = Bounds(220f, 520f, 900f, 700f)
    private val panel = Bounds(920f, 0f, 1280f, 700f)
    private val areas = mapOf("rail" to rail, "content" to content, "player" to player, "panel" to panel)

    private fun from(bounds: Bounds, direction: Direction) =
        AreaFinder.nearest(bounds, areas.filterValues { it != bounds }, direction)

    @Test
    fun `nudging moves to the adjacent area`() {
        assertEquals("content", from(rail, Direction.Right))
        assertEquals("player", from(content, Direction.Down))
        assertEquals("content", from(player, Direction.Up))
        assertEquals("panel", from(content, Direction.Right))
    }

    @Test
    fun `an area beside the current one is not above or below it`() {
        // The rail is taller than the player and its centre is higher, but it is to the left, not above.
        assertEquals("content", from(player, Direction.Up))
        assertEquals("rail", from(player, Direction.Left))
    }

    @Test
    fun `the better aligned of two equally close areas wins`() {
        // Content and player are both 20px left of the panel; content's centre is closer to the panel's.
        assertEquals("content", from(panel, Direction.Left))
        assertEquals("content", from(rail, Direction.Right))
    }

    @Test
    fun `nothing beyond the screen edge`() {
        assertNull(from(rail, Direction.Left))
        assertNull(from(content, Direction.Up))
    }
}
