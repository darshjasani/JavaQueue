package com.example;

import com.example.handlers.EmailTaskHandler;
import com.example.handlers.FailingTaskHandler;
import com.example.handlers.ReportTaskHandler;
import com.example.model.Task;
import com.example.queue.DeadLetterQueue;
import com.example.queue.InMemoryTaskQueue;
import com.example.queue.TaskQueue;
import com.example.worker.WorkerPool;

public class App {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================");
        System.out.println("       JAVAQUEUE - Task Queue System       ");
        System.out.println("===========================================\n");

        // 1. Create queue and DLQ
        TaskQueue taskQueue = new InMemoryTaskQueue();
        DeadLetterQueue dlq = new DeadLetterQueue();

        // 2. Create worker pool with 3 workers
        WorkerPool pool = new WorkerPool(3, taskQueue, dlq);

        // 3. Register handlers
        pool.registerHandler(new EmailTaskHandler());
        pool.registerHandler(new ReportTaskHandler());

        // 4. Start workers
        pool.start();

        System.out.println("\n--- Submitting Tasks ---\n");

        // 5. Submit tasks (some will fail and retry with backoff)
        taskQueue.submit(new Task("email", "Welcome email to darsh@example.com"));
        taskQueue.submit(new Task("email", "Password reset for jasani@example.com"));
        taskQueue.submit(new Task("report", "Monthly sales report"));
        taskQueue.submit(new Task("email", "Order confirmation #5483185"));
        taskQueue.submit(new Task("report", "User analytics report"));
        taskQueue.submit(new Task("email", "Newsletter to subscribers"));

        // Register the failing handler
        pool.registerHandler(new FailingTaskHandler());

        // Submit a task that will always fail
        taskQueue.submit(new Task("failing", "This task will go to DLQ"));

        // 6. Wait for processing (longer due to backoff delays)
        System.out.println("\n--- Processing Tasks ---\n");
        Thread.sleep(30000); // 30 seconds

        // 7. Shutdown
        System.out.println("\n--- Shutting Down ---\n");
        pool.shutdown();

        // 8. Show DLQ summary
        dlq.printSummary();

        System.out.println("\n===========================================");
        System.out.println("              Demo Complete!               ");
        System.out.println("===========================================");
    }
}