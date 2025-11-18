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

class MainActivity : ComponentActivity() {
    private val TAG = "LemonBenchmark"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LemonBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BenchmarkScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun BenchmarkScreen(modifier: Modifier = Modifier) {
        var isRunning by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<EvaluationResult?>(null) }
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
                text = "🍋 Lemon Benchmark",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "ExecuTorch Model Performance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        error = null
                        result = null

                        try {
                            val benchmarkResult = runBenchmark()
                            result = benchmarkResult
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            Log.e(TAG, "Benchmark error", e)
                        } finally {
                            isRunning = false
                        }
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
                    Text("Running Benchmark...")
                } else {
                    Text("Run Benchmark")
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
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            result?.let { res ->
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
                            text = "Benchmark Results",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Model: ${res.modelPath}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Backend: ${res.backend}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        res.latency?.let { latency ->
                            MetricCard(
                                title = "⏱️ Latency",
                                metrics = listOf(
                                    "Mean" to "${String.format("%.3f", latency.mean)} ms",
                                    "Median" to "${String.format("%.3f", latency.median)} ms",
                                    "P95" to "${String.format("%.3f", latency.p95)} ms",
                                    "P99" to "${String.format("%.3f", latency.p99)} ms",
                                    "Min" to "${String.format("%.3f", latency.min)} ms",
                                    "Max" to "${String.format("%.3f", latency.max)} ms",
                                    "StdDev" to "${String.format("%.3f", latency.stdDev)} ms"
                                )
                            )
                        }

                        res.throughput?.let { throughput ->
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricCard(
                                title = "🚀 Throughput",
                                metrics = listOf(
                                    "Samples/sec" to "${String.format("%.2f", throughput.samplesPerSecond)}",
                                    "Total Samples" to "${throughput.totalSamples}",
                                    "Total Time" to "${String.format("%.2f", throughput.totalTimeMs)} ms"
                                )
                            )
                        }

                        res.memory?.let { memory ->
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricCard(
                                title = "💾 Memory",
                                metrics = listOf(
                                    "Peak PSS" to "${String.format("%.2f", memory.peakPss / 1024.0)} MB",
                                    "Peak RSS" to "${String.format("%.2f", memory.peakRss / 1024.0)} MB",
                                    "Native Heap" to "${String.format("%.2f", memory.nativeHeap / (1024.0 * 1024.0))} MB",
                                    "Java Heap" to "${String.format("%.2f", memory.javaHeap / (1024.0 * 1024.0))} MB"
                                )
                            )
                        }

                        res.modelSize?.let { size ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📦 Model Size: ${String.format("%.2f", size / (1024.0 * 1024.0))} MB",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MetricCard(title: String, metrics: List<Pair<String, String>>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                metrics.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    private suspend fun runBenchmark(): EvaluationResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting benchmark...")

        // Copy model from assets to cache directory
        val modelPath = copyAssetToCache("mv2_xnnpack_fp32.pte")
        Log.d(TAG, "Model path: $modelPath")

        // Create a sample input tensor for MobileNetV2
        // MobileNetV2 expects input shape: [1, 3, 224, 224]
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
            .iterations(50)  // 50 iterations for quick demo
            .warmup(5)       // 5 warmup iterations
            .build()

        Log.d(TAG, "Running evaluation...")

        // Run evaluation
        val result = evaluator.evaluate(modelPath, inputs)

        Log.d(TAG, "Benchmark completed!")
        Log.d(TAG, result.toString())

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
