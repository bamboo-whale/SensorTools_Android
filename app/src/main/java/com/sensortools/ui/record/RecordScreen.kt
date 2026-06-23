package com.sensortools.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.sensortools.data.model.SensorInfo
import com.sensortools.domain.HealthAnalyzer
import com.sensortools.ui.components.SensorChart
import com.sensortools.ui.components.StatusBadge
import com.sensortools.ui.components.getSensorColor
import com.sensortools.ui.components.getSensorIcon
import com.sensortools.ui.theme.*

private data class SensorGroup(
    val title: String,
    val subtitle: String,
    val sensors: List<SensorInfo>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(
    preselectedSensorType: Int? = null,
    viewModel: RecordViewModel = viewModel()
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val recordCount by viewModel.recordCount.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val exportFile by viewModel.exportFile.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val listeningSensor by viewModel.listeningSensor.collectAsState()
    val listeningData by viewModel.listeningData.collectAsState()
    val listeningIsRunning by viewModel.listeningIsRunning.collectAsState()
    val listeningStatus by viewModel.listeningStatus.collectAsState()

    val groupedSensors = remember(sensors) { buildSensorGroups(sensors) }
    val expandedGroups = remember(groupedSensors) {
        mutableStateMapOf<String, Boolean>().apply {
            groupedSensors.forEach { put(it.title, false) }
        }
    }

    var selectedSensor by remember { mutableStateOf<SensorInfo?>(null) }
    var showCountdownPicker by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var annotationText by remember { mutableStateOf("") }
    var showAnnotation by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSensor) {
        selectedSensor?.let { viewModel.selectSensor(it) }
    }

    LaunchedEffect(preselectedSensorType, sensors, groupedSensors) {
        val type = preselectedSensorType ?: return@LaunchedEffect
        val sensor = sensors.find { it.type == type } ?: return@LaunchedEffect
        selectedSensor = sensor
        groupedSensors.find { group -> group.sensors.any { it.type == type } }
            ?.title
            ?.let { expandedGroups[it] = true }
    }

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
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CardBackgroundAlt)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 96.sp
                ),
                color = StatusNormal
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "数据记录",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "这里用于采集传感器原始数据，适合做实验、导出分析和老师演示。先选传感器，再开始录制，也可以给当前片段添加标注。",
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
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusNormal.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Hub, null, tint = StatusNormal, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("使用引导", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("按分类折叠浏览传感器，点开后选择目标设备再开始录制。", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(text = if (isRecording) "录制中" else "未录制", status = if (isRecording) HealthAnalyzer.HealthStatus.SUSPECT else HealthAnalyzer.HealthStatus.NO_DATA)
                        StatusBadge(text = "支持 CSV / JSON", status = HealthAnalyzer.HealthStatus.NORMAL)
                        StatusBadge(text = "支持标注", status = HealthAnalyzer.HealthStatus.NORMAL)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundAlt),
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("采样数", "$recordCount")
                    StatItem("录制时长", "%.1fs".format(duration / 1000f))
                    StatItem("传感器数", "${sensors.size}")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusNormal.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = StatusNormal, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("监听预览", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                if (listeningSensor == null) "先选择一个传感器，再查看实时波形和数值。"
                                else "当前监听：${friendlySensorName(listeningSensor!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                        StatusBadge(
                            text = listeningStatus,
                            status = if (listeningIsRunning) HealthAnalyzer.HealthStatus.NORMAL else HealthAnalyzer.HealthStatus.NO_DATA
                        )
                    }

                    if (listeningSensor != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { viewModel.startListeningPreview() },
                                label = { Text("开始监听") },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp)) }
                            )
                            AssistChip(
                                onClick = { viewModel.pauseListeningPreview() },
                                label = { Text("暂停") },
                                leadingIcon = { Icon(Icons.Filled.Pause, null, modifier = Modifier.size(18.dp)) }
                            )
                            AssistChip(
                                onClick = { viewModel.stopListeningPreview() },
                                label = { Text("停止") },
                                leadingIcon = { Icon(Icons.Filled.Stop, null, modifier = Modifier.size(18.dp)) }
                            )
                        }

                        Text(
                            "最新数据：${listeningData.lastOrNull()?.let { "%.2f, %.2f, %.2f".format(it.x, it.y, it.z) } ?: "暂无"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (listeningData.isNotEmpty()) {
                            SensorChart(
                                dataBuffer = listeningData,
                                showX = true,
                                showY = true,
                                showZ = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                "开始监听后，这里会显示实时曲线和采样值。",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    } else {
                        Text(
                            "选择一个传感器后，监听预览会自动就绪。",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        if (!isRecording && exportFile != null) {
            item {
                val file = exportFile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = StatusNormal, modifier = Modifier.size(44.dp))
                        Text("导出完成", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(file?.name ?: "", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextSecondary)
                        Text("${recordCount} 条记录", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (!viewModel.shareFile()) {
                                        Toast.makeText(context, "无法分享，请确认已安装可用应用", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TextSecondary)
                            ) {
                                Icon(Icons.Filled.Share, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("分享", color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearExport() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Text("返回", color = TextSecondary)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (!viewModel.openExportFolder()) {
                                    val path = file?.parentFile?.absolutePath.orEmpty()
                                    Toast.makeText(
                                        context,
                                        if (path.isNotBlank()) "已保存至：$path" else "无法打开文件管理器",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusNormal)
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, tint = StatusNormal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("打开保存位置", color = StatusNormal)
                        }
                    }
                }
            }
        }

        if (isRecording) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(2.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StatusError)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("正在录制", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.Bold)
                        }
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
            }

            item {
                Text("快速标注", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quickLabels = listOf("开始步行", "跑步", "骑行", "静止", "进室内", "到室外")
                    items(quickLabels) { label ->
                        SuggestionChip(
                            onClick = { viewModel.addAnnotation(label) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardBackgroundAlt)
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
            }

            item {
                Button(
                    onClick = { viewModel.stopRecording() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Stop, null)
                    Spacer(Modifier.width(6.dp))
                    Text("停止记录")
                }
            }
        }

        if (!isRecording && exportFile == null) {
            items(groupedSensors) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column {
                        GroupHeaderRow(
                            title = group.title,
                            subtitle = group.subtitle,
                            count = group.sensors.size,
                            icon = categoryIcon(group.title),
                            expanded = expandedGroups[group.title] ?: false,
                            onToggle = { expandedGroups[group.title] = !(expandedGroups[group.title] ?: false) }
                        )
                        AnimatedVisibility(visible = expandedGroups[group.title] ?: false) {
                            Column {
                                group.sensors.forEachIndexed { index, sensor ->
                                    SensorRow(
                                        sensor = sensor,
                                        selected = selectedSensor == sensor,
                                        onClick = { selectedSensor = sensor }
                                    )
                                    if (index < group.sensors.lastIndex) HorizontalDivider(color = Border)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showCountdownPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedSensor != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSensor != null) StatusNormal else Border
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FiberManualRecord, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedSensor != null) "开始记录 ${selectedSensor!!.name}" else "请先选择一个传感器")
                    }

                    if (selectedSensor != null) {
                        Text("当前已选：${selectedSensor!!.name}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text("点击上方按钮选择延时启动", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }

                    if (selectedSensor != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.startListeningPreview() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StatusNormal)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = StatusNormal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("监听", color = StatusNormal)
                            }
                            OutlinedButton(
                                onClick = { viewModel.stopListeningPreview() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Border)
                            ) {
                                Icon(Icons.Filled.Stop, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("停止监听", color = TextSecondary)
                            }
                        }
                    }

                    if (recordCount > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.exportCsv() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ChartAxisX)
                            ) {
                                Text("导出 CSV", color = ChartAxisX)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportJson() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ChartAxisY)
                            ) {
                                Text("导出 JSON", color = ChartAxisY)
                            }
                        }
                    }
                }
            }
        }
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("($count)", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = TextTertiary
        )
    }
}

@Composable
private fun SensorRow(
    sensor: SensorInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) CardBackgroundAlt else Color.Transparent)
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
            Text(friendlySensorName(sensor), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sensor.typeName, style = MaterialTheme.typography.labelSmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (selected) {
            Icon(Icons.Filled.CheckCircle, null, tint = StatusNormal, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
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

private fun categoryIcon(title: String) = when (title) {
    "运动与姿态" -> Icons.Filled.DirectionsRun
    "环境感知" -> Icons.Filled.Cloud
    "人体与计步" -> Icons.Filled.DirectionsWalk
    else -> Icons.Filled.Sensors
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
    else -> sensor.name.ifBlank { sensor.typeName }
}
