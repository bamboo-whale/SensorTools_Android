package com.sensortools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = Border,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )

                // 内圈
                drawCircle(
                    color = BorderLight.copy(alpha = 0.4f),
                    radius = r * 0.85f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f)
                )

                // 十字参考线
                drawLine(BorderLight.copy(alpha = 0.3f), Offset(cx - r, cy), Offset(cx + r, cy), 1f)
                drawLine(BorderLight.copy(alpha = 0.3f), Offset(cx, cy - r), Offset(cx, cy + r), 1f)

                // 气泡位置（限幅）
                val bubbleR = r * 0.2f
                val maxOffset = r * 0.55f
                val bx = (cx + (y / (PI.toFloat() / 6f)) * maxOffset).coerceIn(cx - maxOffset, cx + maxOffset)
                val by = (cy - (x / (PI.toFloat() / 6f)) * maxOffset).coerceIn(cy - maxOffset, cy + maxOffset)

                // 气泡
                drawCircle(
                    color = StatusNormal,
                    radius = bubbleR,
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = StatusNormal.copy(alpha = 0.3f),
                    radius = bubbleR + 3f,
                    center = Offset(bx, by),
                    style = Stroke(width = 1.5f)
                )

                // 中心小点
                drawCircle(
                    color = TextPrimary.copy(alpha = 0.5f),
                    radius = 4f,
                    center = Offset(cx, cy)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("X 倾斜", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text(
                    "${"%.1f".format(Math.toDegrees(x.toDouble()))}°",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = ChartAxisX
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Y 倾斜", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text(
                    "${"%.1f".format(Math.toDegrees(y.toDouble()))}°",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = ChartAxisY
                )
            }
        }
    }
}
