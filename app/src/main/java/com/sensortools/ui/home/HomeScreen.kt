package com.sensortools.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensortools.data.model.SensorInfo
import com.sensortools.ui.components.DeviceInfoCard
import com.sensortools.ui.components.getSensorColor
import com.sensortools.ui.components.getSensorIcon
import com.sensortools.ui.theme.*

private data class SensorGroup(
    val title: String,
    val subtitle: String,
    val sensors: List<SensorInfo>
)

@Composable
fun HomeScreen(
    onSensorClick: (SensorInfo) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val commonSensors by viewModel.commonSensors.collectAsState()
    val unknownSensors by viewModel.unknownSensors.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groupedSensors = remember(commonSensors, unknownSensors) {
        buildSensorGroups(commonSensors + unknownSensors)
    }
    val expandedGroups = remember(groupedSensors) {
        mutableStateMapOf<String, Boolean>().apply {
            groupedSensors.forEach { group -> put(group.title, false) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SensorTools", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("专业传感器检测工具", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
                Row {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, "设置", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // 设备信息
        if (!isLoading && deviceInfo.isNotEmpty()) {
            item { DeviceInfoCard(info = deviceInfo) }
        }

        // 传感器统计
        if (!isLoading) {
            val total = viewModel.totalSensorCount()
            val categoryCount = viewModel.getSensorCountByCategory()
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("传感器总数", "${total}")
                        StatItem("类型数", "${categoryCount.size}")
                        StatItem("动态传感", "${(commonSensors + unknownSensors).count { it.isDynamic }}")
                    }
                }
            }
        }

        if (!isLoading) {
            groupedSensors.forEach { group ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Column {
                            GroupHeaderRow(
                                title = group.title,
                                subtitle = group.subtitle,
                                count = group.sensors.size,
                                icon = categoryIcon(group.title),
                                expanded = expandedGroups[group.title] ?: true,
                                onToggle = { expandedGroups[group.title] = !(expandedGroups[group.title] ?: false) }
                            )
                            AnimatedVisibility(visible = expandedGroups[group.title] ?: false) {
                                Column {
                                    group.sensors.forEachIndexed { index, sensor ->
                                        SensorSummaryRow(
                                            sensor = sensor,
                                            onClick = { onSensorClick(sensor) }
                                        )
                                        if (index < group.sensors.lastIndex) {
                                            HorizontalDivider(color = Border)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 加载态
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextSecondary)
                }
            }
        }

        // 底部留白
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun GroupHeaderRow(
    title: String,
    subtitle: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = TextTertiary
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun SensorSummaryRow(
    sensor: SensorInfo,
    onClick: () -> Unit
) {
    val friendlyName = friendlySensorName(sensor)
    val groupHint = sensorCategoryLabel(sensor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(getSensorColor(sensor.type).copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getSensorIcon(sensor.type),
                contentDescription = null,
                tint = getSensorColor(sensor.type),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                friendlyName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                groupHint,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (sensor.isWakeUp) "唤醒" else "常规",
            style = MaterialTheme.typography.labelSmall,
            color = if (sensor.isWakeUp) StatusWarning else TextTertiary
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

private fun buildSensorGroups(sensors: List<SensorInfo>): List<SensorGroup> {
    val motion = sensors.filter {
        it.type in setOf(
            android.hardware.Sensor.TYPE_ACCELEROMETER,
            android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
            android.hardware.Sensor.TYPE_GRAVITY,
            android.hardware.Sensor.TYPE_GYROSCOPE,
            android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            android.hardware.Sensor.TYPE_ROTATION_VECTOR,
            android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
            android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        )
    }
    val environment = sensors.filter {
        it.type in setOf(
            android.hardware.Sensor.TYPE_LIGHT,
            android.hardware.Sensor.TYPE_PROXIMITY,
            android.hardware.Sensor.TYPE_PRESSURE,
            android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE,
            android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY,
            android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
            android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
        )
    }
    val body = sensors.filter {
        it.type in setOf(
            android.hardware.Sensor.TYPE_STEP_COUNTER,
            android.hardware.Sensor.TYPE_STEP_DETECTOR,
            android.hardware.Sensor.TYPE_HEART_RATE
        )
    }
    val others = sensors - motion.toSet() - environment.toSet() - body.toSet()

    return buildList {
        if (motion.isNotEmpty()) add(SensorGroup("运动与姿态", "加速度、陀螺仪、旋转相关传感器", motion))
        if (environment.isNotEmpty()) add(SensorGroup("环境感知", "光线、距离、气压、温湿度等传感器", environment))
        if (body.isNotEmpty()) add(SensorGroup("人体与计步", "步数和生理相关传感器", body))
        if (others.isNotEmpty()) add(SensorGroup("其他传感器", "厂商扩展或系统私有传感器", others))
    }
}

private fun friendlySensorName(sensor: SensorInfo): String = when (sensor.type) {
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
    else -> "传感器"
}

private fun sensorCategoryLabel(sensor: SensorInfo): String = when (sensor.type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER,
    android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
    android.hardware.Sensor.TYPE_GRAVITY,
    android.hardware.Sensor.TYPE_GYROSCOPE,
    android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
    android.hardware.Sensor.TYPE_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "运动与姿态"
    android.hardware.Sensor.TYPE_LIGHT,
    android.hardware.Sensor.TYPE_PROXIMITY,
    android.hardware.Sensor.TYPE_PRESSURE,
    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE,
    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY,
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "环境感知"
    android.hardware.Sensor.TYPE_STEP_COUNTER,
    android.hardware.Sensor.TYPE_STEP_DETECTOR,
    android.hardware.Sensor.TYPE_HEART_RATE -> "人体与计步"
    else -> "其他传感器"
}

private fun categoryIcon(title: String) = when (title) {
    "运动与姿态" -> Icons.Filled.DirectionsRun
    "环境感知" -> Icons.Filled.Cloud
    "人体与计步" -> Icons.Filled.DirectionsWalk
    else -> Icons.Filled.Sensors
}
