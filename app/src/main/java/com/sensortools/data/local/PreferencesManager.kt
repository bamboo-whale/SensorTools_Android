package com.sensortools.data.local

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sensortools.data.model.CalibrationData

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("sensor_calibration", Context.MODE_PRIVATE)
    private val themePrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // ── 校准数据 ──
    fun saveCalibration(data: CalibrationData) {
        prefs.edit()
            .putFloat("offsetX_${data.sensorType}", data.offsetX)
            .putFloat("offsetY_${data.sensorType}", data.offsetY)
            .putFloat("offsetZ_${data.sensorType}", data.offsetZ)
            .putFloat("quality_${data.sensorType}", data.quality)
            .putBoolean("complete_${data.sensorType}", data.isComplete)
            .putLong("timestamp_${data.sensorType}", data.timestamp)
            .apply()
    }

    fun getCalibration(sensorType: Int): CalibrationData? {
        val key = "complete_${sensorType}"
        if (!prefs.contains(key)) return null
        return CalibrationData(
            sensorType = sensorType,
            offsetX = prefs.getFloat("offsetX_${sensorType}", 0f),
            offsetY = prefs.getFloat("offsetY_${sensorType}", 0f),
            offsetZ = prefs.getFloat("offsetZ_${sensorType}", 0f),
            quality = prefs.getFloat("quality_${sensorType}", 0f),
            isComplete = prefs.getBoolean("complete_${sensorType}", false),
            timestamp = prefs.getLong("timestamp_${sensorType}", 0L)
        )
    }

    fun clearCalibration(sensorType: Int) {
        prefs.edit()
            .remove("offsetX_${sensorType}")
            .remove("offsetY_${sensorType}")
            .remove("offsetZ_${sensorType}")
            .remove("quality_${sensorType}")
            .remove("complete_${sensorType}")
            .remove("timestamp_${sensorType}")
            .apply()
    }

    // ── 采样率 ──
    fun getSamplingPeriod(): Int = themePrefs.getInt("sampling_period", SensorManager.SENSOR_DELAY_GAME)
    fun setSamplingPeriod(period: Int) {
        themePrefs.edit().putInt("sampling_period", period).apply()
    }

    fun getSamplingPeriodLabel(): String = when (getSamplingPeriod()) {
        SensorManager.SENSOR_DELAY_NORMAL -> "NORMAL (~5 Hz)"
        SensorManager.SENSOR_DELAY_UI -> "UI (~15 Hz)"
        SensorManager.SENSOR_DELAY_GAME -> "GAME (~50 Hz)"
        SensorManager.SENSOR_DELAY_FASTEST -> "FASTEST"
        else -> "自定义"
    }

    // ── 图表点数 ──
    fun getChartMaxPoints(): Int = themePrefs.getInt("chart_max_points", 500)
    fun setChartMaxPoints(points: Int) {
        themePrefs.edit().putInt("chart_max_points", points).apply()
    }

    // ── 首次启动 ──
    fun isFirstLaunch(): Boolean = themePrefs.getBoolean("first_launch", true)
    fun setFirstLaunchDone() {
        themePrefs.edit().putBoolean("first_launch", false).apply()
    }
}
