package com.mattgouws.maxlifttracker.ui.detail

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.MetricEntry
import com.mattgouws.maxlifttracker.ui.formatValue
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/** The chart only makes sense once there's a trend to show. */
const val MIN_CHART_ENTRIES = 2

// Beyond this many points, per-point markers crowd the line, so only the latest keeps one.
private const val MAX_MARKED_POINTS = 20

/**
 * Evenly spaced, round-numbered y-axis ticks covering [min]..[max] (e.g. 60, 70, 80, 90),
 * never going below zero.
 */
fun niceTicks(min: Double, max: Double, targetCount: Int = 4): List<Double> {
    if (min == max) {
        val pad = if (min == 0.0) 1.0 else abs(min) * 0.1
        return niceTicks((min - pad).coerceAtLeast(0.0), max + pad, targetCount)
    }
    val rawStep = (max - min) / (targetCount - 1)
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rawStep }
    val start = floor(min / step) * step
    val count = ceil((max - start) / step - 1e-9).toInt() + 1
    // Round away floating-point noise such as 0.30000000000000004.
    return List(count) { i -> ((start + i * step) * 1e6).roundToLong() / 1e6 }
}

@Composable
fun ProgressChart(
    entries: List<MetricEntry>,
    unit: String,
    modifier: Modifier = Modifier,
) {
    if (entries.size < MIN_CHART_ENTRIES) return

    val points = remember(entries) { entries.sortedBy { it.loggedAt } }
    val ticks = remember(points) { niceTicks(points.minOf { it.value }, points.maxOf { it.value }) }
    val shortDate = remember { shortDateFormatter() }
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val colors = MaterialTheme.colorScheme
    val lineColor = colors.primary
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface)
    val textMeasurer = rememberTextMeasurer()
    val yLabelWidth = remember(ticks, unit, textStyle) {
        ticks.maxOf { textMeasurer.measure(formatValue(it, unit), textStyle).size.width }
    }

    fun label(index: Int): String {
        val entry = points[index]
        return "${formatValue(entry.value, unit)} · ${shortDate.format(entry.zoned())}"
    }

    val description = stringResource(
        R.string.chart_description,
        points.size,
        formatValue(points.first().value, unit),
        formatValue(points.last().value, unit),
        formatValue(points.maxOf { it.value }, unit),
    )
    val selectedLabel = selected?.let { label(it) }

    Column(modifier) {
        Text(
            text = stringResource(R.string.chart_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("progressChart")
                .semantics {
                    contentDescription = description
                    if (selectedLabel != null) stateDescription = selectedLabel
                }
                .pointerInput(points, yLabelWidth) {
                    fun nearest(x: Float) =
                        ChartGeometry(points, size.width.toFloat(), size.height.toFloat(), yLabelWidth, density).nearestIndex(x)
                    detectTapGestures { offset ->
                        val index = nearest(offset.x)
                        selected = if (selected == index) null else index
                    }
                }
                .pointerInput(points, yLabelWidth) {
                    fun nearest(x: Float) =
                        ChartGeometry(points, size.width.toFloat(), size.height.toFloat(), yLabelWidth, density).nearestIndex(x)
                    detectHorizontalDragGestures(
                        onDragStart = { selected = nearest(it.x) },
                    ) { change, _ -> selected = nearest(change.position.x) }
                },
        ) {
            val geometry = ChartGeometry(points, size.width, size.height, yLabelWidth, density)
            val yMin = ticks.first()
            val yMax = ticks.last()
            fun yFor(value: Double) =
                geometry.plotBottom - ((value - yMin) / (yMax - yMin)).toFloat() * (geometry.plotBottom - geometry.plotTop)

            // Gridlines and y-axis labels: hairline, recessive.
            ticks.forEach { tick ->
                val y = yFor(tick)
                drawLine(colors.outlineVariant, Offset(geometry.plotLeft, y), Offset(geometry.plotRight, y), strokeWidth = 1f)
                val text = textMeasurer.measure(formatValue(tick, unit), textStyle)
                drawText(
                    text,
                    topLeft = Offset(geometry.plotLeft - 8.dp.toPx() - text.size.width, y - text.size.height / 2f),
                )
            }

            // X-axis: first and last dates only, or a single date when everything is from one day.
            val firstDateText = shortDate.format(points.first().zoned())
            val lastDateText = shortDate.format(points.last().zoned())
            val dateTop = geometry.plotBottom + 6.dp.toPx()
            drawText(textMeasurer.measure(firstDateText, textStyle), topLeft = Offset(geometry.plotLeft, dateTop))
            if (lastDateText != firstDateText) {
                val lastDate = textMeasurer.measure(lastDateText, textStyle)
                drawText(lastDate, topLeft = Offset(geometry.plotRight - lastDate.size.width, dateTop))
            }

            val offsets = points.mapIndexed { i, entry -> Offset(geometry.xFor(i), yFor(entry.value)) }
            val line = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(offsets.last().x, geometry.plotBottom)
                lineTo(offsets.first().x, geometry.plotBottom)
                close()
            }
            drawPath(area, lineColor.copy(alpha = 0.1f))
            drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val selectedIndex = selected
            if (selectedIndex != null) {
                val x = offsets[selectedIndex].x
                drawLine(colors.outline, Offset(x, geometry.plotTop), Offset(x, geometry.plotBottom), strokeWidth = 1.dp.toPx())
            }

            // Markers: filled, with a 2dp ring in the surface colour so they stay legible on the line.
            val marked = if (points.size <= MAX_MARKED_POINTS) offsets.indices else listOf(offsets.lastIndex)
            marked.forEach { i ->
                val radius = if (i == selectedIndex) 6.dp.toPx() else 4.dp.toPx()
                drawCircle(colors.surface, radius + 2.dp.toPx(), offsets[i])
                drawCircle(lineColor, radius, offsets[i])
            }

            // Tooltip for the selected point, kept inside the chart bounds.
            if (selectedIndex != null) {
                val text = textMeasurer.measure(label(selectedIndex), tooltipStyle)
                val pad = 8.dp.toPx()
                val boxSize = Size(text.size.width + pad * 2, text.size.height + pad)
                val anchor = offsets[selectedIndex]
                val left = (anchor.x - boxSize.width / 2).coerceIn(0f, size.width - boxSize.width)
                val above = anchor.y - boxSize.height - 10.dp.toPx()
                val top = if (above >= 0f) above else anchor.y + 10.dp.toPx()
                drawRoundRect(colors.surfaceContainerHighest, Offset(left, top), boxSize, CornerRadius(8.dp.toPx()))
                drawText(text, topLeft = Offset(left + pad, top + pad / 2))
            }
        }
    }
}

/** Maps points to x positions: by time, or evenly if they were all logged at the same moment. */
private class ChartGeometry(
    private val points: List<MetricEntry>,
    width: Float,
    height: Float,
    yLabelWidth: Int,
    density: Float,
) {
    val plotLeft = yLabelWidth + 8f * density + 6f * density
    val plotRight = width - 8f * density
    val plotTop = 12f * density
    val plotBottom = height - 24f * density

    private val start = points.first().loggedAt
    private val span = points.last().loggedAt - start

    fun xFor(index: Int): Float {
        val fraction = if (span == 0L) {
            index.toFloat() / (points.size - 1)
        } else {
            (points[index].loggedAt - start).toFloat() / span
        }
        return plotLeft + fraction * (plotRight - plotLeft)
    }

    fun nearestIndex(x: Float): Int = points.indices.minBy { abs(xFor(it) - x) }
}

private fun MetricEntry.zoned() = Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault())

/** Day and short month in the device locale, e.g. "29 Sept" or "Sep 29". */
private fun shortDateFormatter(): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
}

@Preview(showBackground = true)
@Composable
private fun ProgressChartPreview() {
    val day = 24 * 60 * 60 * 1000L
    val start = 1_788_000_000_000
    val values = listOf(60.0, 62.5, 62.5, 65.0, 67.5, 70.0, 72.5)
    MaxLiftTrackerTheme {
        ProgressChart(
            entries = values.mapIndexed { i, v -> MetricEntry(id = i.toLong(), metricId = 1, value = v, loggedAt = start + i * 3 * day) },
            unit = "kg",
            modifier = Modifier.padding(16.dp),
        )
    }
}
