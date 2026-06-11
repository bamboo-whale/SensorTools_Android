package com.sensortools.ui.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensortools.domain.HealthAnalyzer
import com.sensortools.ui.components.StatusBadge
import com.sensortools.ui.theme.Background
import com.sensortools.ui.theme.Border
import com.sensortools.ui.theme.CardBackground
import com.sensortools.ui.theme.CardBackgroundAlt
import com.sensortools.ui.theme.StatusError
import com.sensortools.ui.theme.StatusNormal
import com.sensortools.ui.theme.StatusWarning
import com.sensortools.ui.theme.TextPrimary
import com.sensortools.ui.theme.TextSecondary
import com.sensortools.ui.theme.TextTertiary
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel = viewModel()
) {
    val accelCalibration by viewModel.accelCalibration.collectAsState()
    val accelProgress by viewModel.accelProgress.collectAsState()
    val accelCalibrating by viewModel.accelCalibrating.collectAsState()
    val gyroCalibration by viewModel.gyroCalibration.collectAsState()
    val gyroProgress by viewModel.gyroProgress.collectAsState()
    val gyroCalibrating by viewModel.gyroCalibrating.collectAsState()
    val magCalibration by viewModel.magCalibration.collectAsState()
    val magObserving by viewModel.magObserving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "传感器校准",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("校准总览", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    statusMessage ?: "选择一个传感器开始校准，采集期间请保持设备稳定或按提示移动设备。",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusMessage == null) TextSecondary else TextTertiary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = if (accelCalibrating || gyroCalibrating || magObserving) "进行中" else "空闲",
                        status = if (accelCalibrating || gyroCalibrating || magObserving) HealthAnalyzer.HealthStatus.SUSPECT else HealthAnalyzer.HealthStatus.NORMAL
                    )
                    StatusBadge(
                        text = "加速度计 / 陀螺仪 / 磁力计",
                        status = HealthAnalyzer.HealthStatus.NORMAL
                    )
                }
            }
        }

        CalibrationCard(
            title = "加速度计校准",
            icon = Icons.Filled.Speed,
            description = "将设备水平静置于桌面，点击开始校准。",
            isCalibrating = accelCalibrating,
            progress = accelProgress,
            progressMax = 100,
            calibration = accelCalibration,
            onStart = { viewModel.startAccelCalibration() }
        )

        CalibrationCard(
            title = "陀螺仪校准",
            icon = Icons.Filled.Autorenew,
            description = "保持设备完全静止，点击开始校准采集零偏。",
            isCalibrating = gyroCalibrating,
            progress = gyroProgress,
            progressMax = 200,
            calibration = gyroCalibration,
            onStart = { viewModel.startGyroCalibration() }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(StatusNormal.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(Icons.Filled.Explore, null, tint = StatusNormal)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("磁力计校准", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("手持设备在空中画 8 字，覆盖各个方向。", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }

                val quality = magCalibration?.quality ?: 0f
                CalibrationProgressBar(quality)
                Text(
                    "校准质量: ${"%.0f".format(quality * 100)}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (quality > 0.85f) StatusNormal else StatusWarning
                )

                if (magCalibration?.isComplete == true) {
                    Text("校准完成", color = StatusNormal, style = MaterialTheme.typography.bodyMedium)
                }

                if (!magObserving) {
                    Button(
                        onClick = { viewModel.startMagCalibration() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusNormal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("开始 8 字校准")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.stopMagCalibration() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusError)
                    ) {
                        Text("停止校准", color = StatusError)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundAlt),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("结果预览", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                ResultLine("加速度计", accelCalibration)
                ResultLine("陀螺仪", gyroCalibration)
                ResultLine("磁力计", magCalibration)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultLine(label: String, calibration: com.sensortools.data.model.CalibrationData?) {
    val text = when {
        calibration == null -> "未开始"
        calibration.isComplete -> "完成 · ${"%.0f".format(calibration.quality * 100)}%"
        else -> "进行中"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun CalibrationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isCalibrating: Boolean,
    progress: Int,
    progressMax: Int,
    calibration: com.sensortools.data.model.CalibrationData?,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(StatusNormal.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(icon, null, tint = StatusNormal)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }

            when {
                isCalibrating -> {
                    LinearProgressIndicator(
                        progress = progress.toFloat() / progressMax,
                        modifier = Modifier.fillMaxWidth(),
                        color = StatusNormal,
                        trackColor = Border
                    )
                    Text("采集中... $progress / $progressMax", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                calibration?.isComplete == true -> {
                    Text("质量: ${"%.0f".format(calibration.quality * 100)}%", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace), color = StatusNormal)
                    Text(
                        "偏移 X=${"%.4f".format(calibration.offsetX)}  Y=${"%.4f".format(calibration.offsetY)}  Z=${"%.4f".format(calibration.offsetZ)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextSecondary
                    )
                }
                calibration != null -> {
                    Text("校准未完成", style = MaterialTheme.typography.bodyMedium, color = StatusWarning)
                }
                else -> {
                    Text("尚未开始", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            if (calibration?.isComplete == true) {
                OutlinedButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.Refresh, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("重新校准", color = TextSecondary)
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.PlayArrow, null, tint = Background, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("开始校准", color = Background)
                }
            }
        }
    }
}

@Composable
private fun CalibrationProgressBar(quality: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(quality)
                .height(8.dp)
                .background(
                    when {
                        quality > 0.85f -> StatusNormal
                        quality > 0.5f -> StatusWarning
                        else -> StatusError
                    }
                )
        )
    }
}
