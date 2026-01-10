package com.example;

import com.example.api.TaskServer;
import com.example.handlers.EmailTaskHandler;
import com.example.handlers.ReportTaskHandler;
import com.example.queue.DeadLetterQueue;
import com.example.queue.InMemoryTaskQueue;
import com.example.queue.TaskQueue;
import com.example.worker.WorkerPool;

public class App {
    
    public static void main(String[] args) throws Exception {
        System.out.println("===========================================");
        System.out.println("       JAVAQUEUE - Task Queue System       ");
        System.out.println("===========================================\n");

        // 1. Create queue and DLQ
        TaskQueue taskQueue = new InMemoryTaskQueue();
        DeadLetterQueue dlq = new DeadLetterQueue();

        // 2. Create and start worker pool
        WorkerPool pool = new WorkerPool(3, taskQueue, dlq);
        pool.registerHandler(new EmailTaskHandler());
        pool.registerHandler(new ReportTaskHandler());
        pool.start();

        // 3. Start REST API server (starts automatically in constructor)
        TaskServer server = new TaskServer(8080, taskQueue, dlq);

        // 4. Shutdown hook for graceful exit (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n--- Shutting Down ---");
            server.stop();
            pool.shutdown();
            dlq.printSummary();
        }));

        System.out.println("\n===========================================");
        System.out.println("  Server running! Press Ctrl+C to stop.   ");
        System.out.println("===========================================\n");

        // Keep main thread alive
        Thread.currentThread().join();
    }
}