package com.sensortools.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensortools.data.model.SensorInfo
import com.sensortools.domain.HealthAnalyzer
import com.sensortools.ui.theme.*

// ── 传感器卡片 ──

@Composable
fun SensorCard(
    sensor: SensorInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 传感器图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getSensorColor(sensor.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getSensorIcon(sensor.type),
                    contentDescription = null,
                    tint = getSensorColor(sensor.type),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friendlySensorLabel(sensor),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sensor.typeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // 状态指示器
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (sensor.isAvailable) StatusNormal else StatusError)
            )
        }
    }
}

private fun friendlySensorLabel(sensor: SensorInfo): String = when (sensor.type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER -> "加速度计"
    android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "未校准加速度计"
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION -> "线性加速度"
    android.hardware.Sensor.TYPE_GRAVITY -> "重力传感器"
    android.hardware.Sensor.TYPE_GYROSCOPE -> "陀螺仪"
    android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "未校准陀螺仪"
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> "磁力计"
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "未校准磁力计"
    android.hardware.Sensor.TYPE_ROTATION_VECTOR -> "旋转矢量"
    android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR -> "游戏旋转矢量"
    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "地磁旋转矢量"
    android.hardware.Sensor.TYPE_LIGHT -> "光线传感器"
    android.hardware.Sensor.TYPE_PROXIMITY -> "距离传感器"
    android.hardware.Sensor.TYPE_PRESSURE -> "气压计"
    android.hardware.Sensor.TYPE_STEP_COUNTER -> "步数统计"
    android.hardware.Sensor.TYPE_STEP_DETECTOR -> "步数检测"
    android.hardware.Sensor.TYPE_HEART_RATE -> "心率传感器"
    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE -> "环境温度"
    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> "相对湿度"
    else -> sensor.typeName.ifBlank { "传感器" }
}

// ── 设备信息卡片 ──

@Composable
fun DeviceInfoCard(
    info: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "设备信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(16.dp))
            info.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { (key, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                key,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Section 标题 ──

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        trailing?.invoke()
    }
}

// ── 数值大字显示 ──

@Composable
fun ValueDisplay(
    label: String,
    value: String,
    color: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ── 控制按钮行 ──

@Composable
fun ControlButtonRow(
    isRunning: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRecord: (() -> Unit)? = null,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isRunning) {
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StatusNormal),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("开始监听")
            }
        } else if (isPaused) {
            OutlinedButton(
                onClick = onResume,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning)
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("恢复监听", color = StatusWarning)
            }
        } else {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary)
            ) {
                Icon(Icons.Filled.Pause, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("暂停", color = TextSecondary)
            }
        }

        if (onRecord != null) {
            OutlinedButton(
                onClick = onRecord,
                enabled = isRunning,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRunning) StatusError else Border
                )
            ) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    null,
                    tint = if (isRunning) StatusError else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("去录制", color = if (isRunning) StatusError else TextTertiary)
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Icon(Icons.Filled.Refresh, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("重置", color = TextSecondary)
        }
    }
}

// ── 状态标签 ──

@Composable
fun StatusBadge(
    text: String,
    status: HealthAnalyzer.HealthStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        HealthAnalyzer.HealthStatus.NORMAL -> StatusNormal
        HealthAnalyzer.HealthStatus.ABNORMAL -> StatusWarning
        HealthAnalyzer.HealthStatus.SUSPECT -> StatusError
        HealthAnalyzer.HealthStatus.NO_DATA -> StatusInactive
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── 传感器颜色/图标辅助 ──

fun getSensorColor(type: Int): Color = when (type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER,
    android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
    android.hardware.Sensor.TYPE_GRAVITY -> SensorAccel

    android.hardware.Sensor.TYPE_GYROSCOPE,
    android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorGyro

    android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorMag

    android.hardware.Sensor.TYPE_LIGHT -> SensorLight
    android.hardware.Sensor.TYPE_PROXIMITY -> SensorProximity
    android.hardware.Sensor.TYPE_PRESSURE -> SensorPressure
    else -> SensorOther
}

fun getSensorIcon(type: Int): ImageVector = when (type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER,
    android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION -> Icons.Filled.Speed

    android.hardware.Sensor.TYPE_GYROSCOPE,
    android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> Icons.Filled.Autorenew

    android.hardware.Sensor.TYPE_GRAVITY -> Icons.Filled.ArrowDownward
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> Icons.Filled.Explore

    android.hardware.Sensor.TYPE_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> Icons.Filled.RotateRight

    android.hardware.Sensor.TYPE_LIGHT -> Icons.Filled.LightMode
    android.hardware.Sensor.TYPE_PROXIMITY -> Icons.Filled.Sensors
    android.hardware.Sensor.TYPE_PRESSURE -> Icons.Filled.Compress
    android.hardware.Sensor.TYPE_STEP_COUNTER,
    android.hardware.Sensor.TYPE_STEP_DETECTOR -> Icons.Filled.DirectionsWalk

    android.hardware.Sensor.TYPE_HEART_RATE -> Icons.Filled.Favorite
    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE -> Icons.Filled.Thermostat
    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> Icons.Filled.WaterDrop
    else -> Icons.Filled.DeviceUnknown
}
