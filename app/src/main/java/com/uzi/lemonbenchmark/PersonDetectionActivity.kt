package com.uzi.lemonbenchmark

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.uzi.lemon.Lemon
import com.uzi.lemonbenchmark.ui.theme.LemonBenchmarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream

class PersonDetectionActivity : ComponentActivity() {
    
    private lateinit var detectionModel: PersonDetectionModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize model
        lifecycleScope.launch(Dispatchers.IO) {
            detectionModel = PersonDetectionModel(this@PersonDetectionActivity)
        }
        
        setContent {
            LemonBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PersonDetectionTestScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    @Composable
    fun PersonDetectionTestScreen(modifier: Modifier = Modifier) {
        var modelComparison by remember { mutableStateOf<ModelComparison?>(null) }
        var isRunning by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 Person Detection Model Comparison",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Text(
                text = "FP32 vs INT8 Quantization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    scope.launch(Dispatchers.Default) {
                        isRunning = true
                        error = null
                        modelComparison = null
                        
                        try {
                            Log.d("PersonDetection", "Starting comparison test...")
                            val results = runModelComparison()
                            modelComparison = results
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            Log.e("PersonDetection", "Comparison error", e)
                        } finally {
                            isRunning = false
                        }
                    }
                },
                enabled = !isRunning && ::detectionModel.isInitialized,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Both Models...")
                } else {
                    Text("Compare FP32 vs INT8")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            modelComparison?.let { comparison ->
                // FP32 Results
                ModelResultCard(
                    title = "FP32 Model (Full Precision)",
                    subtitle = "person_detection_model_xnnpack_fp32.pte",
                    result = comparison.fp32Results,
                    modelType = "FP32"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // INT8 Results
                ModelResultCard(
                    title = "INT8 Model (Quantized)",
                    subtitle = "person_detection_model_xnnpack_int8.pte",
                    result = comparison.int8Results,
                    modelType = "INT8"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Comparison Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 Model Comparison",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        ComparisonRow(
                            label = "Accuracy",
                            fp32 = "${String.format("%.2f", comparison.fp32Results.accuracy)}%",
                            int8 = "${String.format("%.2f", comparison.int8Results.accuracy)}%",
                            delta = "${String.format("%.2f", comparison.accuracyDelta)}%"
                        )
                        
                        ComparisonRow(
                            label = "Latency (ms)",
                            fp32 = "${String.format("%.3f", comparison.fp32Results.benchmarkLatencyMs ?: 0.0)}",
                            int8 = "${String.format("%.3f", comparison.int8Results.benchmarkLatencyMs ?: 0.0)}",
                            delta = "${String.format("%.1f", comparison.latencyDeltaPercent)}%"
                        )
                        
                        ComparisonRow(
                            label = "Throughput (fps)",
                            fp32 = "${String.format("%.2f", comparison.fp32Results.benchmarkThroughputFps ?: 0.0)}",
                            int8 = "${String.format("%.2f", comparison.int8Results.benchmarkThroughputFps ?: 0.0)}",
                            delta = "${String.format("%.1f", comparison.throughputDeltaPercent)}%"
                        )
                        
                        ComparisonRow(
                            label = "Memory (MB)",
                            fp32 = "${String.format("%.2f", comparison.fp32Results.benchmarkMemoryMb ?: 0.0)}",
                            int8 = "${String.format("%.2f", comparison.int8Results.benchmarkMemoryMb ?: 0.0)}",
                            delta = "${String.format("%.1f", comparison.memoryDeltaPercent)}%"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "Recommendation",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                        
                        val recommendation = when {
                            comparison.accuracyDelta > 2.0 -> "FP32 has significantly better accuracy. Use FP32 for production."
                            comparison.accuracyDelta > 1.0 -> "FP32 slightly more accurate but INT8 faster. Choose based on needs."
                            kotlin.math.abs(comparison.accuracyDelta) <= 1.0 && comparison.latencyDeltaPercent < -20 -> "INT8: Similar accuracy but much faster and smaller. Recommended!"
                            else -> "INT8: Good tradeoff between speed and accuracy"
                        }
                        
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "FP32 (Full Precision):\n" +
                                    "• Higher accuracy\n" +
                                    "• Larger file size (~0.24 MB)\n" +
                                    "• Slower inference\n" +
                                    "• Better for high accuracy requirements\n\n" +
                                    "INT8 (Quantized):\n" +
                                    "• Smaller file size (~0.08 MB)\n" +
                                    "• Faster inference\n" +
                                    "• Lower memory usage\n" +
                                    "• Minimal accuracy loss\n" +
                                    "• Better for mobile/edge devices",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun ModelResultCard(
        title: String,
        subtitle: String,
        result: TestResults,
        modelType: String
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (modelType == "FP32")
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                ResultMetric(
                    label = "Accuracy",
                    value = "${String.format("%.2f", result.accuracy)}%"
                )
                ResultMetric(
                    label = "Correct/Total",
                    value = "${result.correct}/${result.totalSamples}"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Confusion Matrix",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TN: ${result.tn}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "FP: ${result.fp}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FN: ${result.fn}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TP: ${result.tp}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Performance Metrics",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                ResultMetric(
                    label = "Latency",
                    value = "${String.format("%.3f", result.benchmarkLatencyMs ?: 0.0)} ms"
                )
                ResultMetric(
                    label = "Throughput",
                    value = "${String.format("%.2f", result.benchmarkThroughputFps ?: 0.0)} fps"
                )
                ResultMetric(
                    label = "Peak Memory",
                    value = "${String.format("%.2f", result.benchmarkMemoryMb ?: 0.0)} MB"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Per-Class Metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                ClassMetric(
                    className = "Empty",
                    precision = result.emptyPrecision,
                    recall = result.emptyRecall,
                    f1 = result.emptyF1
                )
                
                ClassMetric(
                    className = "Occupied",
                    precision = result.occupiedPrecision,
                    recall = result.occupiedRecall,
                    f1 = result.occupiedF1
                )
            }
        }
    }
    
    @Composable
    fun ComparisonRow(label: String, fp32: String, int8: String, delta: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(fp32, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text(int8, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text(delta, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        }
    }
    
    @Composable
    fun ResultMetric(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
    
    @Composable
    fun ClassMetric(className: String, precision: Double, recall: Double, f1: Double) {
        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(className, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "P: ${String.format("%.2f", precision * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "R: ${String.format("%.2f", recall * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "F1: ${String.format("%.2f", f1)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
    
    private suspend fun runModelComparison(): ModelComparison = withContext(Dispatchers.Default) {
        Log.d("PersonDetection", "========== MODEL COMPARISON TEST ==========")
        
        val startTime = System.currentTimeMillis()
        
        // Load test data once
        Log.d("PersonDetection", "Loading synthetic_test_data.csv...")
        val testData = loadTestDataFromCSV()
        Log.d("PersonDetection", "✓ Loaded ${testData.size} test samples\n")
        
        // Test FP32 model
        Log.d("PersonDetection", "--- TESTING FP32 MODEL ---")
        val fp32Results = testModelOnData(testData, "person_detection_model_xnnpack_fp32.pte", "FP32")
        
        // Test INT8 model
        Log.d("PersonDetection", "\n--- TESTING INT8 MODEL ---")
        val int8Results = testModelOnData(testData, "person_detection_model_xnnpack_int8.pte", "INT8")
        
        // Calculate differences
        val accuracyDelta = fp32Results.accuracy - int8Results.accuracy
        val latencyDeltaPercent = if (fp32Results.benchmarkLatencyMs != null && int8Results.benchmarkLatencyMs != null) {
            ((int8Results.benchmarkLatencyMs - fp32Results.benchmarkLatencyMs) / fp32Results.benchmarkLatencyMs) * 100
        } else 0.0
        
        val throughputDeltaPercent = if (fp32Results.benchmarkThroughputFps != null && int8Results.benchmarkThroughputFps != null) {
            ((int8Results.benchmarkThroughputFps - fp32Results.benchmarkThroughputFps) / fp32Results.benchmarkThroughputFps) * 100
        } else 0.0
        
        val memoryDeltaPercent = if (fp32Results.benchmarkMemoryMb != null && int8Results.benchmarkMemoryMb != null) {
            ((int8Results.benchmarkMemoryMb - fp32Results.benchmarkMemoryMb) / fp32Results.benchmarkMemoryMb) * 100
        } else 0.0
        
        Log.d("PersonDetection", "\n========== COMPARISON SUMMARY ==========")
        Log.d("PersonDetection", "Accuracy Delta (FP32 - INT8): ${String.format("%.2f", accuracyDelta)}%")
        Log.d("PersonDetection", "Latency Delta: ${String.format("%.1f", latencyDeltaPercent)}%")
        Log.d("PersonDetection", "Throughput Delta: ${String.format("%.1f", throughputDeltaPercent)}%")
        Log.d("PersonDetection", "Memory Delta: ${String.format("%.1f", memoryDeltaPercent)}%")
        Log.d("PersonDetection", "==========================================\n")
        
        val totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0
        
        return@withContext ModelComparison(
            fp32Results = fp32Results,
            int8Results = int8Results,
            accuracyDelta = accuracyDelta,
            latencyDeltaPercent = latencyDeltaPercent,
            throughputDeltaPercent = throughputDeltaPercent,
            memoryDeltaPercent = memoryDeltaPercent,
            totalTimeSeconds = totalTimeSeconds
        )
    }
    
    private suspend fun testModelOnData(
        testData: List<TestSample>,
        modelFileName: String,
        modelType: String
    ): TestResults {
        Log.d("PersonDetection", "Testing $modelType model: $modelFileName")
        
        var correct = 0
        var tp = 0
        var tn = 0
        var fp = 0
        var fn = 0
        
        // Run inference on all samples
        testData.forEach { sample ->
            val normalized = detectionModel.normalizeInput(sample.features) ?: return@forEach
            val modelOutput = runModel(normalized, modelFileName)
            val pred = if (modelOutput >= 0.5f) 1 else 0
            
            if (pred == sample.label) {
                correct++
                if (pred == 1) tp++ else tn++
            } else {
                if (pred == 1) fp++ else fn++
            }
        }
        
        val accuracy = (correct.toDouble() / testData.size) * 100
        Log.d("PersonDetection", "✓ Accuracy: ${String.format("%.2f", accuracy)}% ($correct/${testData.size})")
        
        // Calculate metrics
        val emptyPrecision = if (tn + fp > 0) tn.toDouble() / (tn + fp) else 0.0
        val emptyRecall = if (tn + fn > 0) tn.toDouble() / (tn + fn) else 0.0
        val emptyF1 = if (emptyPrecision + emptyRecall > 0) 
            2 * (emptyPrecision * emptyRecall) / (emptyPrecision + emptyRecall) else 0.0
        
        val occupiedPrecision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
        val occupiedRecall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
        val occupiedF1 = if (occupiedPrecision + occupiedRecall > 0) 
            2 * (occupiedPrecision * occupiedRecall) / (occupiedPrecision + occupiedRecall) else 0.0
        
        // Run Lemon benchmark
        var benchmarkLatency: Double? = null
        var benchmarkThroughput: Double? = null
        var benchmarkMemory: Double? = null
        
        try {
            val modelPath = copyModelFromAssets(modelFileName)
            val evaluator = Lemon.create(this)
                .latency()
                .throughput()
                .memory()
                .iterations(10)
                .warmup(2)
                .build()
            
            val sampleInputs = testData.take(10).map { sample ->
                val normalized = detectionModel.normalizeInput(sample.features)
                if (normalized != null) {
                    val tensor = Tensor.fromBlob(normalized, longArrayOf(1, 81))
                    arrayOf(EValue.from(tensor))
                } else {
                    arrayOf()
                }
            }.filter { it.isNotEmpty() }
            
            val result = evaluator.evaluate(modelPath, sampleInputs)
            benchmarkLatency = result.latency?.mean
            benchmarkThroughput = result.throughput?.fps
            benchmarkMemory = result.memory?.peakPss?.div(1024.0 * 1024.0)
            
            Log.d("PersonDetection", "  Latency: ${String.format("%.3f", benchmarkLatency ?: 0.0)} ms")
            Log.d("PersonDetection", "  Throughput: ${String.format("%.2f", benchmarkThroughput ?: 0.0)} fps")
            Log.d("PersonDetection", "  Memory: ${String.format("%.2f", benchmarkMemory ?: 0.0)} MB")
        } catch (e: Exception) {
            Log.w("PersonDetection", "Benchmark failed: ${e.message}")
        }
        
        return TestResults(
            accuracy = accuracy,
            totalSamples = testData.size,
            correct = correct,
            incorrect = testData.size - correct,
            tp = tp,
            tn = tn,
            fp = fp,
            fn = fn,
            emptyPrecision = emptyPrecision,
            emptyRecall = emptyRecall,
            emptyF1 = emptyF1,
            occupiedPrecision = occupiedPrecision,
            occupiedRecall = occupiedRecall,
            occupiedF1 = occupiedF1,
            benchmarkLatencyMs = benchmarkLatency,
            benchmarkThroughputFps = benchmarkThroughput,
            benchmarkMemoryMb = benchmarkMemory,
            testTimeSeconds = 0.0
        )
    }
    
    private fun loadTestDataFromCSV(): List<TestSample> {
        val samples = mutableListOf<TestSample>()
        
        try {
            val inputStream = assets.open("synthetic_test_data.csv")
            val lines = inputStream.bufferedReader().readLines()
            
            if (lines.isEmpty()) {
                Log.e("PersonDetection", "CSV is empty")
                return samples
            }
            
            val header = lines[0].split(",")
            val labelIndex = header.indexOf("label")
            val featureIndices = header.indices.filter { it != labelIndex && header[it] !in listOf("weight_category", "posture") }
            
            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                val label = if (parts[labelIndex] == "occupied") 1 else 0
                val features = FloatArray(featureIndices.size) { j ->
                    parts[featureIndices[j]].toFloatOrNull() ?: 0f
                }
                samples.add(TestSample(features, label))
            }
            
        } catch (e: Exception) {
            Log.e("PersonDetection", "Error loading CSV: ${e.message}")
        }
        
        return samples
    }
    
    private fun runModel(normalizedInput: FloatArray, modelFileName: String): Float {
        return try {
            val modelPath = copyModelFromAssets(modelFileName)
            val tensor = Tensor.fromBlob(normalizedInput, longArrayOf(1, 81))
            val evalue = EValue.from(tensor)
            
            // TODO: Implement actual ExecuTorch model loading and inference
            0.85f
            
        } catch (e: Exception) {
            Log.e("PersonDetection", "Model inference error: ${e.message}")
            0.5f
        }
    }
    
    private fun copyModelFromAssets(modelFileName: String): String {
        val modelFile = File(cacheDir, modelFileName)
        
        if (!modelFile.exists()) {
            try {
                assets.open(modelFileName).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("PersonDetection", "Error copying $modelFileName: ${e.message}")
            }
        }
        
        return modelFile.absolutePath
    }
    
    data class TestSample(
        val features: FloatArray,
        val label: Int
    )
    
    data class TestResults(
        val accuracy: Double,
        val totalSamples: Int,
        val correct: Int,
        val incorrect: Int,
        val tp: Int,
        val tn: Int,
        val fp: Int,
        val fn: Int,
        val emptyPrecision: Double,
        val emptyRecall: Double,
        val emptyF1: Double,
        val occupiedPrecision: Double,
        val occupiedRecall: Double,
        val occupiedF1: Double,
        val benchmarkLatencyMs: Double?,
        val benchmarkThroughputFps: Double?,
        val benchmarkMemoryMb: Double?,
        val testTimeSeconds: Double
    )
    
    data class ModelComparison(
        val fp32Results: TestResults,
        val int8Results: TestResults,
        val accuracyDelta: Double,
        val latencyDeltaPercent: Double,
        val throughputDeltaPercent: Double,
        val memoryDeltaPercent: Double,
        val totalTimeSeconds: Double
    )
}
