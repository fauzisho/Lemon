package com.uzi.lemonbenchmark

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uzi.lemon.Lemon
import com.uzi.lemon.core.EvaluationResult
import com.uzi.lemonbenchmark.ui.theme.LemonBenchmarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream

data class ModelBenchmarkResult(
    val modelName: String,
    val result: EvaluationResult?,
    val error: String?
)

class MultiBenchmarkActivity : ComponentActivity() {
    private val TAG = "MultiBenchmark"

    // Start with portable models only (no XNNPACK/Vulkan for now)
    // This will help us isolate the issue
    private val modelList = listOf(
        // CPU Models - Portable only (safest, no optimizations)
//        "mv2_portable_fp32.pte",
//        "mv3_portable_fp32.pte",
//        "mv2_xnnpack_fp32.pte",
//        "mv2_xnnpack_int8_pt2e.pte",
//        "mv3_xnnpack_fp32.pte",
//        "mv3_xnnpack_int8_pt2e.pte",
//         "mv2_vulkan_fp16.pte",
//        "mv2_vulkan_fp32.pte",
        "mv3_vulkan_fp32.pte",
//        "mv3_vulkan_fp16.pte",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LemonBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MultiBenchmarkScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun MultiBenchmarkScreen(modifier: Modifier = Modifier) {
        var isRunning by remember { mutableStateOf(false) }
        var currentModel by remember { mutableStateOf("") }
        var progress by remember { mutableStateOf(0) }
        var results by remember { mutableStateOf<List<ModelBenchmarkResult>>(emptyList()) }
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
                text = "🍋 Multi-Model Benchmark",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Benchmark ${modelList.size} Models",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        progress = 0
                        results = emptyList()

                        val benchmarkResults = mutableListOf<ModelBenchmarkResult>()

                        modelList.forEachIndexed { index, modelName ->
                            currentModel = modelName
                            Log.d(
                                TAG,
                                "Benchmarking model ${index + 1}/${modelList.size}: $modelName"
                            )

                            try {
                                val result = runBenchmark(modelName)
                                benchmarkResults.add(
                                    ModelBenchmarkResult(
                                        modelName = modelName,
                                        result = result,
                                        error = null
                                    )
                                )
                                Log.d(TAG, "✓ Successfully benchmarked $modelName")
                            } catch (e: Exception) {
                                val errorMsg = e.message ?: "Unknown error"
                                Log.e(TAG, "✗ Error benchmarking $modelName: $errorMsg", e)

                                // Check if it's a Vulkan backend error
                                val isVulkanError = errorMsg.contains("VulkanBackend") ||
                                        errorMsg.contains("Backend") ||
                                        modelName.contains("vulkan", ignoreCase = true)

                                benchmarkResults.add(
                                    ModelBenchmarkResult(
                                        modelName = modelName,
                                        result = null,
                                        error = if (isVulkanError) {
                                            "Vulkan backend not available"
                                        } else {
                                            errorMsg
                                        }
                                    )
                                )
                            }

                            progress = index + 1
                            results = benchmarkResults.toList()
                        }

                        isRunning = false
                        currentModel = ""
                    }
                },
                enabled = !isRunning,
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
                    Text("Running... ($progress/${modelList.size})")
                } else {
                    Text("Start Benchmark All Models")
                }
            }

            if (isRunning && currentModel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Currently benchmarking:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currentModel,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress.toFloat() / modelList.size,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display results
            if (results.isNotEmpty()) {
                Text(
                    text = "Benchmark Results",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Summary comparison table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 Summary Comparison",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Model",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "Memory",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Latency",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "FPS",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Data rows
                        results.forEach { modelResult ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = modelResult.modelName.replace(".pte", ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(2f)
                                )

                                if (modelResult.error != null) {
                                    Text(
                                        text = "Error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(3f)
                                    )
                                } else {
                                    modelResult.result?.let { result ->
                                        val memoryMB = result.memory?.peakPss?.div(1024.0)
                                        val latencyMs = result.latency?.mean
                                        val fps = result.throughput?.fps

                                        Text(
                                            text = memoryMB?.let { String.format("%.1f MB", it) }
                                                ?: "N/A",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = latencyMs?.let { String.format("%.2f ms", it) }
                                                ?: "N/A",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = fps?.let { String.format("%.1f", it) } ?: "N/A",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detailed results for each model
                results.forEach { modelResult ->
                    ModelResultCard(modelResult)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    @Composable
    fun ModelResultCard(modelResult: ModelBenchmarkResult) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (modelResult.error != null)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = modelResult.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (modelResult.error != null) {
                    Text(
                        text = "Error: ${modelResult.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    modelResult.result?.let { result ->
                        // Memory metrics
                        result.memory?.let { memory ->
                            Text(
                                text = "💾 Memory",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            MetricRow(
                                "Peak PSS",
                                "${String.format("%.2f", memory.peakPss / 1024.0)} MB"
                            )
                            MetricRow(
                                "Delta PSS",
                                "${String.format("%.2f", memory.deltaPss / 1024.0)} MB"
                            )
                            MetricRow(
                                "Native Heap",
                                "${String.format("%.2f", memory.nativeHeap / (1024.0 * 1024.0))} MB"
                            )
                        }

                        // Latency metrics
                        result.latency?.let { latency ->
                            Text(
                                text = "⏱️ Latency",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            MetricRow("Mean", "${String.format("%.3f", latency.mean)} ms")
                            MetricRow("Median", "${String.format("%.3f", latency.median)} ms")
                            MetricRow("P95", "${String.format("%.3f", latency.p95)} ms")
                            MetricRow("StdDev", "${String.format("%.3f", latency.stdDev)} ms")
                        }

                        // Throughput metrics
                        result.throughput?.let { throughput ->
                            Text(
                                text = "🚀 Throughput",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            MetricRow("FPS", String.format("%.2f", throughput.fps))
                            MetricRow(
                                "Samples/sec",
                                String.format("%.2f", throughput.samplesPerSecond)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MetricRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }

    private suspend fun runBenchmark(modelName: String): EvaluationResult =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting benchmark for $modelName...")

            // Copy model from assets to cache directory
            val modelPath = copyAssetToCache(modelName)
            Log.d(TAG, "Model path: $modelPath")

            // Determine input shape based on model type (MobileNetV2 or V3)
            val inputData = createRandomInput(1, 3, 224, 224)
            val inputTensor = Tensor.fromBlob(
                inputData,
                longArrayOf(1, 3, 224, 224)
            )
            val inputEValue = EValue.from(inputTensor)
            val inputs = listOf(arrayOf(inputEValue))

            // Create Lemon evaluator with all metrics
            val evaluator = Lemon.create(applicationContext)
                .latency()
                .throughput()
                .memory()
                .modelSize()
                .iterations(30)  // 30 iterations for multi-model benchmark
                .warmup(3)       // 3 warmup iterations
                .build()

            Log.d(TAG, "Running evaluation for $modelName...")

            // Run evaluation
            val result = evaluator.evaluate(modelPath, inputs)

            Log.d(TAG, "Benchmark completed for $modelName!")

            return@withContext result
        }

    private fun copyAssetToCache(assetName: String): String {
        val cacheFile = File(cacheDir, assetName)

        if (cacheFile.exists()) {
            Log.d(TAG, "Model already exists in cache")
            return cacheFile.absolutePath
        }

        assets.open(assetName).use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }

        Log.d(TAG, "Model copied to cache: ${cacheFile.absolutePath}")
        return cacheFile.absolutePath
    }

    private fun createRandomInput(batch: Int, channels: Int, height: Int, width: Int): FloatArray {
        val size = batch * channels * height * width
        return FloatArray(size) {
            // Generate random values between 0 and 1
            kotlin.random.Random.nextFloat()
        }
    }
}
