package com.sensortools.ui.health

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensortools.data.model.SensorData
import com.sensortools.data.model.SensorInfo
import com.sensortools.data.repository.SensorRepository
import com.sensortools.domain.HealthAnalyzer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val sensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _results = MutableStateFlow<List<HealthItem>>(emptyList())
    val results: StateFlow<List<HealthItem>> = _results.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentSensor = MutableStateFlow("")
    val currentSensor: StateFlow<String> = _currentSensor.asStateFlow()

    private val _scanNote = MutableStateFlow("准备就绪")
    val scanNote: StateFlow<String> = _scanNote.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    data class HealthItem(
        val sensor: SensorInfo,
        val status: HealthAnalyzer.HealthStatus,
        val reason: String,
        val details: Map<String, String> = emptyMap()
    )

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _results.value = emptyList()
        _progress.value = 0f
        _currentSensor.value = ""
        _scanNote.value = "正在枚举设备上的全部传感器"
        _scanError.value = null

        viewModelScope.launch(Dispatchers.Main) {
            val sensors = runCatching { repository.getAllSensors() }
                .getOrElse { throwable ->
                    _scanError.value = throwable.message ?: "读取传感器列表失败"
                    emptyList()
                }
            val total = sensors.size.coerceAtLeast(1)
            val results = mutableListOf<HealthItem>()

            if (sensors.isEmpty()) {
                _scanNote.value = "未发现可用传感器"
            }

            for ((index, sensor) in sensors.withIndex()) {
                _progress.value = index.toFloat() / total
                _currentSensor.value = sensor.name
                _scanNote.value = "检查 ${sensor.typeName}"

                if (sensor.isDynamic) {
                    val status = runCatching { checkOneSensor(sensor) }
                        .getOrElse { throwable ->
                            HealthItem(
                                sensor,
                                HealthAnalyzer.HealthStatus.SUSPECT,
                                throwable.message ?: "检测失败",
                                mapOf("异常" to (throwable::class.simpleName ?: "Unknown"))
                            )
                        }
                    results.add(status)
                } else {
                    results.add(
                        HealthItem(
                            sensor,
                            HealthAnalyzer.HealthStatus.NORMAL,
                            "静态传感器，工作正常",
                            mapOf("类型" to sensor.typeName, "厂商" to sensor.vendor)
                        )
                    )
                }

                // 让 UI 有时间渲染
                delay(100)
            }

            _results.value = results
            _isScanning.value = false
            _progress.value = 1f
            _currentSensor.value = ""
            _scanNote.value = if (results.isEmpty()) "扫描结束，但没有可展示的结果" else "扫描完成，共 ${results.size} 项"
        }
    }

    /**
     * 检测单个传感器 — Handler 超时 + suspendCancellableCoroutine。
     * 采集至少 500ms 数据用于分析。
     */
    private suspend fun checkOneSensor(sensor: SensorInfo): HealthItem =
        suspendCancellableCoroutine { cont ->
            val type = sensor.type
            val deviceSensor = sensorManager.getDefaultSensor(type)

            if (deviceSensor == null) {
                if (cont.isActive) {
                    cont.resume(
                        HealthItem(
                            sensor,
                            HealthAnalyzer.HealthStatus.NO_DATA,
                            "传感器不存在或系统未开放读取",
                            mapOf("状态" to "未检测到硬件传感器")
                        )
                    )
                }
                return@suspendCancellableCoroutine
            }

            val samples = mutableListOf<SensorData>()
            val startTime = System.currentTimeMillis()
            var done = false
            val minSamples = 15
            val maxDuration = 2500L
            lateinit var listener: SensorEventListener

            val timeoutRunnable = Runnable {
                if (!done) {
                    done = true
                    sensorManager.unregisterListener(listener)
                    val dur = System.currentTimeMillis() - startTime
                    val result = if (samples.isEmpty()) {
                        HealthItem(
                            sensor,
                            HealthAnalyzer.HealthStatus.NO_DATA,
                            "未收到传感器数据",
                            mapOf("采样数" to "0", "超时" to "${maxDuration}ms")
                        )
                    } else {
                        val hr = HealthAnalyzer.analyze(samples, type, sensor.isDynamic, dur)
                        HealthItem(sensor, hr.status, hr.reason, hr.details + ("采样数" to samples.size.toString()))
                    }
                    if (cont.isActive) cont.resume(result)
                }
            }
            mainHandler.postDelayed(timeoutRunnable, maxDuration)

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (done || event.sensor.type != type) return
                    samples.add(SensorData(event.timestamp, event.values.clone(), event.accuracy))
                    val elapsed = System.currentTimeMillis() - startTime
                    if (samples.size >= minSamples && elapsed > 500) {
                        done = true
                        mainHandler.removeCallbacks(timeoutRunnable)
                        sensorManager.unregisterListener(listener)
                        val result = HealthAnalyzer.analyze(samples, type, sensor.isDynamic, elapsed)
                        if (cont.isActive) {
                            cont.resume(
                                HealthItem(
                                    sensor,
                                    result.status,
                                    result.reason,
                                    result.details + ("采样数" to samples.size.toString())
                                )
                            )
                        }
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }

            sensorManager.registerListener(listener, deviceSensor, SensorManager.SENSOR_DELAY_GAME)

            cont.invokeOnCancellation {
                mainHandler.removeCallbacks(timeoutRunnable)
                sensorManager.unregisterListener(listener)
                done = true
            }
        }
}
