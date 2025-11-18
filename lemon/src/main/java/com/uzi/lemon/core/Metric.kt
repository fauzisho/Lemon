package com.uzi.lemon.core

/**
 * Base interface for all metrics in Lemon evaluation framework
 */
interface Metric {
    val name: String
    val unit: String
}

/**
 * Types of metrics available in Lemon
 */
enum class MetricType {
    LATENCY,
    THROUGHPUT,
    MEMORY,
    ENERGY,
    MODEL_SIZE
}
