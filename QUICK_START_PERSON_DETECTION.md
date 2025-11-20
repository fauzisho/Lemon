# 🚀 Quick Start: Person Detection Benchmark

## What You'll Get

A complete benchmarking app that:
- ⚡ Compares **FP32 vs INT8** quantized models
- 📊 Shows **detailed performance metrics** using Lemon library
- 🎯 Tests **accuracy** on real test data
- 💾 Measures **memory, latency, throughput**
- 📈 Displays **beautiful comparison charts**

## Prerequisites

✅ **Already Done:**
- Person detection models trained and exported as `.pte` files
- Test data CSV file ready
- Lemon benchmarking library integrated

## Files Added

### New Activity
- `app/src/main/java/com/uzi/lemonbenchmark/PersonDetectionActivity.kt`
  - Complete benchmark implementation
  - Lemon integration
  - Accuracy testing
  - Beautiful Compose UI

### Assets (Already Present)
- `app/src/main/assets/person_detection_model_xnnpack_fp32.pte`
- `app/src/main/assets/person_detection_model_xnnpack_int8.pte`
- `app/src/main/assets/synthetic_test_data.csv`

### Documentation
- `PERSON_DETECTION_BENCHMARK.md` - Complete technical documentation
- `QUICK_START_PERSON_DETECTION.md` - This file!

## How to Run

### Step 1: Build the App
```bash
# In Android Studio
./gradlew assembleDebug

# Or click the green "Run" button
```

### Step 2: Install on Device
```bash
# Via ADB
./gradlew installDebug

# Or use Android Studio's device manager
```

### Step 3: Launch
1. Open your Android device/emulator
2. You'll see **two launcher icons**:
   - "LemonBenchmark" - Original MobileNetV2 demo
   - "Person Detection Benchmark" - NEW! 🎉
3. Tap **"Person Detection Benchmark"**

### Step 4: Run Benchmark
1. Tap the **"Run Full Benchmark"** button
2. Wait 30-60 seconds (it's doing a LOT of work!)
3. Scroll through the results

## What Happens During Benchmark

```
1. Loading Models (2 models)
   └─ Copy .pte files from assets to cache
   
2. Loading Test Data
   └─ Parse CSV file (~1000 samples)
   
3. FP32 Model Testing
   ├─ Lemon Performance Test (100 samples × 50 iterations)
   │  ├─ Latency measurement
   │  ├─ Throughput calculation
   │  ├─ Memory tracking
   │  └─ Model size check
   └─ Accuracy Test (all 1000 samples)
   
4. INT8 Model Testing
   ├─ Lemon Performance Test (100 samples × 50 iterations)
   └─ Accuracy Test (all 1000 samples)
   
5. Results Display
   ├─ Comparison Summary (speedup, size reduction)
   ├─ FP32 Detailed Results
   └─ INT8 Detailed Results
```

## Understanding the Results

### 📊 Comparison Card
Shows the big picture:
```
⚡ Speed: INT8 is 2.3x faster
💾 Size: INT8 is 3.5x smaller
🎯 Accuracy: FP32: 99.2% | INT8: 98.8%
```

### ⏱️ Latency Card (per model)
Statistical analysis of inference time:
```
Mean: 2.341 ms      ← Average time per inference
Median: 2.330 ms    ← 50th percentile
P95: 2.498 ms       ← 95% of inferences are faster than this
P99: 2.721 ms       ← 99% of inferences are faster than this
Min/Max: 2.102 / 3.456 ms
StdDev: 0.234 ms    ← Consistency measure
```

### 🚀 Throughput Card
How many inferences per second:
```
FPS: 427.35          ← Frames (inferences) per second
Samples/sec: 427.35  ← Same as FPS for single sample
```

### 💾 Memory Card
Memory usage during inference:
```
Peak PSS: 42.35 MB   ← Peak memory footprint
Delta PSS: 8.21 MB   ← Memory increase from baseline
Native Heap: 15.3 MB ← Native memory usage
```

### 📦 Model Size
```
Model Size: 0.35 MB (FP32) vs 0.10 MB (INT8)
```

## Expected Performance

### On Modern Android Device (e.g., Pixel 7, Samsung S23)

| Metric | FP32 | INT8 | Improvement |
|--------|------|------|-------------|
| Latency (mean) | 2-4 ms | 1-2 ms | **2-2.5x faster** |
| Throughput | 250-500 FPS | 500-1000 FPS | **2-2.5x higher** |
| Model Size | 350 KB | 100 KB | **3.5x smaller** |
| Accuracy | 99%+ | 98%+ | **-1% acceptable** |
| Memory (PSS) | 40-60 MB | 35-50 MB | **Slightly lower** |

### On Older Device (e.g., Pixel 3, Samsung S8)

| Metric | FP32 | INT8 | Improvement |
|--------|------|------|-------------|
| Latency (mean) | 5-10 ms | 2-4 ms | **2.5-3x faster** |
| Throughput | 100-200 FPS | 250-500 FPS | **2.5-3x higher** |

## Troubleshooting

### Issue: "Error loading model"
**Solution:**
```bash
# Verify assets exist
ls app/src/main/assets/

# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### Issue: "CSV parsing error"
**Solution:**
- Check CSV format: 81 feature columns + 1 label column
- Verify no missing values
- Ensure UTF-8 encoding

### Issue: "Low accuracy (<90%)"
**Possible causes:**
- Input normalization mismatch
- Model export issue
- Test data corruption

**Debug steps:**
```kotlin
// Check logs for detailed inference info
// Look for lines like:
// "Sample 0 - Features: [0.123, 0.456, ...], 
//  Output: [-1.23, 2.34], 
//  Prediction: 1, Label: 1"
```

### Issue: "Benchmark too slow"
**Optimize:**
```kotlin
// In PersonDetectionActivity.kt, reduce iterations:
.iterations(20)   // Was 50
.warmup(3)        // Was 5

// Or test fewer samples:
val sampleSize = minOf(50, testData.size)  // Was 100
```

## Code Walkthrough

### Key Components

**1. Test Data Loading**
```kotlin
private fun loadTestData(): List<TestSample> {
    // Reads CSV from assets
    // Parses 81 features + 1 label
    // Returns list of TestSample objects
}
```

**2. Lemon Integration**
```kotlin
val evaluator = Lemon.create(applicationContext)
    .latency()      // Track inference time
    .throughput()   // Calculate FPS
    .memory()       // Monitor memory usage
    .modelSize()    // Get file size
    .iterations(50) // Run 50 times
    .warmup(5)      // 5 warmup runs
    .build()

val result = evaluator.evaluate(modelPath, inputs)
```

**3. Accuracy Testing**
```kotlin
private fun testModelAccuracy(
    modelPath: String, 
    testData: List<TestSample>
): Pair<Double, Int> {
    // Load model
    // For each test sample:
    //   - Run inference
    //   - Compare prediction with label
    //   - Count correct predictions
    // Return (accuracy%, correct_count)
}
```

**4. UI with Jetpack Compose**
```kotlin
@Composable
fun PersonDetectionBenchmarkScreen() {
    // Button to start benchmark
    // Progress indicator
    // Results display:
    //   - Comparison card
    //   - Per-model metrics cards
}
```

## Next Steps

### 1. Test on Real Hardware
Run on different devices to see performance variation:
- Budget phones (low-end CPU)
- Flagship phones (high-end CPU)
- Tablets (different thermal characteristics)

### 2. Optimize Based on Results
If INT8 model:
- ✅ **Is 2x+ faster**: Great! Ship it!
- ⚠️ **Lost >2% accuracy**: Consider hybrid approach or retrain
- 📊 **Memory usage high**: Profile with Android Studio

### 3. Production Integration
```kotlin
// Use the benchmark winner in production
val productionModel = if (int8IsFasterAndAccurate) {
    "person_detection_model_xnnpack_int8.pte"
} else {
    "person_detection_model_xnnpack_fp32.pte"
}
```

### 4. Add More Tests
- Test with real pressure sensor data
- Test edge cases (partial occlusion)
- Test different sensor configurations
- Compare with other backends (Vulkan, CoreML)

## Pro Tips

### 💡 Tip 1: Benchmark on Battery
Test both on charger and on battery - CPU throttling is real!

### 💡 Tip 2: Cool Down Between Runs
Wait 30s between benchmarks to avoid thermal throttling.

### 💡 Tip 3: Close Background Apps
For consistent results, close other apps before benchmarking.

### 💡 Tip 4: Use Airplane Mode
Disable network to reduce background CPU activity.

### 💡 Tip 5: Check Lemon Logs
```bash
adb logcat | grep "Lemon\|PersonDetection"
```

## Questions?

Check out:
- `PERSON_DETECTION_BENCHMARK.md` - Full technical details
- Lemon library documentation
- PyTorch ExecutorTorch docs

## What's Next?

Now that you have comprehensive benchmarks, you can:
1. 📊 Make data-driven decisions about model selection
2. 🎯 Optimize based on your specific requirements (speed vs accuracy)
3. 🚀 Ship the best model to production with confidence
4. 📈 Track performance improvements over time

---

**Happy Benchmarking! 🍋**
