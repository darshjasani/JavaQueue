package com.example.queue;

import com.example.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// Stores tasks that failed after all retries
public class DeadLetterQueue {
    
    private final ConcurrentLinkedQueue<Task> deadTasks;
    
    public DeadLetterQueue() {
        this.deadTasks = new ConcurrentLinkedQueue<>();
    }
    
    // Add failed task to DLQ
    public void add(Task task) {
        deadTasks.offer(task);
        System.err.println("[DLQ] Task added to dead letter queue: " + task);
        System.err.println("[DLQ] Reason: " + task.getErrorMessage());
    }
    
    // Get all dead tasks (for monitoring/manual retry)
    public List<Task> getAll() {
        return new ArrayList<>(deadTasks);
    }
    
    // Get count of failed tasks
    public int size() {
        return deadTasks.size();
    }
    
    // Check if empty
    public boolean isEmpty() {
        return deadTasks.isEmpty();
    }
    
    // Requeue a task for retry (manual intervention)
    public Task poll() {
        return deadTasks.poll();
    }
    
    // Print summary
    public void printSummary() {
        System.out.println("\n[DLQ] === Dead Letter Queue Summary ===");
        System.out.println("[DLQ] Total failed tasks: " + size());
        for (Task task : deadTasks) {
            System.out.println("[DLQ]   - " + task + " | Error: " + task.getErrorMessage());
        }
    }
}