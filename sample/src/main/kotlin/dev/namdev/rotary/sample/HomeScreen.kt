package dev.namdev.rotary.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.namdev.hmi.components.HmiBanner
import dev.namdev.hmi.theme.HmiTheme
import dev.namdev.rotary.compose.LocalRotaryNavigator
import dev.namdev.rotary.compose.RotaryInputInjector
import dev.namdev.rotary.compose.focusArea
import dev.namdev.rotary.input.HmiAction

const val AREA_RAIL = "rail"
const val AREA_CONTENT = "content"
const val AREA_PLAYER = "player"

/**
 * A car home screen with three focus areas:
 *
 *     [rail]  [content ]  [controller + input log]
 *             [player  ]
 *
 * The knob moves within an area, a nudge moves between areas; the side panel is not focusable.
 */
@Composable
fun HomeScreen(state: SampleState, injector: RotaryInputInjector) {
    val dimens = HmiTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HmiTheme.colors.background)
            .safeDrawingPadding()
            .padding(dimens.spacingL),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingL),
    ) {
        Rail(state, Modifier.width(250.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spacingM)) {
            state.message?.let { (text, level) -> HmiBanner(text, level = level) }
            Content(state, Modifier.weight(1f))
            Player(state)
        }
        Column(Modifier.width(380.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(dimens.spacingM)) {
            OnScreenController(injector, state)
            InputLog(state, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Rail(state: SampleState, modifier: Modifier) {
    Column(
        modifier = modifier.focusArea(AREA_RAIL),
        verticalArrangement = Arrangement.spacedBy(HmiTheme.dimens.spacingXs),
    ) {
        Text("Home", style = HmiTheme.typography.title, color = HmiTheme.colors.onSurface)
        Section.entries.forEach { section ->
            RotaryTile(
                title = section.label,
                icon = section.icon,
                selected = section == state.section,
                onClick = { state.open(section) },
                modifier = Modifier.testTag("rail-${section.name}"),
            )
        }
    }
}

@Composable
private fun Content(state: SampleState, modifier: Modifier) {
    val focusedArea = LocalRotaryNavigator.current?.focusedAreaId
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HmiTheme.dimens.spacingS)) {
        Text(
            "${state.section.label}${if (focusedArea != null) "   ·   focus: $focusedArea" else ""}",
            style = HmiTheme.typography.label,
            color = HmiTheme.colors.onSurfaceMuted,
        )
        LazyColumn(
            modifier = Modifier.focusArea(AREA_CONTENT),
            verticalArrangement = Arrangement.spacedBy(HmiTheme.dimens.spacingXs),
        ) {
            items(state.section.items, key = { "${state.section}-${it.first}" }) { (title, supporting) ->
                RotaryTile(
                    title = title,
                    supporting = supporting,
                    icon = state.section.icon,
                    onClick = { state.openItem(title) },
                    modifier = Modifier.testTag("item-$title"),
                )
            }
        }
    }
}

@Composable
private fun Player(state: SampleState) {
    val colors = HmiTheme.colors
    val dimens = HmiTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.corner + dimens.spacingS))
            .background(colors.surface)
            .padding(dimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
    ) {
        val (title, artist) = state.track
        Row(
            modifier = Modifier.padding(horizontal = dimens.spacingS),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingM),
        ) {
            Text(title, style = HmiTheme.typography.body, color = colors.onSurface, maxLines = 1)
            Text(
                "$artist   ·   ${time(state.positionSeconds)} / ${time(state.durationSeconds)}   ·   " +
                    if (state.muted) "muted" else "volume ${state.volume}",
                style = HmiTheme.typography.caption,
                color = colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("now-playing"),
            )
        }
        LinearProgressIndicator(
            progress = { state.positionSeconds / state.durationSeconds.toFloat() },
            modifier = Modifier
                .padding(horizontal = dimens.spacingS)
                .fillMaxWidth()
                .height(6.dp),
            color = colors.primary,
            trackColor = colors.surfaceRaised,
        )
        Row(Modifier.focusArea(AREA_PLAYER), horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
            RotaryTile(
                "Previous",
                onClick = { state.onAction(HmiAction.PreviousTrack) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("player-Previous"),
            )
            RotaryTile(
                if (state.playing) "Pause" else "Play",
                onClick = { state.onAction(HmiAction.PlayPause) },
                modifier = Modifier.weight(1f),
                selected = true,
            )
            RotaryTile("Next", onClick = { state.onAction(HmiAction.NextTrack) }, Modifier.weight(1f))
        }
    }
}

private fun time(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
