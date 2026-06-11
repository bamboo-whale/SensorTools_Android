package com.sensortools.data.model

/**
 * 传感器实时数据采样点
 */
data class SensorData(
    val timestamp: Long,          // System.nanoTime()
    val values: FloatArray,       // 原始值数组
    val accuracy: Int = 0
) {
    val x: Float get() = if (values.size > 0) values[0] else 0f
    val y: Float get() = if (values.size > 1) values[1] else 0f
    val z: Float get() = if (values.size > 2) values[2] else 0f
    val scalar: Float get() {
        val v = values
        return if (v.isEmpty()) 0f else v[0]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorData) return false
        return timestamp == other.timestamp &&
                values.contentEquals(other.values) &&
                accuracy == other.accuracy
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        return result
    }
}
