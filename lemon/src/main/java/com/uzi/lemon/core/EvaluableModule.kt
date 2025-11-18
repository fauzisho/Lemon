package com.uzi.lemon.core

import android.content.Context
import org.pytorch.executorch.Module
import org.pytorch.executorch.EValue
import java.io.File

/**
 * Wrapper for ExecuTorch Module with evaluation capabilities
 * 
 * This class wraps the ExecuTorch Module and provides additional
 * functionality needed for performance evaluation.
 * 
 * @param context Android context
 * @param modelPath Path to the .pte model file
 */
class EvaluableModule(
    private val context: Context,
    private val modelPath: String
) {
    private var module: Module? = null
    
    /**
     * Check if the module is loaded
     */
    val isLoaded: Boolean 
        get() = module != null
    
    /**
     * Load the ExecuTorch module
     * @return this instance for chaining
     */
    fun load(): EvaluableModule {
        try {
            module = Module.load(modelPath)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load model from $modelPath", e)
        }
        return this
    }
    
    /**
     * Execute forward pass
     * @param inputs Variable number of EValue inputs
     * @return Array of EValue outputs
     */
    fun forward(vararg inputs: EValue): Array<EValue> {
        return module?.forward(*inputs) 
            ?: throw IllegalStateException("Module not loaded. Call load() first.")
    }
    
    /**
     * Get the model file size in bytes
     * @return Model size in bytes
     */
    fun getModelSize(): Long {
        return try {
            val file = File(modelPath)
            if (file.exists()) {
                file.length()
            } else {
                // Try to get from assets
                context.assets.openFd(modelPath).length
            }
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Release the module resources
     */
    fun release() {
        module = null
    }
}
