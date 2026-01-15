package com.example.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {
    
    private final AtomicLong tasksSubmitted = new AtomicLong(0);
    private final AtomicLong tasksCompleted = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    // Singleton instance
    private static final MetricsCollector INSTANCE = new MetricsCollector();
    public static MetricsCollector getInstance() { return INSTANCE; }

    public void recordSubmit() {
        tasksSubmitted.incrementAndGet();
    }

    public void recordSuccess(long processingTimeMs) {
        tasksCompleted.incrementAndGet();
        totalProcessingTimeMs.addAndGet(processingTimeMs);
    }

    public void recordFailure() {
        tasksFailed.incrementAndGet();
    }

    // Getters
    public long getTasksSubmitted() { return tasksSubmitted.get(); }
    public long getTasksCompleted() { return tasksCompleted.get(); }
    public long getTasksFailed() { return tasksFailed.get(); }
    
    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }

    public double getAvgProcessingTimeMs() {
        long completed = tasksCompleted.get();
        return completed > 0 ? (double) totalProcessingTimeMs.get() / completed : 0;
    }

    public double getSuccessRate() {
        long total = tasksCompleted.get() + tasksFailed.get();
        return total > 0 ? (double) tasksCompleted.get() / total * 100 : 0;
    }
}