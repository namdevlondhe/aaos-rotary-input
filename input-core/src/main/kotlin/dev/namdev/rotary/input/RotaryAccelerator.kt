package dev.namdev.rotary.input

/**
 * Rotary acceleration, as AAOS's RotaryService does it: detents that follow each other quickly in the same
 * direction move focus by 2 or 3 items each, so a long list can be crossed with a flick of the knob while slow,
 * deliberate turns stay precise. Changing direction always starts again at 1x.
 */
class RotaryAccelerator(
    private val tripleBelowMs: Long = DEFAULT_TRIPLE_BELOW_MS,
    private val doubleBelowMs: Long = DEFAULT_DOUBLE_BELOW_MS,
) {
    private var lastTimeMs: Long? = null
    private var lastClockwise: Boolean? = null

    init {
        require(tripleBelowMs in 1..doubleBelowMs) { "tripleBelowMs must be positive and not above doubleBelowMs" }
    }

    /** Signed number of focus steps for [detents] clicks: positive clockwise, negative counter-clockwise. */
    fun steps(clockwise: Boolean, detents: Int, timeMs: Long): Int {
        require(detents > 0) { "detents must be positive" }
        val gap = lastTimeMs?.let { timeMs - it }
        val multiplier = when {
            gap == null || lastClockwise != clockwise -> 1
            gap < tripleBelowMs -> TRIPLE
            gap < doubleBelowMs -> DOUBLE
            else -> 1
        }
        lastTimeMs = timeMs
        lastClockwise = clockwise
        val steps = detents * multiplier
        return if (clockwise) steps else -steps
    }

    fun reset() {
        lastTimeMs = null
        lastClockwise = null
    }

    companion object {
        private const val TRIPLE = 3
        private const val DOUBLE = 2
        const val DEFAULT_TRIPLE_BELOW_MS = 25L
        const val DEFAULT_DOUBLE_BELOW_MS = 50L
    }
}
