package com.example.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Task {
    private final String id;
    private final String type;
    private final String payload;
    private TaskStatus status;
    private int retryCount;
    private final int maxRetries;
    private final LocalDateTime createdAt;
    private String errorMessage;

    public Task(String type, String payload) {
        this(type, payload, 3); // Default 3 retries
    }

    public Task(String type, String payload, int maxRetries) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.payload = payload;
        this.status = TaskStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = maxRetries;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public TaskStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getErrorMessage() { return errorMessage; }

    // Setters
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setErrorMessage(String msg) { this.errorMessage = msg; }
    public void incrementRetry() { this.retryCount++; }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    @Override
    public String toString() {
        return String.format("Task[id=%s, type=%s, status=%s, retries=%d/%d]",
                id, type, status, retryCount, maxRetries);
    }
}