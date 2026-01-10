package com.example.model;

public enum TaskStatus {
    PENDING,      // Waiting in queue
    PROCESSING,   // Currently being worked on
    COMPLETED,    // Successfully finished
    FAILED        // Failed after all retries
}