package com.sensortools.ui.intro

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sensortools.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    onDone: () -> Unit
) {
    val pageCount = 3
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDone) {
                Text("跳过", color = TextSecondary)
            }
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { page ->
                IntroPage(page)
            }
        }

        // Indicators
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                        .background(
                            if (index == currentPage) StatusNormal else Border,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // Bottom button
        Button(
            onClick = {
                if (currentPage < pageCount - 1) currentPage++
                else onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusNormal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (currentPage < pageCount - 1) "下一步" else "开始使用",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun IntroPage(page: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        val (icon, title, desc) = when (page) {
            0 -> Triple(Icons.Filled.Search, "扫描传感器", "自动检测手机所有传感器\n型号、厂商、分辨率、功耗等详尽参数")
            1 -> Triple(Icons.Filled.ShowChart, "实时监控", "XYZ 三轴实时数据流\n动态曲线图 & 状态自动分析")
            else -> Triple(Icons.Filled.SaveAlt, "数据采集", "后台录制、延时启动、快捷标注\nCSV / JSON 导出 + 元数据")
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(CardBackground, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = StatusNormal, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            desc,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}
