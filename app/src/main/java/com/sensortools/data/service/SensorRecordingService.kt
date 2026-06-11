package com.sensortools.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sensortools.MainActivity
import com.sensortools.data.model.SensorData
import com.sensortools.util.ExportManager

class SensorRecordingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var activeSensorType: Int = Sensor.TYPE_ACCELEROMETER
    private var samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME
    private var sensorName: String = ""

    private val records = mutableListOf<ExportManager.ExportRecord>()
    private var startTime: Long = 0L
    private var annotation: String = ""

    companion object {
        const val CHANNEL_ID = "sensor_recording"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SENSOR_TYPE = "sensor_type"
        const val EXTRA_SENSOR_NAME = "sensor_name"
        const val EXTRA_SAMPLING_PERIOD = "sampling_period"
        const val ACTION_STOP = "com.sensortools.STOP_RECORDING"
        const val ACTION_ANNOTATE = "com.sensortools.ANNOTATE"
        const val EXTRA_ANNOTATION = "annotation"

        var onRecordCallback: ((ExportManager.ExportRecord) -> Unit)? = null
        var onStopCallback: ((List<ExportManager.ExportRecord>) -> Unit)? = null

        fun start(context: Context, sensorType: Int, sensorName: String, samplingPeriodUs: Int) {
            val intent = Intent(context, SensorRecordingService::class.java).apply {
                putExtra(EXTRA_SENSOR_TYPE, sensorType)
                putExtra(EXTRA_SENSOR_NAME, sensorName)
                putExtra(EXTRA_SAMPLING_PERIOD, samplingPeriodUs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorRecordingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SensorTools:RecordingWakeLock"
        )
        wakeLock.acquire(24 * 60 * 60 * 1000L) // max 24h
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_STOP -> stopSelf()
                ACTION_ANNOTATE -> {
                    annotation = intent.getStringExtra(EXTRA_ANNOTATION) ?: ""
                }
                else -> {
                    activeSensorType = intent.getIntExtra(EXTRA_SENSOR_TYPE, Sensor.TYPE_ACCELEROMETER)
                    sensorName = intent.getStringExtra(EXTRA_SENSOR_NAME) ?: "Unknown"
                    samplingPeriodUs = intent.getIntExtra(EXTRA_SAMPLING_PERIOD, SensorManager.SENSOR_DELAY_GAME)
                    if (!checkSensor()) return START_NOT_STICKY
                    startRecording()
                }
            }
        }

        val stopIntent = Intent(this, SensorRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SensorTools 录制中")
            .setContentText("正在记录 $sensorName")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startTime = System.currentTimeMillis()

        return START_STICKY
    }

    private fun startRecording() {
        val sensor = sensorManager.getDefaultSensor(activeSensorType)
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, samplingPeriodUs)
        }
    }

    private var hasValidSensor: Boolean = false

    private fun checkSensor(): Boolean {
        val sensor = sensorManager.getDefaultSensor(activeSensorType)
        hasValidSensor = sensor != null
        if (!hasValidSensor) {
            stopSelf()
        }
        return hasValidSensor
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != activeSensorType) return
        val values = event.values.clone()
        val record = ExportManager.ExportRecord(
            timestamp = System.currentTimeMillis(),
            sensorName = sensorName,
            x = if (values.size > 0) values[0] else 0f,
            y = if (values.size > 1) values[1] else 0f,
            z = if (values.size > 2) values[2] else 0f,
            accuracy = event.accuracy,
            annotation = annotation
        )
        records.add(record)
        onRecordCallback?.invoke(record)

        // 更新通知
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SensorTools 录制中")
            .setContentText("${records.size} 条 | ${elapsed}s | $sensorName")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        onStopCallback?.invoke(records.toList())
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "传感器录制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台传感器数据录制通知"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
