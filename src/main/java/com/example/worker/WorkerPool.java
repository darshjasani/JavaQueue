package com.example.worker;

import com.example.queue.TaskQueue;
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
    private final Map<String, TaskHandler> handlers;
    private final List<Worker> workers;
    private ExecutorService executor;

    public WorkerPool(int poolSize, TaskQueue taskQueue) {
        this.poolSize = poolSize;
        this.taskQueue = taskQueue;
        this.handlers = new HashMap<>();
        this.workers = new ArrayList<>();
    }

    // Register a handler for a task type
    public void registerHandler(TaskHandler handler) {
        handlers.put(handler.getTaskType(), handler);
        System.out.println("[POOL] Registered handler for: " + handler.getTaskType());
    }

    // Start all workers
    public void start() {
        System.out.println("[POOL] Starting " + poolSize + " workers...");
        executor = Executors.newFixedThreadPool(poolSize);
        
        for (int i = 1; i <= poolSize; i++) {
            Worker worker = new Worker("Worker-" + i, taskQueue, handlers);
            workers.add(worker);
            executor.submit(worker);
        }
        
        System.out.println("[POOL] All workers started!");
    }

    // Graceful shutdown
    public void shutdown() {
        System.out.println("[POOL] Shutting down...");
        
        // Signal workers to stop
        for (Worker worker : workers) {
            worker.stop();
        }
        
        // Shutdown executor
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