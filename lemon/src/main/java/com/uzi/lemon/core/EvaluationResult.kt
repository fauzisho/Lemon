package com.uzi.lemon.core

import com.uzi.lemon.metrics.EnergyResult
import com.uzi.lemon.metrics.LatencyResult
import com.uzi.lemon.metrics.MemoryResult
import com.uzi.lemon.metrics.ThroughputResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Result of model evaluation containing all measured metrics
 * 
 * @param modelPath Path to the evaluated model
 * @param backend Backend used for evaluation
 * @param latency Latency measurement result (optional)
 * @param throughput Throughput measurement result (optional)
 * @param memory Memory measurement result (optional)
 * @param energy Energy measurement result (optional)
 * @param modelSize Model file size in bytes (optional)
 * @param timestamp Timestamp of evaluation
 */
@Serializable
data class EvaluationResult(
    val modelPath: String,
    val backend: String,
    val latency: LatencyResult? = null,
    val throughput: ThroughputResult? = null,
    val memory: MemoryResult? = null,
    val energy: EnergyResult? = null,
    val modelSize: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    
    /**
     * Export evaluation result to JSON file
     * 
     * @param outputPath Path to output JSON file
     */
    fun exportToJson(outputPath: String) {
        val json = Json { 
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        File(outputPath).writeText(json.encodeToString(this))
    }
    
    /**
     * Print a formatted report to console
     */
    fun printReport() {
        println("""
            ====================================
            🍋 Lemon Evaluation Report
            ====================================
            Model: $modelPath
            Backend: $backend
            Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)}
            ------------------------------------
            ${latency?.let { "📊 $it\n" } ?: ""}${throughput?.let { "⚡ $it\n" } ?: ""}${memory?.let { "💾 ${it.toMB()}\n" } ?: ""}${energy?.let { "🔋 $it\n" } ?: ""}${modelSize?.let { "📦 Model Size: ${it / 1024 / 1024} MB\n" } ?: ""}====================================
        """.trimIndent())
    }
    
    /**
     * Get a summary string of the evaluation
     */
    fun getSummary(): String {
        return buildString {
            append("Model: $modelPath | Backend: $backend")
            latency?.let { append(" | Latency: ${"%.2f".format(it.mean)}ms") }
            throughput?.let { append(" | Throughput: ${"%.1f".format(it.samplesPerSecond)} samples/s") }
            memory?.let { append(" | Memory: ${it.peakPss / 1024}MB") }
            energy?.let { append(" | Energy: ${"%.2f".format(it.energyConsumedMah)}mAh") }
        }
    }
}
