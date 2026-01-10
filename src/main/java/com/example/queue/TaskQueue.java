package com.example.queue;

import com.example.model.Task;
import java.util.List;

public interface TaskQueue {
    
    // Add task to queue
    void submit(Task task);
    
    // Get next task (blocks if empty)
    Task poll() throws InterruptedException;
    
    // Get queue size
    int size();
    
    // Check if queue is empty
    boolean isEmpty();
    
    // Get all pending tasks (for monitoring)
    List<Task> getAllPending();
}