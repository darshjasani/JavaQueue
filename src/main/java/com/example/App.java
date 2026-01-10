package com.example;

import com.example.handlers.EmailTaskHandler;
import com.example.handlers.ReportTaskHandler;
import com.example.model.Task;
import com.example.queue.InMemoryTaskQueue;
import com.example.queue.TaskQueue;
import com.example.worker.WorkerPool;

public class App {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("===========================================");
        System.out.println("       JAVAQUEUE - Task Queue System       ");
        System.out.println("===========================================\n");

        // 1. Create the queue
        TaskQueue taskQueue = new InMemoryTaskQueue();

        // 2. Create worker pool with 3 workers
        WorkerPool pool = new WorkerPool(3, taskQueue);

        // 3. Register task handlers
        pool.registerHandler(new EmailTaskHandler());
        pool.registerHandler(new ReportTaskHandler());

        // 4. Start workers
        pool.start();

        System.out.println("\n--- Submitting Tasks ---\n");

        // 5. Submit some tasks
        taskQueue.submit(new Task("email", "Welcome email to john@example.com"));
        taskQueue.submit(new Task("email", "Password reset for jane@example.com"));
        taskQueue.submit(new Task("report", "Monthly sales report - January"));
        taskQueue.submit(new Task("email", "Order confirmation #12345"));
        taskQueue.submit(new Task("report", "User analytics report"));
        taskQueue.submit(new Task("email", "Newsletter to subscribers"));

        // 6. Wait for tasks to complete
        System.out.println("\n--- Processing Tasks ---\n");
        Thread.sleep(20000); // Wait 20 seconds

        // 7. Shutdown
        System.out.println("\n--- Shutting Down ---\n");
        pool.shutdown();

        System.out.println("\n===========================================");
        System.out.println("              Demo Complete!               ");
        System.out.println("===========================================");
    }
}