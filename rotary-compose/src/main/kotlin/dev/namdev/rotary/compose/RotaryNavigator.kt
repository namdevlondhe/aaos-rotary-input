package dev.namdev.rotary.compose

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import dev.namdev.rotary.input.AreaFinder
import dev.namdev.rotary.input.Bounds
import dev.namdev.rotary.input.Direction
import kotlin.math.abs

/**
 * Rotary navigation over focus areas, modelled on AAOS (car-ui-lib `FocusArea` + RotaryService):
 * - **rotate** moves focus to the next/previous item *within* the current area and stops at its edge;
 * - **nudge** moves to the nearest area in that direction and restores the item last focused there;
 * - with no area focused yet, any input focuses the default area.
 *
 * Screens without areas still work: rotation and nudges fall back to plain Compose focus traversal.
 */
@Stable
class RotaryNavigator internal constructor() {

    internal class Area(val id: String) {
        val requester = FocusRequester()
        var bounds: Bounds? = null
        var hasFocus by mutableStateOf(false)

        /** Enters the area; its focusRestorer picks the last focused item, or the first one. */
        fun focus(): Boolean {
            requester.requestFocus()
            return true
        }
    }

    private val areas = LinkedHashMap<String, Area>()
    internal var focusManager: FocusManager? = null

    /** Area to focus when nothing is focused; the first registered area if null. */
    var defaultAreaId: String? = null

    /** The focus area that currently contains focus, if any. Observable from composition. */
    val focusedAreaId: String?
        get() = areas.values.firstOrNull { it.hasFocus }?.id

    /** The area that last had focus; where focus reappears after a touch has hidden it. */
    var lastFocusedAreaId: String? = null
        private set

    val areaIds: List<String> get() = areas.keys.toList()

    val hasAreas: Boolean get() = areas.isNotEmpty()

    /** Moves focus [steps] items within the current area (positive = next). Returns true if focus moved. */
    fun rotate(steps: Int): Boolean {
        val manager = focusManager
        return when {
            manager == null || steps == 0 -> false
            hasAreas && focusedAreaId == null -> revealFocus()
            else -> rotateWithinArea(manager, steps)
        }
    }

    /** Moves focus to the nearest area in [direction]. Returns true if focus moved. */
    fun nudge(direction: Direction): Boolean {
        val manager = focusManager ?: return false
        val current = focusedAreaId?.let(areas::get)
        return when {
            !hasAreas -> manager.moveFocus(direction.toFocusDirection())
            current == null -> revealFocus()
            else -> nearestArea(areas.values, current, direction)?.focus() ?: false
        }
    }

    /** Focuses the area the user was last in, or the default area. Returns true if an area was focused. */
    fun revealFocus(): Boolean = lastFocusedAreaId?.let(::focusArea)?.takeIf { it } ?: focusDefaultArea()

    /** Focuses [defaultAreaId] (or the first area). Returns true if an area was focused. */
    fun focusDefaultArea(): Boolean {
        val area = defaultAreaId?.let(areas::get) ?: areas.values.firstOrNull() ?: return false
        return area.focus()
    }

    /** Focuses the area with [id], restoring its last focused item. */
    fun focusArea(id: String): Boolean = areas[id]?.focus() ?: false

    private fun rotateWithinArea(manager: FocusManager, steps: Int): Boolean {
        val direction = if (steps > 0) FocusDirection.Next else FocusDirection.Previous
        var moved = 0
        var blocked = false
        while (moved < abs(steps) && !blocked) {
            val before = focusedAreaId
            blocked = !manager.moveFocus(direction) || leftArea(before)
            if (!blocked) moved++
        }
        return moved > 0
    }

    /**
     * True if the last move stepped out of the area [before]. Focus is then put back by re-entering that area:
     * its focusRestorer returns to the item just left, so rotation stops on the edge item, as in AAOS.
     */
    private fun leftArea(before: String?): Boolean {
        val left = hasAreas && focusedAreaId != before
        if (left) before?.let(areas::get)?.focus()
        return left
    }

    internal fun register(id: String): Area = areas.getOrPut(id) { Area(id) }

    internal fun unregister(id: String) {
        areas.remove(id)
    }

    internal fun onAreaFocusChanged(area: Area, hasFocus: Boolean) {
        area.hasFocus = hasFocus
        if (hasFocus) lastFocusedAreaId = area.id
    }
}

private fun nearestArea(
    areas: Collection<RotaryNavigator.Area>,
    current: RotaryNavigator.Area,
    direction: Direction,
): RotaryNavigator.Area? {
    val from = current.bounds ?: return null
    val candidates = areas.filter { it !== current }.mapNotNull { area -> area.bounds?.let { area to it } }.toMap()
    return AreaFinder.nearest(from, candidates, direction)
}

private fun Direction.toFocusDirection(): FocusDirection = when (this) {
    Direction.Up -> FocusDirection.Up
    Direction.Down -> FocusDirection.Down
    Direction.Left -> FocusDirection.Left
    Direction.Right -> FocusDirection.Right
}

val LocalRotaryNavigator = staticCompositionLocalOf<RotaryNavigator?> { null }

@Composable
fun rememberRotaryNavigator(): RotaryNavigator = remember { RotaryNavigator() }

/**
 * Marks a group of focusable items as one focus area (a nav rail, a list, a media bar).
 * Must be inside a [RotaryHost].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.focusArea(id: String): Modifier {
    val navigator = checkNotNull(LocalRotaryNavigator.current) { "focusArea(\"$id\") must be used inside a RotaryHost" }
    val area = remember(navigator, id) { navigator.register(id) }
    DisposableEffect(navigator, id) {
        onDispose { navigator.unregister(id) }
    }
    return this
        .onGloballyPositioned { coordinates ->
            val rect = coordinates.boundsInRoot()
            area.bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom)
        }
        .onFocusChanged { navigator.onAreaFocusChanged(area, it.hasFocus) }
        .focusRequester(area.requester)
        .focusRestorer()
        .focusGroup()
}
