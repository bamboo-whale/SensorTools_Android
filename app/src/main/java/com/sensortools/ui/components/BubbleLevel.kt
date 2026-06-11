package com.sensortools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sensortools.ui.theme.*
import kotlin.math.*

/**
 * 气泡水平仪
 * @param x X 轴倾斜角（弧度）
 * @param y Y 轴倾斜角（弧度）
 */
@Composable
fun BubbleLevel(
    x: Float,
    y: Float,
    modifier: Modifier = Modifier
) {
    var smoothX by remember { mutableFloatStateOf(x) }
    var smoothY by remember { mutableFloatStateOf(y) }
    smoothX = smoothX * 0.78f + x * 0.22f
    smoothY = smoothY * 0.78f + y * 0.22f
    val tiltMagnitude = sqrt(smoothX * smoothX + smoothY * smoothY)
    val nearLevel = tiltMagnitude < 0.04f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "水平仪",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.width / 2 - 8f

                // 外圈
                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 5f)
                )

                // 参考圆 + 活动圆，水平时重合
                val targetRadius = r * 0.22f
                val referenceRadius = r * 0.22f
                val maxOffset = r * 0.42f
                val offsetScale = r * 0.58f
                val bx = (cx + (smoothY / (PI.toFloat() / 7.5f)) * offsetScale).coerceIn(cx - maxOffset, cx + maxOffset)
                val by = (cy - (smoothX / (PI.toFloat() / 7.5f)) * offsetScale).coerceIn(cy - maxOffset, cy + maxOffset)

                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = referenceRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4.5f)
                )
                drawCircle(
                    color = if (nearLevel) StatusNormal else StatusWarning,
                    radius = targetRadius,
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = if (nearLevel) StatusNormal.copy(alpha = 0.3f) else StatusWarning.copy(alpha = 0.3f),
                    radius = targetRadius + 4f,
                    center = Offset(bx, by),
                    style = Stroke(width = 1.5f)
                )

                // 专业刻度感：四个方向的小刻线
                val markLength = r * 0.08f
                val markInset = r * 0.92f
                val marks = listOf(
                    Offset(cx, cy - markInset) to Offset(cx, cy - markInset + markLength),
                    Offset(cx + markInset, cy) to Offset(cx + markInset - markLength, cy),
                    Offset(cx, cy + markInset) to Offset(cx, cy + markInset - markLength),
                    Offset(cx - markInset, cy) to Offset(cx - markInset + markLength, cy)
                )
                marks.forEach { (start, end) ->
                    drawLine(
                        color = Color.White,
                        start = start,
                        end = end,
                        strokeWidth = 3f
                    )
                }

                // 中心十字参考
                drawLine(Color.White.copy(alpha = 0.65f), Offset(cx - r * 0.18f, cy), Offset(cx + r * 0.18f, cy), 2f)
                drawLine(Color.White.copy(alpha = 0.65f), Offset(cx, cy - r * 0.18f), Offset(cx, cy + r * 0.18f), 2f)

                // 中心小点
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 9f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (nearLevel) "已接近水平" else "偏移越大，说明越不水平",
            style = MaterialTheme.typography.bodyMedium,
            color = if (nearLevel) StatusNormal else TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("X 倾斜", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text(
                    "${"%.1f".format(Math.toDegrees(smoothX.toDouble()))}°",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = ChartAxisX
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Y 倾斜", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text(
                    "${"%.1f".format(Math.toDegrees(smoothY.toDouble()))}°",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = ChartAxisY
                )
            }
        }
    }
}
