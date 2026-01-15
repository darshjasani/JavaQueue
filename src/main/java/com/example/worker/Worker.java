package com.example.worker;

import com.example.metrics.MetricsCollector;
import com.example.model.Task;
import com.example.model.TaskStatus;
import com.example.queue.DeadLetterQueue;
import com.example.queue.PersistentTaskQueue;
import com.example.retry.RetryStrategy;
import java.time.Duration;
import java.util.Map;

public class Worker implements Runnable {
    
    private final String workerId;
    private final PersistentTaskQueue taskQueue;
    private final DeadLetterQueue dlq;
    private final Map<String, TaskHandler> handlers;
    private final RetryStrategy retryStrategy;
    private final MetricsCollector metrics = MetricsCollector.getInstance();
    private volatile boolean running = true;

    public Worker(String workerId, PersistentTaskQueue taskQueue, DeadLetterQueue dlq,
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
        long startTime = System.currentTimeMillis();
        System.out.println("[" + workerId + "] Processing: " + task);
        task.setStatus(TaskStatus.PROCESSING);
        taskQueue.updateTask(task);

        TaskHandler handler = handlers.get(task.getType());
        
        if (handler == null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No handler for type: " + task.getType());
            taskQueue.updateTask(task);
            dlq.add(task);
            metrics.recordFailure();
            return;
        }

        try {
            handler.handle(task);
            long duration = System.currentTimeMillis() - startTime;
            task.setStatus(TaskStatus.COMPLETED);
            taskQueue.removeTask(task.getId());
            metrics.recordSuccess(duration);
            System.out.println("[" + workerId + "] Completed: " + task + " (" + duration + "ms)");
            
        } catch (Exception e) {
            handleFailure(task, e);
        }
    }

    private void handleFailure(Task task, Exception e) {
        task.incrementRetry();
        task.setErrorMessage(e.getMessage());
        
        if (retryStrategy.shouldRetry(task.getRetryCount(), task.getMaxRetries())) {
            Duration delay = retryStrategy.getDelay(task.getRetryCount());
            System.out.println("[" + workerId + "] Task failed, retry in " + 
                             delay.toMillis() + "ms... " + task);
            
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            task.setStatus(TaskStatus.PENDING);
            taskQueue.submit(task);
        } else {
            System.err.println("[" + workerId + "] Task failed permanently: " + task);
            task.setStatus(TaskStatus.FAILED);
            taskQueue.updateTask(task);
            dlq.add(task);
            metrics.recordFailure();
        }
    }

    public void stop() {
        running = false;
    }
}