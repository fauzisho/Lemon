package com.uzi.lemon.metrics

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.uzi.lemon.core.EvaluableModule
import com.uzi.lemon.core.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.pytorch.executorch.EValue

/**
 * Result of memory measurement
 */
@Serializable
data class MemoryResult(
    val peakPss: Long,           // Peak Proportional Set Size in KB
    val peakRss: Long,           // Peak Resident Set Size in KB
    val nativeHeap: Long,        // Native heap allocation in bytes
    val javaHeap: Long,          // Java heap allocation in bytes
    val modelSize: Long,         // Model file size in bytes
    override val unit: String = "MB"
) : Metric {
    override val name: String = "Memory"
    
    /**
     * Convert memory values to megabytes
     */
    fun toMB(): MemoryResult {
        return copy(
            peakPss = peakPss / 1024,      // KB to MB
            peakRss = peakRss / 1024,      // KB to MB
            nativeHeap = nativeHeap / 1024 / 1024,  // bytes to MB
            javaHeap = javaHeap / 1024 / 1024,      // bytes to MB
            modelSize = modelSize / 1024 / 1024     // bytes to MB
        )
    }
    
    override fun toString(): String {
        val mb = toMB()
        return """
            Memory Statistics:
              Peak PSS: ${mb.peakPss} ${mb.unit}
              Peak RSS: ${mb.peakRss} ${mb.unit}
              Native Heap: ${mb.nativeHeap} ${mb.unit}
              Java Heap: ${mb.javaHeap} ${mb.unit}
              Model Size: ${mb.modelSize} ${mb.unit}
        """.trimIndent()
    }
}

/**
 * Measures memory usage during model inference
 * 
 * @param context Android context for accessing system services
 */
class MemoryMetric(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val runtime = Runtime.getRuntime()
    
    /**
     * Measure memory usage for the given module and inputs
     * 
     * @param module The evaluable module to measure
     * @param inputs List of input arrays to test with
     * @return MemoryResult containing memory statistics
     */
    suspend fun measure(
        module: EvaluableModule,
        inputs: List<Array<EValue>>
    ): MemoryResult = withContext(Dispatchers.Default) {
        
        // Force GC before measurement to get baseline
        System.gc()
        Thread.sleep(100)
        
        val memoryInfo = Debug.MemoryInfo()
        
        // Get initial memory
        Debug.getMemoryInfo(memoryInfo)
        val initialPss = memoryInfo.totalPss.toLong()
        
        // Run inference to allocate memory
        inputs.forEach { input ->
            module.forward(*input)
        }
        
        // Get peak memory after inference
        Debug.getMemoryInfo(memoryInfo)
        val peakPss = memoryInfo.totalPss.toLong()

        // Get process memory info
        val nativeHeapSize = Debug.getNativeHeapAllocatedSize()
        val javaHeapSize = runtime.totalMemory() - runtime.freeMemory()
        
        MemoryResult(
            peakPss = peakPss,
            peakRss = Debug.getNativeHeapSize() / 1024,
            nativeHeap = nativeHeapSize,
            javaHeap = javaHeapSize,
            modelSize = module.getModelSize()
        )
    }
}
