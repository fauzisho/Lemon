package com.uzi.lemon.metrics

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.uzi.lemon.core.EvaluableModule
import com.uzi.lemon.core.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.pytorch.executorch.EValue

/**
 * Result of energy measurement
 */
@Serializable
data class EnergyResult(
    val energyConsumedMah: Double,       // Energy consumed in mAh
    val durationMs: Long,                // Duration of measurement in milliseconds
    val averagePowerMw: Double,          // Average power consumption in milliwatts
    val initialBatteryLevel: Int,        // Initial battery level (%)
    val finalBatteryLevel: Int,          // Final battery level (%)
    override val unit: String = "mAh"
) : Metric {
    override val name: String = "Energy"
    
    override fun toString(): String {
        return """
            Energy Statistics:
              Energy Consumed: ${"%.2f".format(energyConsumedMah)} $unit
              Duration: ${durationMs}ms
              Average Power: ${"%.2f".format(averagePowerMw)} mW
              Battery Level: $initialBatteryLevel% → $finalBatteryLevel%
        """.trimIndent()
    }
}

/**
 * Measures energy consumption during model inference
 * 
 * Note: Energy measurement requires Android API 21+ and may not be available
 * on all devices. Results are estimates based on battery drain.
 * 
 * @param context Android context for accessing battery services
 */
class EnergyMetric(private val context: Context) {
    
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    
    /**
     * Measure energy consumption for the given module and inputs
     * 
     * @param module The evaluable module to measure
     * @param inputs List of input arrays to test with
     * @return EnergyResult containing energy statistics
     */
    suspend fun measure(
        module: EvaluableModule,
        inputs: List<Array<EValue>>
    ): EnergyResult = withContext(Dispatchers.Default) {
        
        require(inputs.isNotEmpty()) { "Inputs list cannot be empty" }
        
        // Get initial battery state
        val initialLevel = getBatteryLevel()
        val initialEnergy = getCurrentEnergyCounter()
        val startTime = System.currentTimeMillis()
        
        // Run inference
        inputs.forEach { input ->
            module.forward(*input)
        }
        
        // Get final battery state
        val endTime = System.currentTimeMillis()
        val finalEnergy = getCurrentEnergyCounter()
        val finalLevel = getBatteryLevel()
        
        val duration = endTime - startTime
        
        // Calculate energy consumed
        val energyConsumed = if (initialEnergy > 0 && finalEnergy > 0) {
            // Energy counter available (more accurate)
            (initialEnergy - finalEnergy) / 1000.0  // Convert to mAh
        } else {
            // Estimate from battery level change (less accurate)
            val batteryCapacity = getBatteryCapacity()
            val levelDrop = (initialLevel - finalLevel) / 100.0
            batteryCapacity * levelDrop
        }
        
        // Calculate average power
        val averagePower = if (duration > 0) {
            (energyConsumed * 3.7 * 1000) / duration  // Convert to mW (assuming 3.7V nominal)
        } else {
            0.0
        }
        
        EnergyResult(
            energyConsumedMah = energyConsumed.coerceAtLeast(0.0),
            durationMs = duration,
            averagePowerMw = averagePower.coerceAtLeast(0.0),
            initialBatteryLevel = initialLevel,
            finalBatteryLevel = finalLevel
        )
    }
    
    /**
     * Get current battery level as percentage (0-100)
     */
    private fun getBatteryLevel(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Get current energy counter in microampere-hours (µAh)
     * Returns -1 if not available
     */
    private fun getCurrentEnergyCounter(): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: -1L
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Get battery capacity in mAh
     * Returns estimated value if not available
     */
    private fun getBatteryCapacity(): Double {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
                if (capacity > 0) {
                    // Estimate typical battery capacity based on device
                    // This is a rough approximation
                    3000.0  // Typical smartphone battery capacity
                } else {
                    3000.0
                }
            } else {
                3000.0
            }
        } catch (e: Exception) {
            3000.0
        }
    }
}
