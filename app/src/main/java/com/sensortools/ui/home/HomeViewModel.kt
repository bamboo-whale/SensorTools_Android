package com.sensortools.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensortools.data.model.SensorInfo
import com.sensortools.data.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)

    private val _commonSensors = MutableStateFlow<List<SensorInfo>>(emptyList())
    val commonSensors: StateFlow<List<SensorInfo>> = _commonSensors.asStateFlow()

    private val _unknownSensors = MutableStateFlow<List<SensorInfo>>(emptyList())
    val unknownSensors: StateFlow<List<SensorInfo>> = _unknownSensors.asStateFlow()

    private val _deviceInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, String>> = _deviceInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val all = repository.getAllSensors()
            _commonSensors.value = all.filter { !it.typeName.startsWith("未知传感器") }
            _unknownSensors.value = all.filter { it.typeName.startsWith("未知传感器") }
            _deviceInfo.value = repository.getDeviceInfo()
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadData()
    }

    fun getSensorCountByCategory(): Map<String, Int> {
        val all = _commonSensors.value + _unknownSensors.value
        return all.groupBy { categoryKey(it) }.mapValues { it.value.size }
    }

    fun totalSensorCount(): Int = _commonSensors.value.size + _unknownSensors.value.size

    private fun categoryKey(sensor: SensorInfo): String = when (sensor.type) {
        android.hardware.Sensor.TYPE_ACCELEROMETER,
        android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
        android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
        android.hardware.Sensor.TYPE_GRAVITY,
        android.hardware.Sensor.TYPE_GYROSCOPE,
        android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
        android.hardware.Sensor.TYPE_ROTATION_VECTOR,
        android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
        android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "运动与姿态"

        android.hardware.Sensor.TYPE_LIGHT,
        android.hardware.Sensor.TYPE_PROXIMITY,
        android.hardware.Sensor.TYPE_PRESSURE,
        android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE,
        android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY,
        android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
        android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "环境感知"

        android.hardware.Sensor.TYPE_STEP_COUNTER,
        android.hardware.Sensor.TYPE_STEP_DETECTOR,
        android.hardware.Sensor.TYPE_HEART_RATE -> "人体与计步"

        else -> "其他传感器"
    }
}
