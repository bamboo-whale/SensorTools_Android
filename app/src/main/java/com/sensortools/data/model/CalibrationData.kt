package com.sensortools.data.model

/**
 * 校准结果数据
 */
data class CalibrationData(
    val sensorType: Int,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val quality: Float = 0f,     // 0.0 ~ 1.0
    val isComplete: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
