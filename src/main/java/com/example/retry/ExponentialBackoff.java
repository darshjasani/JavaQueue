package com.example.retry;

import java.time.Duration;
import java.util.Random;

// Exponential backoff: wait time doubles each retry
// Example: 1s → 2s → 4s → 8s
public class ExponentialBackoff implements RetryStrategy {
    
    private final long baseDelayMs;      // Starting delay
    private final long maxDelayMs;       // Cap maximum wait
    private final Random random;         // For jitter
    
    public ExponentialBackoff() {
        this(1000, 30000); // Default: 1s base, 30s max
    }
    
    public ExponentialBackoff(long baseDelayMs, long maxDelayMs) {
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.random = new Random();
    }
    
    @Override
    public Duration getDelay(int attemptNumber) {
        // Calculate: baseDelay * 2^attempt
        long delay = baseDelayMs * (long) Math.pow(2, attemptNumber - 1);
        
        // Cap at max delay
        delay = Math.min(delay, maxDelayMs);
        
        // Add jitter (±20%) to prevent thundering herd
        long jitter = (long) (delay * 0.2 * (random.nextDouble() - 0.5));
        delay += jitter;
        
        return Duration.ofMillis(delay);
    }
    
    @Override
    public boolean shouldRetry(int attemptNumber, int maxRetries) {
        return attemptNumber < maxRetries;
    }
}