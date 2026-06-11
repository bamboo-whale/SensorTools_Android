package com.sensortools.ui.health

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensortools.domain.HealthAnalyzer
import com.sensortools.ui.components.StatusBadge
import com.sensortools.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    viewModel: HealthViewModel = viewModel()
) {
    val results by viewModel.results.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentSensor by viewModel.currentSensor.collectAsState()
    val scanNote by viewModel.scanNote.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val totalSensors = results.size
    val abnormalCount = results.count { it.status == HealthAnalyzer.HealthStatus.ABNORMAL || it.status == HealthAnalyzer.HealthStatus.SUSPECT }
    val normalCount = results.count { it.status == HealthAnalyzer.HealthStatus.NORMAL }
    val noDataCount = results.count { it.status == HealthAnalyzer.HealthStatus.NO_DATA }
    val onlyAbnormal = remember { mutableStateOf(false) }
    val visibleResults = if (onlyAbnormal.value) {
        results.filter { it.status != HealthAnalyzer.HealthStatus.NORMAL }
    } else {
        results
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "健康诊断",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "扫描全部传感器的工作状态，快速发现异常、冻结和无数据项",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(StatusNormal.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.HealthAndSafety, null, tint = StatusNormal)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("快速健康检查", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(scanNote, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        StatusBadge(
                            text = if (isScanning) "扫描中" else "待命",
                            status = if (isScanning) HealthAnalyzer.HealthStatus.SUSPECT else HealthAnalyzer.HealthStatus.NORMAL
                        )
                    }

                    if (scanError != null) {
                        Surface(
                            color = StatusError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = scanError ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = StatusError,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.startScan() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusNormal,
                            disabledContainerColor = Border
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.HealthAndSafety, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isScanning) "扫描中..." else "开始健康扫描")
                    }

                    if (isScanning) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = StatusNormal,
                            trackColor = Border,
                        )
                        Text(
                            "当前检查: ${if (currentSensor.isBlank()) "准备中" else currentSensor}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundAlt),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("扫描概览", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryMetric("总数", totalSensors.toString(), TextPrimary)
                        SummaryMetric("正常", normalCount.toString(), StatusNormal)
                        SummaryMetric("异常", abnormalCount.toString(), StatusWarning)
                        SummaryMetric("无数据", noDataCount.toString(), StatusInactive)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { viewModel.startScan() },
                            enabled = !isScanning,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Icon(Icons.Filled.Refresh, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("重新扫描", color = TextSecondary)
                        }
                        OutlinedButton(
                            onClick = { onlyAbnormal.value = !onlyAbnormal.value },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (onlyAbnormal.value) StatusWarning else Border)
                        ) {
                            Icon(Icons.Filled.FilterAlt, null, tint = if (onlyAbnormal.value) StatusWarning else TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (onlyAbnormal.value) "显示全部" else "仅看异常", color = if (onlyAbnormal.value) StatusWarning else TextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("如何理解结果", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("正常表示读数变化符合预期；异常通常是采样间隔、跳变或值域不稳定；疑似故障更偏向卡死、冻结或长期无变化。", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("如果你刚刚才开始扫描，请保持手机静置或轻微晃动 2-3 秒，让动态传感器有足够样本。", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
        }

        if (results.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("尚未开始扫描", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("点击上方按钮后，应用会按顺序检查所有可用传感器并生成健康报告。", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ResultChip("正常", normalCount, StatusNormal)
                    ResultChip("异常", results.count { it.status == HealthAnalyzer.HealthStatus.ABNORMAL }, StatusWarning)
                    ResultChip("疑似故障", results.count { it.status == HealthAnalyzer.HealthStatus.SUSPECT }, StatusError)
                    ResultChip("无数据", noDataCount, StatusInactive)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("检测结果", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("${visibleResults.size} 项", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            }
        }

        items(visibleResults, key = { it.sensor.name + it.sensor.type }) { item ->
            HealthItemCard(item)
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ResultChip(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthItemCard(item: HealthViewModel.HealthItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.sensor.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        item.sensor.typeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(
                    text = when (item.status) {
                        HealthAnalyzer.HealthStatus.NORMAL -> "正常"
                        HealthAnalyzer.HealthStatus.ABNORMAL -> "异常"
                        HealthAnalyzer.HealthStatus.SUSPECT -> "疑似故障"
                        HealthAnalyzer.HealthStatus.NO_DATA -> "无数据"
                    },
                    status = item.status
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2
            )

            if (item.details.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.details.entries.take(3).forEach { entry ->
                        Surface(
                            color = CardBackgroundAlt,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Text(
                                "${entry.key}: ${entry.value}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    item.details.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text(
                                value,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
