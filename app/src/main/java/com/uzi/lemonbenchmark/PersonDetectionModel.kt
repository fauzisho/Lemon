package com.uzi.lemonbenchmark

import android.content.Context
import android.util.Log
import org.json.JSONObject

class PersonDetectionModel(context: Context) {
    
    private var scalerMean: FloatArray? = null
    private var scalerStd: FloatArray? = null
    
    init {
        loadModelInfo(context)
    }
    
    private fun loadModelInfo(context: Context) {
        try {
            val json = context.assets.open("model_info.json")
                .bufferedReader().use { it.readText() }
            val info = JSONObject(json)
            
            val meanArray = info.getJSONArray("scaler_mean")
            scalerMean = FloatArray(meanArray.length()) { i ->
                meanArray.getDouble(i).toFloat()
            }
            
            val stdArray = info.getJSONArray("scaler_std")
            scalerStd = FloatArray(stdArray.length()) { i ->
                stdArray.getDouble(i).toFloat()
            }
            
            Log.d("PersonDetection", "✓ Scaler loaded: ${scalerMean?.size} features")
        } catch (e: Exception) {
            Log.e("PersonDetection", "Error loading model_info.json: ${e.message}")
        }
    }
    
    fun normalizeInput(rawInput: FloatArray): FloatArray? {
        if (scalerMean == null || scalerStd == null) {
            Log.e("PersonDetection", "Scaler not loaded")
            return null
        }
        
        if (rawInput.size != 81) {
            Log.e("PersonDetection", "Input size mismatch: ${rawInput.size}")
            return null
        }
        
        return FloatArray(81) { i ->
            val mean = scalerMean!![i]
            val std = scalerStd!![i]
            if (std == 0f) rawInput[i] - mean else (rawInput[i] - mean) / std
        }
    }
    
    fun predict(sensorData: FloatArray, modelOutput: Float): Prediction {
        val confidence = modelOutput.coerceIn(0f, 1f)
        val label = if (confidence >= 0.5f) "occupied" else "empty"
        
        Log.d("PersonDetection", "Prediction: $label (${(confidence * 100).toInt()}%)")
        
        return Prediction(label, confidence)
    }
    
    data class Prediction(
        val label: String,
        val confidence: Float
    )
}
