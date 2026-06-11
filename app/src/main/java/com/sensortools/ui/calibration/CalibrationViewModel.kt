package com.sensortools.ui.calibration

import android.app.Application
import android.hardware.Sensor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensortools.data.model.CalibrationData
import com.sensortools.data.repository.CalibrationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalibrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalibrationRepository(application)

    private val _accelCalibration = MutableStateFlow<CalibrationData?>(null)
    val accelCalibration: StateFlow<CalibrationData?> = _accelCalibration.asStateFlow()

    private val _accelProgress = MutableStateFlow(0)
    val accelProgress: StateFlow<Int> = _accelProgress.asStateFlow()

    private val _accelCalibrating = MutableStateFlow(false)
    val accelCalibrating: StateFlow<Boolean> = _accelCalibrating.asStateFlow()

    private val _gyroCalibration = MutableStateFlow<CalibrationData?>(null)
    val gyroCalibration: StateFlow<CalibrationData?> = _gyroCalibration.asStateFlow()

    private val _gyroProgress = MutableStateFlow(0)
    val gyroProgress: StateFlow<Int> = _gyroProgress.asStateFlow()

    private val _gyroCalibrating = MutableStateFlow(false)
    val gyroCalibrating: StateFlow<Boolean> = _gyroCalibrating.asStateFlow()

    private val _magCalibration = MutableStateFlow<CalibrationData?>(null)
    val magCalibration: StateFlow<CalibrationData?> = _magCalibration.asStateFlow()

    private val _magObserving = MutableStateFlow(false)
    val magObserving: StateFlow<Boolean> = _magObserving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var magJob: Job? = null

    init {
        loadSaved()
    }

    private fun loadSaved() {
        _accelCalibration.value = repository.getSavedCalibration(Sensor.TYPE_ACCELEROMETER)
        _gyroCalibration.value = repository.getSavedCalibration(Sensor.TYPE_GYROSCOPE)
        _magCalibration.value = repository.getSavedCalibration(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun startAccelCalibration() {
        if (_accelCalibrating.value) return
        _accelCalibrating.value = true
        _accelProgress.value = 0
        _statusMessage.value = "正在采集加速度计样本"

        viewModelScope.launch {
            val result = runCatching {
                repository.calibrateAccelerometer(100) { count, _ ->
                    _accelProgress.value = count
                }
            }.getOrElse { throwable ->
                _statusMessage.value = throwable.message ?: "加速度计校准失败"
                CalibrationData(Sensor.TYPE_ACCELEROMETER, quality = 0f, isComplete = false)
            }
            _accelCalibration.value = result
            _accelCalibrating.value = false
            _statusMessage.value = if (result.isComplete) "加速度计校准完成" else "加速度计校准未完成"
        }
    }

    fun startGyroCalibration() {
        if (_gyroCalibrating.value) return
        _gyroCalibrating.value = true
        _gyroProgress.value = 0
        _statusMessage.value = "正在采集陀螺仪零偏"

        viewModelScope.launch {
            val result = runCatching {
                repository.calibrateGyroscope(200) { count, _ ->
                    _gyroProgress.value = count
                }
            }.getOrElse { throwable ->
                _statusMessage.value = throwable.message ?: "陀螺仪校准失败"
                CalibrationData(Sensor.TYPE_GYROSCOPE, quality = 0f, isComplete = false)
            }
            _gyroCalibration.value = result
            _gyroCalibrating.value = false
            _statusMessage.value = if (result.isComplete) "陀螺仪校准完成" else "陀螺仪校准未完成"
        }
    }

    fun startMagCalibration() {
        if (_magObserving.value) return
        _magObserving.value = true
        _statusMessage.value = "请移动设备画 8 字，正在观察磁场变化"

        magJob?.cancel()
        magJob = viewModelScope.launch {
            try {
                repository.observeMagnetometerCalibration().collect { data ->
                    _magCalibration.value = data
                    if (data.isComplete) {
                        _magObserving.value = false
                        _statusMessage.value = "磁力计校准完成"
                    }
                }
            } catch (throwable: Throwable) {
                _statusMessage.value = throwable.message ?: "磁力计校准失败"
                _magObserving.value = false
            }
        }
    }

    fun stopMagCalibration() {
        magJob?.cancel()
        magJob = null
        _magObserving.value = false
        _statusMessage.value = "已停止磁力计校准"
    }
}
