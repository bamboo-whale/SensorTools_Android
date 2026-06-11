package com.sensortools.ui.record

import android.app.Application
import android.hardware.SensorManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensortools.data.local.PreferencesManager
import com.sensortools.data.model.SensorInfo
import com.sensortools.data.repository.SensorRepository
import com.sensortools.data.service.SensorRecordingService
import com.sensortools.util.ExportManager
import com.sensortools.data.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)
    private val prefs = PreferencesManager(application)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _exportFile = MutableStateFlow<File?>(null)
    val exportFile: StateFlow<File?> = _exportFile.asStateFlow()

    private val _sensors = MutableStateFlow<List<SensorInfo>>(emptyList())
    val sensors: StateFlow<List<SensorInfo>> = _sensors.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _listeningSensor = MutableStateFlow<SensorInfo?>(null)
    val listeningSensor: StateFlow<SensorInfo?> = _listeningSensor.asStateFlow()

    private val _listeningData = MutableStateFlow<List<SensorData>>(emptyList())
    val listeningData: StateFlow<List<SensorData>> = _listeningData.asStateFlow()

    private val _listeningIsRunning = MutableStateFlow(false)
    val listeningIsRunning: StateFlow<Boolean> = _listeningIsRunning.asStateFlow()

    private val _listeningStatus = MutableStateFlow("未监听")
    val listeningStatus: StateFlow<String> = _listeningStatus.asStateFlow()

    private val allRecords = mutableListOf<ExportManager.ExportRecord>()
    private var startTime = 0L
    private var activeSensor: SensorInfo? = null
    private var lastAnnotation = ""
    private var listenJob: kotlinx.coroutines.Job? = null
    private var selectedListeningSensor: SensorInfo? = null

    init {
        _sensors.value = repository.getAllSensors()

        SensorRecordingService.onRecordCallback = { record ->
            allRecords.add(record)
            _recordCount.value = allRecords.size
            _duration.value = System.currentTimeMillis() - startTime
        }

        SensorRecordingService.onStopCallback = { records ->
            allRecords.clear()
            allRecords.addAll(records)
            _recordCount.value = allRecords.size
            _isRecording.value = false
        }
    }

    fun selectSensor(sensor: SensorInfo) {
        selectedListeningSensor = sensor
        _listeningSensor.value = sensor
    }

    fun startListeningPreview() {
        val sensor = selectedListeningSensor ?: return
        listenJob?.cancel()
        _listeningIsRunning.value = true
        _listeningStatus.value = "监听中"
        listenJob = viewModelScope.launch {
            repository.observeSensor(sensor.type, prefs.getSamplingPeriod()).collect { data ->
                val buffer = (_listeningData.value + data).takeLast(300)
                _listeningData.value = buffer
                _listeningStatus.value = "监听中 · ${buffer.size} 条"
            }
        }
    }

    fun pauseListeningPreview() {
        listenJob?.cancel()
        listenJob = null
        _listeningIsRunning.value = false
        _listeningStatus.value = "已暂停"
    }

    fun stopListeningPreview() {
        listenJob?.cancel()
        listenJob = null
        _listeningIsRunning.value = false
        _listeningData.value = emptyList()
        _listeningStatus.value = "未监听"
    }

    fun startRecordingWithCountdown(sensor: SensorInfo, countdownSeconds: Int = 0) {
        if (_isRecording.value) return
        activeSensor = sensor
        allRecords.clear()
        _recordCount.value = 0
        _duration.value = 0L
        lastAnnotation = ""

        if (countdownSeconds > 0) {
            _countdown.value = countdownSeconds
            viewModelScope.launch {
                for (i in countdownSeconds downTo 1) {
                    _countdown.value = i
                    kotlinx.coroutines.delay(1000L)
                }
                _countdown.value = 0
                doStartRecording()
            }
        } else {
            doStartRecording()
        }
    }

    private fun doStartRecording() {
        val sensor = activeSensor ?: return
        _isRecording.value = true
        startTime = System.currentTimeMillis()

        val samplingPeriod = prefs.getSamplingPeriod()
        SensorRecordingService.start(
            getApplication(),
            sensor.type,
            sensor.name,
            samplingPeriod
        )
    }

    fun addAnnotation(text: String) {
        lastAnnotation = text
        // Send annotation intent to service
        val intent = android.content.Intent(getApplication(), SensorRecordingService::class.java).apply {
            action = SensorRecordingService.ACTION_ANNOTATE
            putExtra(SensorRecordingService.EXTRA_ANNOTATION, text)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopRecording() {
        SensorRecordingService.stop(getApplication())
        _isRecording.value = false
        _recordCount.value = allRecords.size
        _duration.value = System.currentTimeMillis() - startTime
    }

    fun exportCsv() {
        if (allRecords.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val file = ExportManager.exportCsv(getApplication(), allRecords.toList())
            // Also save metadata
            exportMetadata()
            _exportFile.value = file
        }
    }

    fun exportJson() {
        if (allRecords.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = buildMetadata()
            val file = ExportManager.exportJson(getApplication(), allRecords.toList(), metadata)
            _exportFile.value = file
        }
    }

    private fun exportMetadata() {
        val metadata = buildMetadata()
        val dir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
            ?: getApplication<Application>().filesDir
        val fileDateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        val metaFile = java.io.File(dir, "sensor_data_${fileDateFormat.format(java.util.Date())}_meta.json")
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"device_model\": \"${metadata.deviceModel}\",\n")
        sb.append("  \"android_version\": \"${metadata.androidVersion}\",\n")
        sb.append("  \"sensor_name\": \"${metadata.sensorName}\",\n")
        sb.append("  \"sensor_type\": ${metadata.sensorType},\n")
        sb.append("  \"sensor_vendor\": \"${metadata.sensorVendor}\",\n")
        sb.append("  \"sensor_resolution\": ${metadata.sensorResolution},\n")
        sb.append("  \"sensor_max_range\": ${metadata.sensorMaxRange},\n")
        sb.append("  \"sampling_rate\": \"${metadata.samplingRate}\",\n")
        sb.append("  \"start_time\": ${metadata.startTime},\n")
        sb.append("  \"end_time\": ${metadata.endTime},\n")
        sb.append("  \"total_records\": ${metadata.totalRecords}\n")
        sb.append("}\n")
        metaFile.writeText(sb.toString())
    }

    private fun buildMetadata(): ExportManager.SessionMetadata {
        val sensor = activeSensor
        return ExportManager.SessionMetadata(
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            androidVersion = android.os.Build.VERSION.RELEASE,
            sensorName = sensor?.name ?: "Unknown",
            sensorType = sensor?.type ?: 0,
            sensorVendor = sensor?.vendor ?: "Unknown",
            sensorResolution = sensor?.resolution ?: 0f,
            sensorMaxRange = sensor?.maxRange ?: 0f,
            samplingRate = getSamplingRateLabel(),
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            totalRecords = allRecords.size,
            annotations = allRecords.map { it.annotation }.filter { it.isNotBlank() }.distinct()
        )
    }

    private fun getSamplingRateLabel(): String {
        return when (prefs.getSamplingPeriod()) {
            SensorManager.SENSOR_DELAY_NORMAL -> "NORMAL (~5Hz)"
            SensorManager.SENSOR_DELAY_UI -> "UI (~15Hz)"
            SensorManager.SENSOR_DELAY_GAME -> "GAME (~50Hz)"
            SensorManager.SENSOR_DELAY_FASTEST -> "FASTEST"
            else -> "Custom"
        }
    }

    fun shareFile() {
        val file = _exportFile.value ?: return
        val mime = if (file.extension == "csv") "text/csv" else "application/json"
        ExportManager.shareFile(getApplication(), file, mime)
    }

    fun openExportFolder() {
        val file = _exportFile.value ?: return
        ExportManager.openExportDirectory(getApplication(), file)
    }

    fun clearExport() {
        _exportFile.value = null
    }

    override fun onCleared() {
        super.onCleared()
        SensorRecordingService.onRecordCallback = null
        SensorRecordingService.onStopCallback = null
        listenJob?.cancel()
    }
}
