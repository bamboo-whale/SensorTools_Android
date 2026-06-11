package com.sensortools.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sensortools.ui.theme.*

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = TextPrimary)
            }
            Text("关于", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // App Icon placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CardBackground, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Sensors, null, tint = TextPrimary, modifier = Modifier.size(40.dp))
            }

            Text("SensorTools", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("专业传感器检测与录制工具", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("版本 1.0.0", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = TextTertiary)

            // Feature list
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionItem(Icons.Filled.Search, "传感器扫描", "自动检测全部传感器及设备信息")
                    SectionItem(Icons.Filled.ShowChart, "实时监控", "XYZ 三轴实时数据 & 动态图表")
                    SectionItem(Icons.Filled.Tune, "传感器校准", "加速度计/陀螺仪/磁力计校准")
                    SectionItem(Icons.Filled.HealthAndSafety, "健康诊断", "异常/卡死/跳变检测 & 原因分析")
                    SectionItem(Icons.Filled.SaveAlt, "数据记录", "后台录制 + CSV/JSON 导出 + 元数据")
                }
            }

            // Tech stack
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("技术栈", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Kotlin + Jetpack Compose + Material 3", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Text("MVVM 架构 | 自定义 Canvas 图表 | 前台 Service", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Text("Android 8.0+ | 零第三方图表库", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = StatusNormal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}
