package com.sensortools.ui.record

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sensortools.data.model.SensorInfo
import com.sensortools.ui.components.getSensorColor
import com.sensortools.ui.components.getSensorIcon
import com.sensortools.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: RecordViewModel = viewModel()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordCount by viewModel.recordCount.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val exportFile by viewModel.exportFile.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val countdown by viewModel.countdown.collectAsState()

    var selectedSensor by remember { mutableStateOf<SensorInfo?>(null) }
    var showCountdownPicker by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var annotationText by remember { mutableStateOf("") }
    var showAnnotation by remember { mutableStateOf(false) }

    // Countdown dialog
    if (showCountdownPicker) {
        AlertDialog(
            onDismissRequest = { showCountdownPicker = false },
            containerColor = CardBackground,
            title = { Text("延时启动", color = TextPrimary) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0 to "立即", 3 to "3秒", 5 to "5秒", 10 to "10秒").forEach { (secs, label) ->
                        FilterChip(
                            selected = countdownSeconds == secs,
                            onClick = { countdownSeconds = secs },
                            label = { Text(label, color = if (countdownSeconds == secs) TextPrimary else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CardBackgroundAlt
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCountdownPicker = false
                    selectedSensor?.let { viewModel.startRecordingWithCountdown(it, countdownSeconds) }
                }) { Text("开始", color = StatusNormal) }
            },
            dismissButton = {
                TextButton(onClick = { showCountdownPicker = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }

    // Annotation dialog
    if (showAnnotation) {
        AlertDialog(
            onDismissRequest = { showAnnotation = false },
            containerColor = CardBackground,
            title = { Text("添加标注", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = annotationText,
                    onValueChange = { annotationText = it },
                    placeholder = { Text("例如：开始步行", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = StatusNormal,
                        unfocusedBorderColor = Border
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (annotationText.isNotBlank()) {
                        viewModel.addAnnotation(annotationText)
                        annotationText = ""
                    }
                    showAnnotation = false
                }) { Text("标注", color = StatusNormal) }
            },
            dismissButton = {
                TextButton(onClick = { showAnnotation = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }

    // Countdown overlay
    if (countdown > 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$countdown",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = StatusNormal
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "数据记录",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "后台录制传感器数据，支持延时启动与标注",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Recording UI
        if (isRecording) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(2.dp, StatusError.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(StatusError)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("● 录制中", style = MaterialTheme.typography.titleLarge, color = StatusError, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("采样数", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("$recordCount", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace), color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("时长", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("%.1fs".format(duration / 1000f), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace), color = TextPrimary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick annotation chips
            Text("快速标注", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickLabels = listOf("开始步行", "跑步", "骑行", "静止", "进室内", "到室外")
                items(quickLabels) { label ->
                    SuggestionChip(
                        onClick = { viewModel.addAnnotation(label) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CardBackgroundAlt
                        )
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { showAnnotation = true },
                        label = { Text("自定义...", fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardBackgroundAlt)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.stopRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Stop, null)
                Spacer(Modifier.width(6.dp))
                Text("停止记录")
            }
        }

        // Export UI
        if (!isRecording && exportFile != null) {
            val file = exportFile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = StatusNormal, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("导出完成", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(file?.name ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("${recordCount} 条记录", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { viewModel.shareFile() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary)) {
                            Icon(Icons.Filled.Share, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("分享", color = TextSecondary)
                        }
                        OutlinedButton(onClick = { viewModel.clearExport() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                            Text("返回", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Sensor selection + Start UI
        if (!isRecording && exportFile == null) {
            Text("选择传感器", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sensors) { sensor ->
                    val isSelected = selectedSensor == sensor
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) CardBackgroundAlt else CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) StatusNormal else Border),
                        onClick = { selectedSensor = sensor }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(getSensorIcon(sensor.type), null, tint = getSensorColor(sensor.type), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sensor.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(sensor.typeName, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                            if (isSelected) Icon(Icons.Filled.CheckCircle, null, tint = StatusNormal, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showCountdownPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSensor != null,
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedSensor != null) StatusNormal else Border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.FiberManualRecord, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (selectedSensor != null) "开始记录 ${selectedSensor!!.name}" else "请先选择一个传感器")
                }

                if (selectedSensor != null) {
                    Text("点击上方按钮选择延时启动", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(horizontal = 4.dp))
                }

                if (recordCount > 0 && !isRecording) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { viewModel.exportCsv() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ChartAxisX)) {
                            Text("导出 CSV", color = ChartAxisX)
                        }
                        OutlinedButton(onClick = { viewModel.exportJson() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ChartAxisY)) {
                            Text("导出 JSON", color = ChartAxisY)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
