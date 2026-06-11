package com.sensortools.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.sensortools.data.model.CalibrationData
import com.sensortools.data.model.SensorData
import com.sensortools.data.local.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CalibrationRepository(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = PreferencesManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun calibrateAccelerometer(
        sampleCount: Int = 100,
        onProgress: (Int, Float) -> Unit
    ): CalibrationData {
        val data = collectSamples(Sensor.TYPE_ACCELEROMETER, sampleCount, onProgress, 20000L)
        if (data.isEmpty()) return CalibrationData(Sensor.TYPE_ACCELEROMETER, quality = 0f)
        return computeCalibration(Sensor.TYPE_ACCELEROMETER, data)
    }

    suspend fun calibrateGyroscope(
        sampleCount: Int = 200,
        onProgress: (Int, Float) -> Unit
    ): CalibrationData {
        val data = collectSamples(Sensor.TYPE_GYROSCOPE, sampleCount, onProgress, 20000L)
        if (data.isEmpty()) return CalibrationData(Sensor.TYPE_GYROSCOPE, quality = 0f)
        return computeCalibration(Sensor.TYPE_GYROSCOPE, data)
    }

    fun getSavedCalibration(sensorType: Int): CalibrationData? =
        prefs.getCalibration(sensorType)

    fun observeMagnetometerCalibration(): Flow<CalibrationData> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensor == null) {
            close(IllegalStateException("当前设备不支持磁力计"))
            return@callbackFlow
        }
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
                val x = event.values[0]; val y = event.values[1]
                val z = if (event.values.size > 2) event.values[2] else 0f
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                val quality = ((maxX - minX + maxY - minY + maxZ - minZ) / 300f).coerceIn(0f, 1f)
                trySend(CalibrationData(Sensor.TYPE_MAGNETIC_FIELD,
                    (minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f,
                    quality, quality > 0.85f))
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    // ── 核心采集：Handler 超时 + suspendCancellableCoroutine，无水合 ──

    private suspend fun collectSamples(
        sensorType: Int,
        targetCount: Int,
        onProgress: (Int, Float) -> Unit,
        timeoutMs: Long
    ): List<SensorData> = suspendCancellableCoroutine { cont ->
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            cont.resumeWithException(IllegalStateException("当前设备不支持该传感器"))
            return@suspendCancellableCoroutine
        }

        val samples = mutableListOf<SensorData>()
        var done = false
        lateinit var listener: SensorEventListener

        // 超时 Runnable
        val timeoutRunnable = Runnable {
            if (!done) {
                done = true
                sensorManager.unregisterListener(listener)
                cont.resume(samples.toList())
            }
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (done || event.sensor.type != sensorType) return
                samples.add(SensorData(event.timestamp, event.values.clone(), event.accuracy))
                onProgress(samples.size, samples.size.toFloat() / targetCount)
                if (samples.size >= targetCount) {
                    done = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    sensorManager.unregisterListener(listener)
                    cont.resume(samples.toList())
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)

        cont.invokeOnCancellation {
            mainHandler.removeCallbacks(timeoutRunnable)
            sensorManager.unregisterListener(listener)
            if (!done) {
                done = true
                // Already cancelled, no resume needed
            }
        }
    }

    private fun computeCalibration(sensorType: Int, data: List<SensorData>): CalibrationData {
        val isAccel = sensorType == Sensor.TYPE_ACCELEROMETER
        val meanX = data.map { it.values[0] }.average().toFloat()
        val meanY = data.map { it.values[1] }.average().toFloat()
        val meanZ = data.map {
            if (it.values.size > 2) { if (isAccel) it.values[2] - 9.81f else it.values[2] } else 0f
        }.average().toFloat()
        val varX = data.map { (it.values[0] - meanX).let { v -> v * v } }.average()
        val varY = data.map { (it.values[1] - meanY).let { v -> v * v } }.average()
        val varZ = data.map {
            val raw = if (it.values.size > 2) it.values[2] else 0f
            val base = if (isAccel) raw - 9.81f else raw
            (base - meanZ).let { v -> v * v }
        }.average()
        val quality = (1f - ((varX + varY + varZ) / 3.0 / 2.0f).toFloat().coerceIn(0f, 1f))
        val result = CalibrationData(sensorType, meanX, meanY, meanZ, quality, true)
        prefs.saveCalibration(result)
        return result
    }
}
