package com.sensortools.domain

import android.hardware.Sensor
import com.sensortools.data.model.SensorData

/**
 * 传感器健康状态分析器
 */
object HealthAnalyzer {

    enum class HealthStatus {
        NORMAL,      // 正常
        ABNORMAL,    // 异常
        SUSPECT,     // 疑似故障
        NO_DATA      // 无数据
    }

    data class HealthResult(
        val status: HealthStatus,
        val reason: String,
        val details: Map<String, String> = emptyMap()
    )

    /**
     * 综合分析传感器健康状态
     * @param data 最近采集的数据（至少需要 20 个采样点）
     * @param sensorType 传感器类型
     * @param isDynamic 是否为动态传感器
     * @param samplingDurationMs 采样持续时间（ms）
     */
    fun analyze(
        data: List<SensorData>,
        sensorType: Int,
        isDynamic: Boolean,
        samplingDurationMs: Long
    ): HealthResult {
        // 1. 无数据检查
        if (data.isEmpty()) {
            return HealthResult(HealthStatus.NO_DATA, "传感器无数据返回，可能被禁用或硬件故障")
        }

        // 2. 采样过少
        if (data.size < 5) {
            return HealthResult(
                HealthStatus.SUSPECT,
                "采样数据过少（${data.size} 个采样点），可能采样频率异常"
            )
        }

        val reasons = mutableListOf<String>()
        val details = mutableMapOf<String, String>()

        // 3. 长时间固定不变检查（仅动态传感器）
        if (isDynamic && data.size >= 10) {
            val recent = data.takeLast(10)
            val firstVal = recent.first()
            val allSame = recent.all { sd ->
                sd.x == firstVal.x && sd.y == firstVal.y && sd.z == firstVal.z
            }
            if (allSame) {
                reasons.add("动态传感器数据长时间未变化，可能卡死")
            }

            // 计算方差
            val meanX = data.map { it.x }.average().toFloat()
            val varX = data.map { (it.x - meanX) * (it.x - meanX) }.average().toFloat()
            if (varX < 0.0001f && data.size > 50) {
                reasons.add("数据方差极小（var=%.6f），疑似传感器冻结".format(varX))
            }
            details["方差(X)"] = "%.6f".format(varX)
        }

        // 4. 异常跳变检查
        if (data.size >= 3) {
            var spikeCount = 0
            val maxExpected = getMaxExpectedJump(sensorType)
            for (i in 1 until data.size) {
                val d = kotlin.math.sqrt(
                    (data[i].x - data[i - 1].x).let { it * it } +
                    (data[i].y - data[i - 1].y).let { it * it } +
                    (data[i].z - data[i - 1].z).let { it * it }
                )
                if (d > maxExpected) spikeCount++
            }
            val spikeRate = spikeCount.toFloat() / data.size
            if (spikeRate > 0.3f) {
                reasons.add("异常跳变比例过高（%.0f%%），可能传感器硬件故障".format(spikeRate * 100))
            }
            details["跳变比例"] = "%.1f%%".format(spikeRate * 100)
            details["跳变次数"] = "$spikeCount / ${data.size}"
        }

        // 5. 采样频率异常检查
        if (samplingDurationMs > 0 && data.size >= 2) {
            val avgInterval = samplingDurationMs.toFloat() / data.size
            details["平均采样间隔"] = "%.1f ms".format(avgInterval)

            val expectedMin = 5f // 5ms 最小间隔
            if (avgInterval > 200f && isDynamic) {
                reasons.add("采样间隔过大（%.0f ms），可能系统负载过高或传感器异常".format(avgInterval))
            }
        }

        // 6. 值域检查
        val maxRange = getExpectedRange(sensorType)
        val outOfRange = data.count { sd ->
            kotlin.math.abs(sd.x) > maxRange || kotlin.math.abs(sd.y) > maxRange || kotlin.math.abs(sd.z) > maxRange
        }
        if (outOfRange > data.size * 0.1) {
            reasons.add("数据超出预期范围（>%.1f），可能传感器故障".format(maxRange))
            details["超范围比例"] = "%.0f%%".format(outOfRange.toFloat() / data.size * 100)
        }

        val status = when {
            reasons.isEmpty() -> HealthStatus.NORMAL
            reasons.size == 1 && !reasons.any { it.contains("故障") } -> HealthStatus.ABNORMAL
            reasons.any { it.contains("故障") || it.contains("卡死") || it.contains("冻结") } -> HealthStatus.SUSPECT
            else -> HealthStatus.ABNORMAL
        }

        return HealthResult(
            status = status,
            reason = if (reasons.isEmpty()) "传感器工作正常" else reasons.joinToString("；"),
            details = details
        )
    }

    private fun getMaxExpectedJump(sensorType: Int): Float = when (sensorType) {
        Sensor.TYPE_ACCELEROMETER -> 30f       // m/s²
        Sensor.TYPE_GYROSCOPE -> 20f            // rad/s
        Sensor.TYPE_MAGNETIC_FIELD -> 200f       // μT
        Sensor.TYPE_LIGHT -> 5000f               // lux
        Sensor.TYPE_PRESSURE -> 50f              // hPa
        Sensor.TYPE_PROXIMITY -> 10f             // cm
        else -> 100f
    }

    private fun getExpectedRange(sensorType: Int): Float = when (sensorType) {
        Sensor.TYPE_ACCELEROMETER -> 20f         // ±2g ≈ 19.6 m/s², some up to ±16g
        Sensor.TYPE_GYROSCOPE -> 35f             // rad/s
        Sensor.TYPE_MAGNETIC_FIELD -> 1000f       // μT
        Sensor.TYPE_LIGHT -> 120000f              // lux (direct sunlight)
        Sensor.TYPE_PRESSURE -> 1100f             // hPa
        Sensor.TYPE_PROXIMITY -> 100f             // cm
        else -> 1000f
    }
}
