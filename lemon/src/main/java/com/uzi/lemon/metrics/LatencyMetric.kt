package com.uzi.lemon.metrics

import com.uzi.lemon.core.EvaluableModule
import com.uzi.lemon.core.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Result of latency measurement
 */
@Serializable
data class LatencyResult(
    val mean: Double,
    val median: Double,
    val p50: Double,
    val p95: Double,
    val p99: Double,
    val min: Double,
    val max: Double,
    val stdDev: Double,
    override val unit: String = "ms"
) : Metric {
    override val name: String = "Latency"
    
    override fun toString(): String {
        return """
            Latency Statistics:
              Mean: ${"%.3f".format(mean)} $unit
              Median: ${"%.3f".format(median)} $unit
              P95: ${"%.3f".format(p95)} $unit
              P99: ${"%.3f".format(p99)} $unit
              Min: ${"%.3f".format(min)} $unit
              Max: ${"%.3f".format(max)} $unit
              StdDev: ${"%.3f".format(stdDev)} $unit
        """.trimIndent()
    }
}

/**
 * Measures model inference latency
 * 
 * @param iterations Number of inference iterations to measure
 * @param warmupIterations Number of warmup iterations before measurement
 */
class LatencyMetric(
    private val iterations: Int = 100,
    private val warmupIterations: Int = 10
) {
    
    /**
     * Measure latency for the given module and inputs
     * 
     * @param module The evaluable module to measure
     * @param inputs List of input arrays to test with
     * @return LatencyResult containing statistics
     */
    suspend fun measure(
        module: EvaluableModule,
        inputs: List<Array<EValue>>
    ): LatencyResult = withContext(Dispatchers.Default) {
        
        require(inputs.isNotEmpty()) { "Inputs list cannot be empty" }
        
        // Warmup phase
        repeat(warmupIterations) {
            val input = inputs.random()
            module.forward(*input)
        }
        
        // Actual measurement
        val latencies = mutableListOf<Long>()
        
        repeat(iterations) {
            val input = inputs[it % inputs.size]
            
            // Use System.nanoTime() for precise measurement
            val startTime = System.nanoTime()
            module.forward(*input)
            val endTime = System.nanoTime()
            
            latencies.add(endTime - startTime)
        }
        
        // Calculate statistics
        calculateStatistics(latencies)
    }
    
    private fun calculateStatistics(latencies: List<Long>): LatencyResult {
        val sorted = latencies.sorted()
        val mean = latencies.average()
        
        return LatencyResult(
            mean = mean / 1_000_000.0,  // Convert nanoseconds to milliseconds
            median = sorted[sorted.size / 2] / 1_000_000.0,
            p50 = sorted[sorted.size / 2] / 1_000_000.0,
            p95 = sorted[(sorted.size * 0.95).toInt()] / 1_000_000.0,
            p99 = sorted[(sorted.size * 0.99).toInt()] / 1_000_000.0,
            min = sorted.first() / 1_000_000.0,
            max = sorted.last() / 1_000_000.0,
            stdDev = calculateStdDev(latencies, mean)
        )
    }
    
    private fun calculateStdDev(values: List<Long>, mean: Double): Double {
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance) / 1_000_000.0
    }
}
