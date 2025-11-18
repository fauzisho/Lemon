package com.uzi.lemon.core

import android.content.Context
import com.uzi.lemon.metrics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue

/**
 * Main evaluator class for ExecuTorch models
 * 
 * This is the entry point for evaluating ExecuTorch models with Lemon.
 * It orchestrates the evaluation of multiple metrics based on the provided configuration.
 * 
 * Example usage:
 * ```
 * val config = PerformanceConfig.builder()
 *     .addMetric(MetricType.LATENCY)
 *     .addMetric(MetricType.MEMORY)
 *     .iterations(100)
 *     .build()
 * 
 * val evaluator = ModelEvaluator(context, config)
 * val result = evaluator.evaluate(modelPath, inputs)
 * result.printReport()
 * ```
 * 
 * @param context Android context
 * @param config Performance evaluation configuration
 */
class ModelEvaluator(
    private val context: Context,
    private val config: PerformanceConfig
) {
    
    /**
     * Evaluate a model with the configured metrics
     * 
     * @param modelPath Path to the .pte model file
     * @param inputs List of input arrays for evaluation
     * @return EvaluationResult containing all measured metrics
     */
    suspend fun evaluate(
        modelPath: String,
        inputs: List<Array<EValue>>
    ): EvaluationResult = withContext(Dispatchers.Default) {
        
        require(inputs.isNotEmpty()) { "Inputs list cannot be empty" }
        
        // Load the model
        val module = EvaluableModule(context, modelPath).load()
        
        try {
            // Measure each configured metric
            val results = mutableMapOf<MetricType, Any>()
            
            config.metrics.forEach { metricType ->
                when (metricType) {
                    MetricType.LATENCY -> {
                        val metric = LatencyMetric(
                            iterations = config.iterations,
                            warmupIterations = config.warmupIterations
                        )
                        results[metricType] = metric.measure(module, inputs)
                    }
                    
                    MetricType.MEMORY -> {
                        val metric = MemoryMetric(context)
                        results[metricType] = metric.measure(module, inputs)
                    }
                    
                    MetricType.THROUGHPUT -> {
                        val metric = ThroughputMetric(iterations = config.iterations)
                        results[metricType] = metric.measure(module, inputs)
                    }
                    
                    MetricType.ENERGY -> {
                        val metric = EnergyMetric(context)
                        results[metricType] = metric.measure(module, inputs)
                    }
                    
                    MetricType.MODEL_SIZE -> {
                        val metric = ModelSizeMetric()
                        results[metricType] = metric.measure(module, inputs)
                    }
                }
            }
            
            // Build evaluation result
            EvaluationResult(
                modelPath = modelPath,
                backend = config.backend.name,
                latency = results[MetricType.LATENCY] as? LatencyResult,
                memory = results[MetricType.MEMORY] as? MemoryResult,
                throughput = results[MetricType.THROUGHPUT] as? ThroughputResult,
                energy = results[MetricType.ENERGY] as? EnergyResult,
                modelSize = (results[MetricType.MODEL_SIZE] as? ModelSizeResult)?.sizeBytes
            )
        } finally {
            // Clean up resources
            module.release()
        }
    }
    
    /**
     * Compare two evaluation results
     * 
     * @param baseline Baseline evaluation result
     * @param optimized Optimized evaluation result
     * @return ComparisonReport containing the comparison
     */
    fun compare(
        baseline: EvaluationResult,
        optimized: EvaluationResult
    ): ComparisonReport {
        return ComparisonReport(baseline, optimized)
    }
}
