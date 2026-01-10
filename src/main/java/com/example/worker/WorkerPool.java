package com.example.worker;

import com.example.queue.DeadLetterQueue;
import com.example.queue.TaskQueue;
import com.example.retry.ExponentialBackoff;
import com.example.retry.RetryStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WorkerPool {
    
    private final int poolSize;
    private final TaskQueue taskQueue;
    private final DeadLetterQueue dlq;
    private final Map<String, TaskHandler> handlers;
    private final List<Worker> workers;
    private final RetryStrategy retryStrategy;
    private ExecutorService executor;

    public WorkerPool(int poolSize, TaskQueue taskQueue, DeadLetterQueue dlq) {
        this.poolSize = poolSize;
        this.taskQueue = taskQueue;
        this.dlq = dlq;
        this.handlers = new HashMap<>();
        this.workers = new ArrayList<>();
        this.retryStrategy = new ExponentialBackoff(); // Default strategy
    }

    // Register a handler for a task type
    public void registerHandler(TaskHandler handler) {
        handlers.put(handler.getTaskType(), handler);
        System.out.println("[POOL] Registered handler for: " + handler.getTaskType());
    }

    // Get DLQ for monitoring
    public DeadLetterQueue getDeadLetterQueue() {
        return dlq;
    }

    // Start all workers
    public void start() {
        System.out.println("[POOL] Starting " + poolSize + " workers...");
        executor = Executors.newFixedThreadPool(poolSize);
        
        for (int i = 1; i <= poolSize; i++) {
            Worker worker = new Worker("Worker-" + i, taskQueue, dlq, handlers, retryStrategy);
            workers.add(worker);
            executor.submit(worker);
        }
        
        System.out.println("[POOL] All workers started!");
    }

    // Graceful shutdown
    public void shutdown() {
        System.out.println("[POOL] Shutting down...");
        
        for (Worker worker : workers) {
            worker.stop();
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        System.out.println("[POOL] Shutdown complete");
    }
}