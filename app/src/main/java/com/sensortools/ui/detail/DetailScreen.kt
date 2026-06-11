package com.sensortools.ui.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensortools.data.model.SensorInfo
import com.sensortools.ui.components.*
import com.sensortools.ui.theme.*
import com.sensortools.util.MotionDetector

@Composable
fun DetailScreen(
    sensorInfo: SensorInfo,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val data by viewModel.data.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val statusLabel by viewModel.statusLabel.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val samplingRate by viewModel.samplingRate.collectAsState()

    LaunchedEffect(sensorInfo) {
        viewModel.initSensor(sensorInfo)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    val motionState = remember(data.size) {
        if (data.size >= 20 && sensorInfo.type == android.hardware.Sensor.TYPE_ACCELEROMETER)
            MotionDetector.getMotionLabel(MotionDetector.detectMotion(data.takeLast(50)))
        else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.stopListening()
                onBack()
            }) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary)
            }
            Text(
                sensorInfo.name,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sensorInfo.typeName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusBadge(
                            text = if (isRunning && !isPaused) "监听中" else if (isPaused) "已暂停" else "已停止",
                            status = if (isRunning) com.sensortools.domain.HealthAnalyzer.HealthStatus.NORMAL
                                     else com.sensortools.domain.HealthAnalyzer.HealthStatus.NO_DATA
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoChip("厂商", sensorInfo.vendor)
                        InfoChip("版本", "${sensorInfo.version}")
                        InfoChip("功耗", "%.2f mA".format(sensorInfo.power))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoChip("分辨率", "%.4f".format(sensorInfo.resolution))
                        InfoChip("最大量程", "%.1f".format(sensorInfo.maxRange))
                        InfoChip("最小延迟", "${sensorInfo.minDelay} μs")
                    }
                }
            }

            if (isRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ValueDisplay("X 轴", "%.3f".format(stats.currentX), ChartAxisX)
                            ValueDisplay("Y 轴", "%.3f".format(stats.currentY), ChartAxisY)
                            ValueDisplay("Z 轴", "%.3f".format(stats.currentZ), ChartAxisZ)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("采样频率", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Text(
                                    "%.0f Hz".format(samplingRate),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("采样数", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Text(
                                    "${stats.valueCount}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("运行时间", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Text(
                                    "%.1fs".format(stats.elapsedMs / 1000f),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("极值统计", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MiniStat("X Min", "%.3f".format(stats.minX), ChartAxisX)
                            MiniStat("X Max", "%.3f".format(stats.maxX), ChartAxisX)
                            MiniStat("Y Min", "%.3f".format(stats.minY), ChartAxisY)
                            MiniStat("Y Max", "%.3f".format(stats.maxY), ChartAxisY)
                            MiniStat("Z Min", "%.3f".format(stats.minZ), ChartAxisZ)
                            MiniStat("Z Max", "%.3f".format(stats.maxZ), ChartAxisZ)
                        }
                    }
                }

                if (statusLabel != "等待中") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("状态分析", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusNormal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (motionState.isNotEmpty()) {
                            Text(
                                "运动状态: $motionState",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            if (isRunning && data.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("实时曲线", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        SensorChart(
                            dataBuffer = data,
                            showX = true,
                            showY = true,
                            showZ = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            ControlButtonRow(
                isRunning = isRunning,
                isPaused = isPaused,
                onStart = { viewModel.startListening() },
                onPause = { viewModel.pauseListening() },
                onResume = { viewModel.resumeListening() },
                onReset = { viewModel.reset() }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextTertiary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = color
        )
    }
}
