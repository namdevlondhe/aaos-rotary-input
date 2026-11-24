package dev.namdev.rotary.input

import kotlin.math.abs

/** An axis-aligned rectangle in screen coordinates (y grows downwards). */
data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
}

/**
 * Chooses the focus area a nudge should go to.
 *
 * Uses the rules of Android's FocusFinder: a candidate qualifies only if it lies past the current area in the
 * nudge direction (an area beside it does not count as "above" just because its centre is higher), and the
 * closest wins by `13 * major² + minor²`, where major is the edge gap along the direction and minor the
 * distance between centres across it.
 */
object AreaFinder {

    private const val MAJOR_WEIGHT = 13f

    fun <K> nearest(from: Bounds, candidates: Map<K, Bounds>, direction: Direction): K? =
        candidates.entries
            .filter { (_, bounds) -> isBeyond(from, bounds, direction) }
            .minByOrNull { (_, bounds) -> score(from, bounds, direction) }
            ?.key

    private fun isBeyond(from: Bounds, to: Bounds, direction: Direction): Boolean = when (direction) {
        Direction.Right -> (from.left < to.left || from.right <= to.left) && from.right < to.right
        Direction.Left -> (from.right > to.right || from.left >= to.right) && from.left > to.left
        Direction.Down -> (from.top < to.top || from.bottom <= to.top) && from.bottom < to.bottom
        Direction.Up -> (from.bottom > to.bottom || from.top >= to.bottom) && from.top > to.top
    }

    private fun score(from: Bounds, to: Bounds, direction: Direction): Float {
        val major = when (direction) {
            Direction.Right -> to.left - from.right
            Direction.Left -> from.left - to.right
            Direction.Down -> to.top - from.bottom
            Direction.Up -> from.top - to.bottom
        }.coerceAtLeast(0f)
        val minor = when (direction) {
            Direction.Left, Direction.Right -> abs(from.centerY - to.centerY)
            Direction.Up, Direction.Down -> abs(from.centerX - to.centerX)
        }
        return MAJOR_WEIGHT * major * major + minor * minor
    }
}
