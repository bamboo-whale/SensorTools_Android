package com.sensortools.ui.detail

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensortools.data.model.CalibrationData
import com.sensortools.data.model.SensorData
import com.sensortools.data.model.SensorInfo
import com.sensortools.data.repository.SensorRepository
import com.sensortools.data.local.PreferencesManager
import com.sensortools.domain.SensorClassifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val prefs = PreferencesManager(application)

    private val _data = MutableStateFlow<List<SensorData>>(emptyList())
    val data: StateFlow<List<SensorData>> = _data.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _samplingRate = MutableStateFlow(0f)
    val samplingRate: StateFlow<Float> = _samplingRate.asStateFlow()

    private val _statusLabel = MutableStateFlow("等待中")
    val statusLabel: StateFlow<String> = _statusLabel.asStateFlow()

    private val _stats = MutableStateFlow(SensorStats())
    val stats: StateFlow<SensorStats> = _stats.asStateFlow()

    private var sensorJob: Job? = null
    private var lastTimestamp = 0L
    private var sampleCount = 0

    data class SensorStats(
        val currentX: Float = 0f,
        val currentY: Float = 0f,
        val currentZ: Float = 0f,
        val meanX: Float = 0f,
        val meanY: Float = 0f,
        val meanZ: Float = 0f,
        val minX: Float = Float.MAX_VALUE,
        val maxX: Float = Float.MIN_VALUE,
        val minY: Float = Float.MAX_VALUE,
        val maxY: Float = Float.MIN_VALUE,
        val minZ: Float = Float.MAX_VALUE,
        val maxZ: Float = Float.MIN_VALUE,
        val valueCount: Int = 0,
        val elapsedMs: Long = 0L
    )

    private var activeSensorType: Int = Sensor.TYPE_ACCELEROMETER
    private var activeSensorInfo: SensorInfo? = null

    fun getSensorInfo(): SensorInfo? = activeSensorInfo

    fun initSensor(sensorInfo: SensorInfo) {
        activeSensorType = sensorInfo.type
        activeSensorInfo = sensorInfo
        reset()
    }

    fun startListening() {
        if (_isRunning.value && !_isPaused.value) return
        if (_isPaused.value) {
            _isPaused.value = false
            return
        }

        _isRunning.value = true
        _isPaused.value = false
        sampleCount = 0
        lastTimestamp = 0L
        val startTime = System.currentTimeMillis()

        sensorJob = viewModelScope.launch {
            repository.observeSensor(activeSensorType, prefs.getSamplingPeriod())
                .collect { sensorData ->
                    if (_isPaused.value) return@collect

                    sampleCount++
                    val now = System.currentTimeMillis()
                    val elapsed = now - startTime

                    // 更新采样率（每 20 个样本计算一次）
                    if (sampleCount % 20 == 0 && lastTimestamp > 0 && sampleCount > 20) {
                        _samplingRate.value = 1000f / (elapsed.toFloat() / sampleCount)
                    }
                    lastTimestamp = now

                    // 更新数据缓冲
                    val buffer = _data.value.toMutableList()
                    buffer.add(sensorData)
                    if (buffer.size > 500) buffer.removeAt(0)
                    _data.value = buffer

                    // 更新统计
                    val s = _stats.value
                    _stats.value = s.copy(
                        currentX = sensorData.x,
                        currentY = sensorData.y,
                        currentZ = sensorData.z,
                        meanX = buffer.map { it.x }.average().toFloat(),
                        meanY = buffer.map { it.y }.average().toFloat(),
                        meanZ = buffer.map { it.z }.average().toFloat(),
                        minX = minOf(s.minX, sensorData.x),
                        maxX = maxOf(s.maxX, sensorData.x),
                        minY = minOf(s.minY, sensorData.y),
                        maxY = maxOf(s.maxY, sensorData.y),
                        minZ = minOf(s.minZ, sensorData.z),
                        maxZ = maxOf(s.maxZ, sensorData.z),
                        valueCount = sampleCount,
                        elapsedMs = elapsed
                    )

                    // 状态分类（每 10 个样本更新）
                    if (sampleCount % 10 == 0 && buffer.size >= 10) {
                        val recent = buffer.takeLast(50)
                        _statusLabel.value = SensorClassifier.classify(activeSensorType, recent)
                    }
                }
        }
    }

    fun pauseListening() {
        _isPaused.value = true
    }

    fun resumeListening() {
        _isPaused.value = false
    }

    fun reset() {
        sensorJob?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _data.value = emptyList()
        _stats.value = SensorStats()
        _statusLabel.value = "等待中"
        _samplingRate.value = 0f
        lastTimestamp = 0L
        sampleCount = 0
    }

    fun stopListening() {
        reset()
    }

    override fun onCleared() {
        super.onCleared()
        sensorJob?.cancel()
    }
}
