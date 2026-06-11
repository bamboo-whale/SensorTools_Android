package com.sensortools.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import com.sensortools.data.model.SensorData
import com.sensortools.data.model.SensorInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SensorRepository(private val context: Context) {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // ── 传感器类型名称映射 ──
    private val typeNameMap = mapOf(
        Sensor.TYPE_ACCELEROMETER to "加速度计",
        Sensor.TYPE_GYROSCOPE to "陀螺仪",
        Sensor.TYPE_GRAVITY to "重力传感器",
        Sensor.TYPE_LINEAR_ACCELERATION to "线性加速度",
        Sensor.TYPE_MAGNETIC_FIELD to "磁力计",
        Sensor.TYPE_ROTATION_VECTOR to "旋转矢量",
        Sensor.TYPE_LIGHT to "光线传感器",
        Sensor.TYPE_PROXIMITY to "距离传感器",
        Sensor.TYPE_PRESSURE to "气压计",
        Sensor.TYPE_STEP_COUNTER to "步数计",
        Sensor.TYPE_STEP_DETECTOR to "步数检测器",
        Sensor.TYPE_HEART_RATE to "心率传感器",
        Sensor.TYPE_GAME_ROTATION_VECTOR to "游戏旋转矢量",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "地磁旋转矢量",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "环境温度",
        Sensor.TYPE_RELATIVE_HUMIDITY to "相对湿度",
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to "未校准磁力计",
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED to "未校准陀螺仪",
        Sensor.TYPE_SIGNIFICANT_MOTION to "显著运动",
        Sensor.TYPE_STATIONARY_DETECT to "静止检测",
        Sensor.TYPE_MOTION_DETECT to "运动检测",
        Sensor.TYPE_POSE_6DOF to "6自由度姿态",
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED to "未校准加速度计",
        Sensor.TYPE_ORIENTATION to "方向传感器"
    )

    /** 获取所有传感器列表 */
    fun getAllSensors(): List<SensorInfo> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                typeName = typeNameMap[sensor.type] ?: "未知传感器(${sensor.type})",
                vendor = sensor.vendor ?: "Unknown",
                version = sensor.version,
                power = sensor.power,
                resolution = sensor.resolution,
                maxRange = sensor.maximumRange,
                minDelay = sensor.minDelay,
                isDynamic = isDynamicSensor(sensor.type),
                isWakeUp = sensor.isWakeUpSensor,
                isAvailable = true
            )
        }
    }

    /** 按类型获取传感器 */
    fun getSensor(type: Int): Sensor? {
        return sensorManager.getDefaultSensor(type)
    }

    /** 订阅传感器实时数据流 */
    fun observeSensor(sensorType: Int, samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME): Flow<SensorData> {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        return callbackFlow {
            if (sensor == null) {
                close()
                return@callbackFlow
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == sensorType) {
                        trySend(
                            SensorData(
                                timestamp = event.timestamp,
                                values = event.values.clone(),
                                accuracy = event.accuracy
                            )
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }

            sensorManager.registerListener(
                listener, sensor,
                samplingPeriodUs
            )

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    /** 获取设备信息 */
    fun getDeviceInfo(): Map<String, String> {
        val info = linkedMapOf<String, String>()

        info["设备型号"] = "${Build.MANUFACTURER} ${Build.MODEL}"
        info["产品代号"] = Build.PRODUCT
        info["Android版本"] = Build.VERSION.RELEASE
        info["SDK级别"] = Build.VERSION.SDK_INT.toString()
        info["CPU架构"] = Build.SUPPORTED_ABIS.joinToString(", ")
        info["硬件"] = Build.HARDWARE

        // 内存
        val memInfo = getMemoryInfo()
        info["总内存"] = memInfo.first
        info["可用内存"] = memInfo.second

        // 传感器数量
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        info["传感器数量"] = "${allSensors.size}"

        // 电池（从系统文件读取）
        info["电池状态"] = getBatteryInfo()

        return info
    }

    private fun getMemoryInfo(): Pair<String, String> {
        try {
            val reader = BufferedReader(InputStreamReader(File("/proc/meminfo").inputStream()))
            var totalMem = 0L
            var availMem = 0L
            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("MemTotal:") -> {
                            totalMem = line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                        }
                        line.startsWith("MemAvailable:") -> {
                            availMem = line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                        }
                    }
                }
            }
            val totalGb = totalMem / 1024.0 / 1024.0
            val availGb = availMem / 1024.0 / 1024.0
            return Pair("%.1f GB".format(totalGb), "%.1f GB".format(availGb))
        } catch (_: Exception) {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.getMemoryInfo(memInfo)
            val totalGb = memInfo.totalMem / 1024.0 / 1024.0 / 1024.0
            val availGb = memInfo.availMem / 1024.0 / 1024.0 / 1024.0
            return Pair("%.1f GB".format(totalGb), "%.1f GB".format(availGb))
        }
    }

    private fun getBatteryInfo(): String {
        try {
            val intent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (scale > 0) (level * 100 / scale) else -1
            val statusStr = when (status) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                else -> "未知"
            }
            return "$statusStr ($pct%)"
        } catch (_: Exception) {
            return "无法获取"
        }
    }

    companion object {
        fun isDynamicSensor(type: Int): Boolean {
            return type in listOf(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_GRAVITY,
                Sensor.TYPE_LINEAR_ACCELERATION,
                Sensor.TYPE_ROTATION_VECTOR,
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
            )
        }
    }
}
