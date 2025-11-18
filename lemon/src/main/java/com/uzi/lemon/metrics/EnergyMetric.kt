package com.uzi.lemon.metrics

/**
 * ENERGY METRIC REMOVED
 * 
 * Reason: Energy measurement is unreliable for micro-benchmarks (<10 seconds)
 * 
 * Why it doesn't work:
 * 1. Battery level granularity: Updates every 1-5 seconds
 * 2. Energy counter noise: ±50-100 µAh noise floor
 * 3. Typical benchmark duration: 0.5-5 seconds
 * 4. Result: Always reports ~0 mAh or highly inaccurate values
 * 
 * Alternative approaches for energy measurement:
 * 1. Long-duration benchmarks (30+ seconds)
 * 2. Hardware power meters (external measurement)
 * 3. Model-based estimation (CPU frequency/utilization)
 * 4. Device-specific power profiles
 * 
 * For accurate power measurement, use external tools like:
 * - Monsoon Power Monitor
 * - Android Battery Historian (long-term trends)
 * - Qualcomm Trepn Profiler
 */

// File kept for documentation purposes
// Energy metric not available in this version
