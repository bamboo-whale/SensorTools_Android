package com.sensortools.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    data class ExportRecord(
        val timestamp: Long,
        val sensorName: String,
        val x: Float,
        val y: Float,
        val z: Float,
        val accuracy: Int = 0,
        val annotation: String = ""
    )

    data class SessionMetadata(
        val deviceModel: String,
        val androidVersion: String,
        val sensorName: String,
        val sensorType: Int,
        val sensorVendor: String,
        val sensorResolution: Float,
        val sensorMaxRange: Float,
        val samplingRate: String,
        val startTime: Long,
        val endTime: Long,
        val totalRecords: Int,
        val annotations: List<String>
    )

    fun exportCsv(context: Context, records: List<ExportRecord>): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val fileName = "sensor_data_${fileDateFormat.format(Date())}.csv"
        val file = File(dir, fileName)

        FileWriter(file).use { writer ->
            writer.append("\uFEFF") // BOM for Excel Chinese support
            writer.append("timestamp,sensor_name,x,y,z,accuracy,annotation\n")
            records.forEach { r ->
                writer.append("${dateFormat.format(Date(r.timestamp))},${r.sensorName},${r.x},${r.y},${r.z},${r.accuracy},${r.annotation}\n")
            }
        }
        return file
    }

    fun exportJson(context: Context, records: List<ExportRecord>, metadata: SessionMetadata? = null): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val baseName = "sensor_data_${fileDateFormat.format(Date())}"
        val file = File(dir, "$baseName.json")

        val sb = StringBuilder()
        sb.append("{\n")

        // 元数据
        if (metadata != null) {
            sb.append("  \"metadata\": {\n")
            sb.append("    \"device_model\": \"${metadata.deviceModel}\",\n")
            sb.append("    \"android_version\": \"${metadata.androidVersion}\",\n")
            sb.append("    \"sensor_name\": \"${metadata.sensorName}\",\n")
            sb.append("    \"sensor_type\": ${metadata.sensorType},\n")
            sb.append("    \"sensor_vendor\": \"${metadata.sensorVendor}\",\n")
            sb.append("    \"sensor_resolution\": ${metadata.sensorResolution},\n")
            sb.append("    \"sensor_max_range\": ${metadata.sensorMaxRange},\n")
            sb.append("    \"sampling_rate\": \"${metadata.samplingRate}\",\n")
            sb.append("    \"start_time\": \"${dateFormat.format(Date(metadata.startTime))}\",\n")
            sb.append("    \"end_time\": \"${dateFormat.format(Date(metadata.endTime))}\",\n")
            sb.append("    \"total_records\": ${metadata.totalRecords},\n")
            sb.append("    \"annotations\": ${metadata.annotations.joinToString(", ") { "\"$it\"" }}\n")
            sb.append("  },\n")
        }

        sb.append("  \"records\": [\n")
        records.forEachIndexed { i, r ->
            sb.append("    {\n")
            sb.append("      \"time\": \"${dateFormat.format(Date(r.timestamp))}\",\n")
            sb.append("      \"sensor\": \"${r.sensorName}\",\n")
            sb.append("      \"x\": ${r.x},\n")
            sb.append("      \"y\": ${r.y},\n")
            sb.append("      \"z\": ${r.z},\n")
            sb.append("      \"accuracy\": ${r.accuracy},\n")
            sb.append("      \"annotation\": \"${r.annotation}\"\n")
            sb.append("    }")
            if (i < records.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        file.writeText(sb.toString())

        // Also write standalone metadata JSON
        if (metadata != null) {
            val metaFile = File(dir, "${baseName}_meta.json")
            val metaSb = StringBuilder()
            metaSb.append("{\n")
            metaSb.append("  \"device_model\": \"${metadata.deviceModel}\",\n")
            metaSb.append("  \"android_version\": \"${metadata.androidVersion}\",\n")
            metaSb.append("  \"sensor_name\": \"${metadata.sensorName}\",\n")
            metaSb.append("  \"sensor_type\": ${metadata.sensorType},\n")
            metaSb.append("  \"sensor_vendor\": \"${metadata.sensorVendor}\",\n")
            metaSb.append("  \"sensor_resolution\": ${metadata.sensorResolution},\n")
            metaSb.append("  \"sensor_max_range\": ${metadata.sensorMaxRange},\n")
            metaSb.append("  \"sampling_rate\": \"${metadata.samplingRate}\",\n")
            metaSb.append("  \"start_time\": \"${dateFormat.format(Date(metadata.startTime))}\",\n")
            metaSb.append("  \"end_time\": \"${dateFormat.format(Date(metadata.endTime))}\",\n")
            metaSb.append("  \"total_records\": ${metadata.totalRecords},\n")
            metaSb.append("  \"annotations\": ${metadata.annotations.joinToString(", ") { "\"$it\"" }}\n")
            metaSb.append("}\n")
            metaFile.writeText(metaSb.toString())
        }

        return file
    }

    private fun mimeTypeFor(file: File): String = when (file.extension.lowercase()) {
        "csv" -> "text/csv"
        "json" -> "application/json"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            ?: "application/octet-stream"
    }

    private fun fileProviderUri(context: Context, file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    fun shareFile(context: Context, file: File, mimeType: String): Boolean {
        if (!file.exists()) return false
        return try {
            val uri = fileProviderUri(context, file)
            val resolvedMime = mimeType.ifBlank { mimeTypeFor(file) }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "分享数据").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun openExportDirectory(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val dir = file.parentFile ?: return false
        return tryOpenDirectory(context, dir)
    }

    private fun tryOpenDirectory(context: Context, dir: File): Boolean {
        return try {
            val uri = fileProviderUri(context, dir)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "打开保存位置"))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
