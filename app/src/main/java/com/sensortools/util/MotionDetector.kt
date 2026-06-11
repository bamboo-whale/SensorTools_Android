package com.sensortools.util

import com.sensortools.data.model.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 运动状态识别：基于加速度计数据的运动模式检测
 * 使用简单阈值 + 方差分析
 */
object MotionDetector {

    enum class MotionState {
        STATIONARY,   // 静止
        WALKING,      // 步行
        RUNNING,      // 跑步
        CYCLING       // 骑行
    }

    /**
     * 分析最近的数据窗口，识别运动状态
     */
    fun detectMotion(data: List<SensorData>): MotionState {
        if (data.size < 20) return MotionState.STATIONARY

        // 分离重力分量后可更精确，这里用总加速度幅值
        val magnitudes = data.map { sd ->
            sqrt(sd.x * sd.x + sd.y * sd.y + sd.z * sd.z)
        }

        val meanMag = magnitudes.average().toFloat()
        val varMag = magnitudes.map { (it - meanMag) * (it - meanMag) }.average().toFloat()
        val stdMag = sqrt(varMag)

        // 峰值频率分析（简化版：计数过零点）
        val zeroCrossings = countZeroCrossings(magnitudes.map { it - meanMag })

        return when {
            stdMag < 0.3f -> MotionState.STATIONARY
            stdMag < 2.0f && zeroCrossings < magnitudes.size * 0.3f -> MotionState.WALKING
            stdMag < 6.0f && zeroCrossings >= magnitudes.size * 0.3f -> MotionState.RUNNING
            stdMag < 4.0f -> MotionState.CYCLING
            else -> MotionState.RUNNING
        }
    }

    private fun countZeroCrossings(signal: List<Float>): Int {
        var count = 0
        for (i in 1 until signal.size) {
            if (signal[i - 1] * signal[i] < 0) count++
        }
        return count
    }

    fun getMotionLabel(state: MotionState): String = when (state) {
        MotionState.STATIONARY -> "静止"
        MotionState.WALKING -> "步行"
        MotionState.RUNNING -> "跑步"
        MotionState.CYCLING -> "骑行"
    }
}
