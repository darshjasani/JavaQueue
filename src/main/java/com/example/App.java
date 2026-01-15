package com.example;

import com.example.api.TaskServer;
import com.example.db.DatabaseManager;
import com.example.handlers.EmailTaskHandler;
import com.example.handlers.ReportTaskHandler;
import com.example.queue.DeadLetterQueue;
import com.example.queue.PersistentTaskQueue;
import com.example.worker.WorkerPool;

public class App {
    
    public static void main(String[] args) throws Exception {
        System.out.println("===========================================");
        System.out.println("       JAVAQUEUE - Task Queue System       ");
        System.out.println("===========================================\n");

        // 1. Initialize database
        DatabaseManager db = new DatabaseManager();
        db.init();

        // 2. Create persistent queue and DLQ
        PersistentTaskQueue taskQueue = new PersistentTaskQueue(db);
        DeadLetterQueue dlq = new DeadLetterQueue();

        // 3. Create and start worker pool
        WorkerPool pool = new WorkerPool(3, taskQueue, dlq);
        pool.registerHandler(new EmailTaskHandler());
        pool.registerHandler(new ReportTaskHandler());
        pool.start();

        // 4. Start REST API server
        TaskServer server = new TaskServer(8080, taskQueue, dlq);

        // 5. Shutdown hook for graceful exit (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n--- Shutting Down ---");
            server.stop();
            pool.shutdown();
            taskQueue.shutdown();
            try { db.close(); } catch (Exception e) { }
            dlq.printSummary();
        }));

        System.out.println("\n===========================================");
        System.out.println("  Server running! Press Ctrl+C to stop.   ");
        System.out.println("===========================================\n");

        // Keep main thread alive
        Thread.currentThread().join();
    }
}