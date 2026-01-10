package com.example.retry;

import java.time.Duration;

// Interface for different retry strategies
public interface RetryStrategy {
    
    // Calculate wait time before next retry
    Duration getDelay(int attemptNumber);
    
    // Check if we should retry
    boolean shouldRetry(int attemptNumber, int maxRetries);
}