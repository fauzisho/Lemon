# 🍋 Person Detection Benchmark

## Overview

This benchmark tests the performance of PyTorch ExecutorTorch models for person detection using pressure sensor data. It compares FP32 and INT8 quantized models using the Lemon benchmarking library.

## Models

Two models are tested:
- **FP32 Model**: `person_detection_model_xnnpack_fp32.pte` - Full precision model
- **INT8 Model**: `person_detection_model_xnnpack_int8.pte` - Quantized model for faster inference

Both models are:
- Trained on synthetic pressure sensor data (9x9 grid = 81 features)
- Binary classifiers (empty vs occupied)
- Optimized with XNNPACK backend for mobile CPU execution

## Model Architecture

The models are Deep Neural Networks with:
- **Input**: 81 features (9x9 pressure sensor grid)
- **Hidden layers**: [256, 128, 64] neurons with BatchNorm, ReLU, and Dropout
- **Output**: 2 classes (empty=0, occupied=1)
- **Total parameters**: ~29K trainable parameters

## Test Data

The benchmark uses `synthetic_test_data.csv` which contains:
- Synthetic pressure sensor readings
- Ground truth labels (empty/occupied)
- Approximately 1000 test samples

## Features

### 1. **Lemon Performance Metrics**

The benchmark uses the Lemon library to measure:

- **⏱️ Latency Metrics**:
  - Mean inference time
  - Median (P50)
  - P95 and P99 percentiles
  - Min/Max latency
  - Standard deviation
  - Coefficient of variation
  - Outlier detection

- **🚀 Throughput Metrics**:
  - Frames per second (FPS)
  - Samples per second
  - Average latency

- **💾 Memory Metrics**:
  - Peak PSS (Proportional Set Size)
  - Delta PSS
  - Native heap usage
  - Java heap usage

- **📦 Model Size**:
  - File size in MB

### 2. **Accuracy Testing**

Tests model accuracy on the entire test dataset:
- Compares predictions with ground truth labels
- Calculates accuracy percentage
- Shows correct predictions vs total samples

### 3. **Model Comparison**

Automatically compares FP32 vs INT8 models:
- Speed improvement (latency reduction)
- Size reduction
- Accuracy comparison

## How to Run

1. **Launch the app** and select "Person Detection Benchmark" from the launcher
2. **Tap "Run Full Benchmark"** button
3. **Wait for results** (typically 30-60 seconds)
4. **Review metrics**:
   - Overall comparison summary
   - Detailed per-model metrics
   - Lemon performance statistics

## Benchmark Configuration

```kotlin
val evaluator = Lemon.create(applicationContext)
    .latency()        // Enable latency measurement
    .throughput()     // Enable throughput measurement
    .memory()         // Enable memory tracking
    .modelSize()      // Enable size measurement
    .iterations(50)   // 50 inference runs per sample
    .warmup(5)        // 5 warmup iterations
    .build()
```

## Expected Results

### FP32 Model
- ✅ Higher accuracy (typically 99%+)
- 🐌 Slower inference (~2-5ms per sample)
- 📦 Larger model size (~350KB)

### INT8 Model
- ✅ Good accuracy (typically 98%+)
- ⚡ Faster inference (~1-3ms per sample)
- 📦 Smaller model size (~100KB)
- 💡 **1.5-2.5x speedup** vs FP32
- 💡 **3-4x size reduction** vs FP32

## Technical Details

### Input Normalization

The test data is already normalized (StandardScaler from training). The model expects:
- Input shape: `[1, 81]` (batch_size=1, features=81)
- Input type: `float32`
- Normalization: StandardScaler (mean/std from training)

### Output Format

The model outputs logits for 2 classes:
- Output shape: `[1, 2]`
- Output[0]: logit for "empty" class
- Output[1]: logit for "occupied" class
- Prediction: `argmax(output)` → 0 or 1

### Performance Considerations

- **Warmup**: 5 iterations to stabilize CPU frequency and caches
- **Iterations**: 50 runs per sample for statistical reliability
- **Sample size**: 100 samples for Lemon benchmarking (full dataset for accuracy)
- **Backend**: XNNPACK (optimized CPU backend)

## Project Structure

```
app/src/main/
├── assets/
│   ├── person_detection_model_xnnpack_fp32.pte  # FP32 model
│   ├── person_detection_model_xnnpack_int8.pte  # INT8 model
│   └── synthetic_test_data.csv                   # Test data
└── java/com/uzi/lemonbenchmark/
    └── PersonDetectionActivity.kt                 # Benchmark activity
```

## Dependencies

```kotlin
// Lemon benchmarking library
implementation(project(":lemon"))

// PyTorch ExecutorTorch
implementation("org.pytorch:executorch-android:1.0.0-rc2")

// Jetpack Compose (UI)
implementation(platform("androidx.compose:compose-bom:2024.xx.xx"))
```

## Training Script

The models were trained using `train_dnn.py`:
- Framework: PyTorch
- Export: ExecuTorch with XNNPACK backend
- Quantization: PT2E (PyTorch 2 Export) INT8 quantization
- Training: ~100 epochs with early stopping
- Validation: 70/15/15 train/val/test split

## Next Steps

1. **Test on real hardware**: Run on different Android devices to compare performance
2. **Optimize further**: Try different quantization schemes (dynamic, per-channel)
3. **Profile**: Use Android Profiler to analyze CPU and memory usage
4. **Compare backends**: Test Vulkan backend for GPU acceleration
5. **Edge cases**: Test on edge cases (partial occlusion, boundary conditions)

## Troubleshooting

### Model Not Found Error
- Ensure `.pte` files are in `app/src/main/assets/`
- Clean and rebuild the project

### CSV Parsing Error
- Verify CSV file format matches expected structure
- Check that features are in columns 0-80, label in last column

### Low Accuracy
- Verify input normalization matches training
- Check model export process
- Validate test data integrity

### Out of Memory
- Reduce batch size or number of iterations
- Close background apps
- Test on device with more RAM

## Contributing

To add new benchmarks or improve existing ones:
1. Follow the Lemon API patterns
2. Add comprehensive error handling
3. Log important metrics and errors
4. Update this README with new features

## License

This benchmark is part of the LemonBenchmark project.

---

**Built with 🍋 Lemon - ExecuTorch Performance Evaluation Framework**
