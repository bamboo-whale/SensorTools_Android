package com.sensortools.ui.settings

import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sensortools.data.local.PreferencesManager
import com.sensortools.ui.theme.Background
import com.sensortools.ui.theme.Border
import com.sensortools.ui.theme.CardBackground
import com.sensortools.ui.theme.StatusNormal
import com.sensortools.ui.theme.TextPrimary
import com.sensortools.ui.theme.TextSecondary
import com.sensortools.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var samplingPeriod by remember { mutableIntStateOf(prefs.getSamplingPeriod()) }
    var chartMaxPoints by remember { mutableIntStateOf(prefs.getChartMaxPoints()) }

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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("传感器采样率", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前: ${prefs.getSamplingPeriodLabel()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusNormal
                    )
                    Spacer(Modifier.height(8.dp))

                    val options = listOf(
                        SensorManager.SENSOR_DELAY_NORMAL to "NORMAL (~5 Hz)",
                        SensorManager.SENSOR_DELAY_UI to "UI (~15 Hz)",
                        SensorManager.SENSOR_DELAY_GAME to "GAME (~50 Hz)",
                        SensorManager.SENSOR_DELAY_FASTEST to "FASTEST"
                    )

                    options.forEach { (period, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    samplingPeriod = period
                                    prefs.setSamplingPeriod(period)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = samplingPeriod == period,
                                onClick = {
                                    samplingPeriod = period
                                    prefs.setSamplingPeriod(period)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = StatusNormal,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
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
                    Text("图表设置", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text("最大数据点数: $chartMaxPoints", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = chartMaxPoints.toFloat(),
                        onValueChange = {
                            chartMaxPoints = it.toInt()
                            prefs.setChartMaxPoints(chartMaxPoints)
                        },
                        valueRange = 100f..2000f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = StatusNormal,
                            activeTrackColor = StatusNormal,
                            inactiveTrackColor = Border
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("导出说明", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "CSV: timestamp, sensor_name, x, y, z, accuracy, annotation (BOM UTF-8)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        "JSON: records[] + metadata{device, sensor, session}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        "同时生成 _meta.json 会话元数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
