package com.sensortools.domain

import android.hardware.Sensor
import com.sensortools.data.model.SensorData

/**
 * 传感器状态分类器：根据实时数据判断设备/环境状态
 */
object SensorClassifier {

    // ── 加速度计 ──
    fun classifyAccelerometer(data: List<SensorData>): String {
        if (data.isEmpty()) return "无数据"
        val magnitudes = data.map { sd ->
            kotlin.math.sqrt(sd.x * sd.x + sd.y * sd.y + sd.z * sd.z)
        }
        val avgMag = magnitudes.average().toFloat()
        val varMag = magnitudes.map { (it - avgMag) * (it - avgMag) }.average().toFloat()

        return when {
            varMag < 0.02f -> "静止"
            varMag < 0.5f -> "轻微移动"
            varMag < 5.0f -> "运动"
            varMag < 20.0f -> "剧烈晃动"
            else -> "冲击"
        }
    }

    // ── 陀螺仪 ──
    fun classifyGyroscope(data: List<SensorData>): String {
        if (data.isEmpty()) return "无数据"
        val magnitudes = data.map { sd ->
            kotlin.math.sqrt(sd.x * sd.x + sd.y * sd.y + sd.z * sd.z)
        }
        val avgMag = magnitudes.average().toFloat()

        return when {
            avgMag < 0.05f -> "静止"
            avgMag < 1.0f -> "旋转中"
            avgMag < 5.0f -> "快速旋转"
            else -> "高速旋转"
        }
    }

    // ── 光线传感器 ──
    fun classifyLight(data: List<SensorData>): String {
        if (data.isEmpty()) return "无数据"
        val avgLux = data.map { it.scalar }.average().toFloat()

        return when {
            avgLux < 1f -> "黑暗"
            avgLux < 100f -> "室内弱光"
            avgLux < 1000f -> "室内明亮"
            avgLux < 10000f -> "室外"
            else -> "阳光直射"
        }
    }

    // ── 距离传感器 ──
    fun classifyProximity(data: List<SensorData>): String {
        if (data.isEmpty()) return "无数据"
        val avgDist = data.map { it.scalar }.average().toFloat()

        return when {
            avgDist < 1f -> "近距离"
            avgDist < 5f -> "中距离"
            else -> "远距离"
        }
    }

    // ── 磁力计 ──
    fun classifyMagnetometer(data: List<SensorData>): String {
        if (data.isEmpty()) return "无数据"
        val magnitudes = data.map { sd ->
            kotlin.math.sqrt(sd.x * sd.x + sd.y * sd.y + sd.z * sd.z)
        }
        val avgMag = magnitudes.average().toFloat()

        return when {
            avgMag < 20f -> "弱磁场"
            avgMag < 60f -> "正常磁场"
            avgMag < 100f -> "强磁场"
            else -> "极强磁场（附近有磁铁）"
        }
    }

    /** 通用分类入口 */
    fun classify(sensorType: Int, data: List<SensorData>): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_LINEAR_ACCELERATION -> classifyAccelerometer(data)

            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> classifyGyroscope(data)

            Sensor.TYPE_LIGHT -> classifyLight(data)
            Sensor.TYPE_PROXIMITY -> classifyProximity(data)

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> classifyMagnetometer(data)

            else -> if (data.isEmpty()) "无数据" else "数据正常"
        }
    }
}
