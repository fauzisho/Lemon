package com.uzi.lemon.metrics

import com.uzi.lemon.core.EvaluableModule
import com.uzi.lemon.core.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.pytorch.executorch.EValue

/**
 * Result of throughput measurement
 */
@Serializable
data class ThroughputResult(
    val samplesPerSecond: Double,
    val totalSamples: Int,
    val totalTimeMs: Double,
    override val unit: String = "samples/sec"
) : Metric {
    override val name: String = "Throughput"
    
    override fun toString(): String {
        return """
            Throughput Statistics:
              Throughput: ${"%.2f".format(samplesPerSecond)} $unit
              Total Samples: $totalSamples
              Total Time: ${"%.2f".format(totalTimeMs)} ms
        """.trimIndent()
    }
}

/**
 * Measures model inference throughput (samples per second)
 * 
 * @param iterations Number of inference iterations to measure
 */
class ThroughputMetric(
    private val iterations: Int = 100
) {
    
    /**
     * Measure throughput for the given module and inputs
     * 
     * @param module The evaluable module to measure
     * @param inputs List of input arrays to test with
     * @return ThroughputResult containing throughput statistics
     */
    suspend fun measure(
        module: EvaluableModule,
        inputs: List<Array<EValue>>
    ): ThroughputResult = withContext(Dispatchers.Default) {
        
        require(inputs.isNotEmpty()) { "Inputs list cannot be empty" }
        
        val startTime = System.nanoTime()
        
        // Run all iterations
        repeat(iterations) {
            val input = inputs[it % inputs.size]
            module.forward(*input)
        }
        
        val endTime = System.nanoTime()
        val totalTimeMs = (endTime - startTime) / 1_000_000.0
        val samplesPerSecond = (iterations * 1000.0) / totalTimeMs
        
        ThroughputResult(
            samplesPerSecond = samplesPerSecond,
            totalSamples = iterations,
            totalTimeMs = totalTimeMs
        )
    }
}
