package com.example.worker;

import com.example.model.Task;
import com.example.model.TaskStatus;
import com.example.queue.DeadLetterQueue;
import com.example.queue.TaskQueue;
import com.example.retry.RetryStrategy;
import java.time.Duration;
import java.util.Map;

public class Worker implements Runnable {
    
    private final String workerId;
    private final TaskQueue taskQueue;
    private final DeadLetterQueue dlq;
    private final Map<String, TaskHandler> handlers;
    private final RetryStrategy retryStrategy;
    private volatile boolean running = true;

    public Worker(String workerId, TaskQueue taskQueue, DeadLetterQueue dlq,
                  Map<String, TaskHandler> handlers, RetryStrategy retryStrategy) {
        this.workerId = workerId;
        this.taskQueue = taskQueue;
        this.dlq = dlq;
        this.handlers = handlers;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public void run() {
        System.out.println("[" + workerId + "] Worker started");
        
        while (running) {
            try {
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

        TaskHandler handler = handlers.get(task.getType());
        
        if (handler == null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No handler for type: " + task.getType());
            dlq.add(task);
            return;
        }

        try {
            handler.handle(task);
            task.setStatus(TaskStatus.COMPLETED);
            System.out.println("[" + workerId + "] Completed: " + task);
            
        } catch (Exception e) {
            handleFailure(task, e);
        }
    }

    private void handleFailure(Task task, Exception e) {
        task.incrementRetry();
        task.setErrorMessage(e.getMessage());
        
        if (retryStrategy.shouldRetry(task.getRetryCount(), task.getMaxRetries())) {
            // Calculate backoff delay
            Duration delay = retryStrategy.getDelay(task.getRetryCount());
            System.out.println("[" + workerId + "] Task failed, retry in " + 
                             delay.toMillis() + "ms... " + task);
            
            // Wait before requeue (backoff)
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            task.setStatus(TaskStatus.PENDING);
            taskQueue.submit(task);
        } else {
            // Max retries reached → send to DLQ
            System.err.println("[" + workerId + "] Task failed permanently: " + task);
            task.setStatus(TaskStatus.FAILED);
            dlq.add(task);
        }
    }

    public void stop() {
        running = false;
    }
}