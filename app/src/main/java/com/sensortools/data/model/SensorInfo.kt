package com.sensortools.data.model

/**
 * 传感器静态信息
 */
data class SensorInfo(
    val name: String,
    val type: Int,
    val typeName: String,
    val vendor: String,
    val version: Int,
    val power: Float,         // mA
    val resolution: Float,
    val maxRange: Float,
    val minDelay: Int,         // μs
    val isDynamic: Boolean,    // 是否动态传感器（加速度计等）
    val isWakeUp: Boolean,
    val isAvailable: Boolean
)
