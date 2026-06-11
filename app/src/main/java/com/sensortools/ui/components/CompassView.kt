package com.sensortools.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sensortools.ui.theme.*
import kotlin.math.*

@Composable
fun CompassView(
    azimuth: Float,
    accuracy: Int = 0,
    modifier: Modifier = Modifier
) {
    var smoothAzimuth by remember { mutableFloatStateOf(azimuth) }
    var azimuthInitialized by remember { mutableStateOf(false) }
    if (!azimuthInitialized) {
        smoothAzimuth = azimuth
        azimuthInitialized = true
    } else {
        val current = smoothAzimuth
        val diff = ((azimuth - current + 540f) % 360f) - 180f
        smoothAzimuth = (current + diff * 0.18f + 360f) % 360f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "指南针",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))

        val direction = getDirection(smoothAzimuth)

        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.width / 2 - 4f

                // 刻度盘背景
                drawCircle(color = CardBackground, radius = r)
                drawCircle(color = Border, radius = r, style = Stroke(width = 1.5f))

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#A0A6B2")
                    textSize = 28f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                val textPaintBold = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFFFFF")
                    textSize = 34f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }

                // 刻度线 + 文字标注
                for (deg in 0 until 360 step 5) {
                    val rotatedDeg = deg - smoothAzimuth.toInt()
                    val rad = Math.toRadians(rotatedDeg.toDouble())
                    val cosA = cos(rad).toFloat()
                    val sinA = sin(rad).toFloat()

                    val isCardinal = deg % 90 == 0
                    val isMajor = deg % 30 == 0
                    val innerR = if (isCardinal) r * 0.65f else if (isMajor) r * 0.72f else if (deg % 15 == 0) r * 0.78f else r * 0.83f
                    val outerR = r * 0.92f

                    drawLine(
                        color = if (isCardinal) TextPrimary
                                else if (isMajor) TextSecondary
                                else BorderLight,
                        start = Offset(cx + cosA * innerR, cy + sinA * innerR),
                        end = Offset(cx + cosA * outerR, cy + sinA * outerR),
                        strokeWidth = if (isCardinal) 2.5f else if (isMajor) 2f else 1f
                    )

                    // 文字标注：N/S/E/W 及每30度标度数
                    if (isCardinal) {
                        val label = when (deg) {
                            0 -> "N"
                            90 -> "E"
                            180 -> "S"
                            270 -> "W"
                            else -> ""
                        }
                        val labelR = r * 0.52f
                        val lx = cx + cosA * labelR
                        val ly = cy + sinA * labelR + 10f
                        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, textPaintBold)
                    } else if (isMajor) {
                        val label = "${deg}°"
                        val labelR = r * 0.57f
                        val lx = cx + cosA * labelR
                        val ly = cy + sinA * labelR + 10f
                        drawContext.canvas.nativeCanvas.drawText(label, lx, ly, textPaint)
                    }
                }

                // 指针（不随表盘旋转 — 固定在设备方向）
                // 设备固定朝上，表盘已经旋转了 azimuth 角度
                // 指针指向正上方（北）
                // 北指针
                val northPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy - r * 0.55f)
                    lineTo(cx - 14f, cy + 6f)
                    lineTo(cx, cy)
                    lineTo(cx + 14f, cy + 6f)
                    close()
                }
                drawPath(northPath, color = StatusError)

                // 南指针（半透明）
                val southPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy + r * 0.55f)
                    lineTo(cx - 10f, cy - 4f)
                    lineTo(cx, cy)
                    lineTo(cx + 10f, cy - 4f)
                    close()
                }
                drawPath(southPath, color = TextSecondary.copy(alpha = 0.35f))

                // 中心点
                drawCircle(color = TextPrimary, radius = 6f, center = Offset(cx, cy))
                drawCircle(color = StatusError, radius = 3f, center = Offset(cx, cy))
            }

            // 方向文字
            Text(
                text = direction.label,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${"%.1f".format(smoothAzimuth)}° ${direction.label}",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            color = TextPrimary
        )

        val accuracyStr = when (accuracy) {
            3 -> "高精度"
            2 -> "中等精度"
            1 -> "低精度"
            else -> "不可靠"
        }
        Text(
            text = accuracyStr,
            style = MaterialTheme.typography.labelSmall,
            color = if (accuracy >= 2) StatusNormal else StatusWarning
        )
    }
}

enum class CompassDirection(val label: String, val range: ClosedFloatingPointRange<Float>) {
    N("北", 348.75f..360f),
    NNE("东北偏北", 11.25f..33.75f),
    NE("东北", 33.75f..56.25f),
    ENE("东北偏东", 56.25f..78.75f),
    E("东", 78.75f..101.25f),
    ESE("东南偏东", 101.25f..123.75f),
    SE("东南", 123.75f..146.25f),
    SSE("东南偏南", 146.25f..168.75f),
    S("南", 168.75f..191.25f),
    SSW("西南偏南", 191.25f..213.75f),
    SW("西南", 213.75f..236.25f),
    WSW("西南偏西", 236.25f..258.75f),
    W("西", 258.75f..281.25f),
    WNW("西北偏西", 281.25f..303.75f),
    NW("西北", 303.75f..326.25f),
    NNW("西北偏北", 326.25f..348.75f),
    N2("北", 0f..11.25f);
}

private fun getDirection(azimuth: Float): CompassDirection {
    val a = azimuth % 360f
    return CompassDirection.entries.firstOrNull { a in it.range }
        ?: CompassDirection.N
}
