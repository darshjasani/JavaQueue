package com.example.worker;

import com.example.model.Task;
import com.example.model.TaskStatus;
import com.example.queue.TaskQueue;
import java.util.Map;

public class Worker implements Runnable {
    
    private final String workerId;
    private final TaskQueue taskQueue;
    private final Map<String, TaskHandler> handlers;
    private volatile boolean running = true;

    public Worker(String workerId, TaskQueue taskQueue, Map<String, TaskHandler> handlers) {
        this.workerId = workerId;
        this.taskQueue = taskQueue;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        System.out.println("[" + workerId + "] Worker started");
        
        while (running) {
            try {
                // Wait for next task
                Task task = taskQueue.poll();
                processTask(task);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("[" + workerId + "] Worker stopped");
    }

    private void processTask(Task task) {
        System.out.println("[" + workerId + "] Processing: " + task);
        task.setStatus(TaskStatus.PROCESSING);

        // Find handler for this task type
        TaskHandler handler = handlers.get(task.getType());
        
        if (handler == null) {
            System.err.println("[" + workerId + "] No handler for type: " + task.getType());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No handler found for type: " + task.getType());
            return;
        }

        try {
            // Execute the task
            handler.handle(task);
            task.setStatus(TaskStatus.COMPLETED);
            System.out.println("[" + workerId + "] Completed: " + task);
            
        } catch (Exception e) {
            handleFailure(task, e);
        }
    }

    private void handleFailure(Task task, Exception e) {
        task.incrementRetry();
        
        if (task.canRetry()) {
            System.out.println("[" + workerId + "] Task failed, retrying... " + task);
            task.setStatus(TaskStatus.PENDING);
            taskQueue.submit(task); // Re-queue for retry
        } else {
            System.err.println("[" + workerId + "] Task failed permanently: " + task);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            // TODO: Send to Dead Letter Queue (Day 2)
        }
    }

    public void stop() {
        running = false;
    }
}