package com.sensortools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sensortools.data.model.SensorData
import com.sensortools.ui.theme.*
import kotlin.math.*

/**
 * 高性能实时传感器曲线图
 * 自定义 Canvas 绘制，零第三方依赖
 */
@Composable
fun SensorChart(
    dataBuffer: List<SensorData>,
    showX: Boolean = true,
    showY: Boolean = true,
    showZ: Boolean = true,
    modifier: Modifier = Modifier,
    maxPoints: Int = 500
) {
    var scaleX by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    val displayData = remember(dataBuffer) {
        if (dataBuffer.size <= maxPoints) dataBuffer
        else dataBuffer.takeLast(maxPoints)
    }

    val chartColors = listOfNotNull(
        if (showX) ChartAxisX else null,
        if (showY) ChartAxisY else null,
        if (showZ) ChartAxisZ else null
    )

    Column(modifier = modifier) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showX) LegendItem("X", ChartAxisX)
            if (showY) LegendItem("Y", ChartAxisY)
            if (showZ) LegendItem("Z", ChartAxisZ)
        }

        // 图表
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scaleX = (scaleX * zoom).coerceIn(0.5f, 10f)
                        offsetX = (offsetX + pan.x).coerceIn(-500f, 0f)
                    }
                }
        ) {
            val data = displayData
            if (data.isEmpty()) return@Canvas

            val w = size.width
            val h = size.height
            val paddingLeft = 50f
            val paddingRight = 16f
            val paddingTop = 16f
            val paddingBottom = 30f
            val plotW = w - paddingLeft - paddingRight
            val plotH = h - paddingTop - paddingBottom

            // 背景网格
            drawGrid(paddingLeft, paddingTop, plotW, plotH)

            // 计算数据范围
            var minVal = Float.MAX_VALUE
            var maxVal = Float.MIN_VALUE
            data.forEach { sd ->
                if (showX) { if (sd.x < minVal) minVal = sd.x; if (sd.x > maxVal) maxVal = sd.x }
                if (showY) { if (sd.y < minVal) minVal = sd.y; if (sd.y > maxVal) maxVal = sd.y }
                if (showZ) { if (sd.z < minVal) minVal = sd.z; if (sd.z > maxVal) maxVal = sd.z }
            }
            if (minVal == Float.MAX_VALUE) minVal = -1f
            if (maxVal == Float.MIN_VALUE) maxVal = 1f
            // 留 10% 边距
            val range = maxVal - minVal
            if (range < 0.001f) {
                minVal -= 1f; maxVal += 1f
            } else {
                val padding = range * 0.1f
                minVal -= padding; maxVal += padding
            }

            // 绘制曲线
            val axes = listOfNotNull(
                if (showX) 0 else null,
                if (showY) 1 else null,
                if (showZ) 2 else null
            )

            axes.forEach { axis ->
                val color = chartColors[axes.indexOf(axis)]
                val path = Path()
                var first = true

                val visibleCount = (data.size / scaleX).toInt().coerceAtMost(data.size)
                val startIdx = (data.size - visibleCount + (offsetX / plotW * data.size)).toInt()
                    .coerceIn(0, (data.size - 1).coerceAtLeast(0))

                for (i in startIdx until data.size) {
                    val x = paddingLeft + plotW * (i - startIdx).toFloat() / (data.size - startIdx).coerceAtLeast(1)
                    val value = when (axis) {
                        0 -> data[i].x
                        1 -> data[i].y
                        else -> data[i].z
                    }
                    val y = paddingTop + plotH * (1f - (value - minVal) / (maxVal - minVal))

                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.5f)
                )
            }

            // 刻度标注
            drawAxisLabels(paddingLeft, paddingTop, plotW, plotH, minVal, maxVal)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = 4.dp.toPx())
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun DrawScope.drawGrid(
    left: Float, top: Float, width: Float, height: Float
) {
    val gridColor = Border.copy(alpha = 0.3f)

    // 水平网格线 (5条)
    for (i in 0..4) {
        val y = top + height * i / 4
        drawLine(
            color = gridColor,
            start = Offset(left, y),
            end = Offset(left + width, y),
            strokeWidth = 0.5f
        )
    }

    // 垂直网格线 (4条)
    for (i in 0..4) {
        val x = left + width * i / 4
        drawLine(
            color = gridColor,
            start = Offset(x, top),
            end = Offset(x, top + height),
            strokeWidth = 0.5f
        )
    }

    // 边框
    drawRect(
        color = Border,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawAxisLabels(
    left: Float, top: Float, width: Float, height: Float,
    minVal: Float, maxVal: Float
) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6B7280")
        textSize = 22f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.RIGHT
    }

    for (i in 0..4) {
        val value = maxVal - (maxVal - minVal) * i / 4
        val y = top + height * i / 4 + 6f
        drawContext.canvas.nativeCanvas.drawText(
            "%.1f".format(value), left - 6f, y, textPaint
        )
    }
}
