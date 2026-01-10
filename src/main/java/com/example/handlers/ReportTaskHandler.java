package com.example.handlers;

import com.example.model.Task;
import com.example.worker.TaskHandler;
import java.util.Random;

public class ReportTaskHandler implements TaskHandler {
    
    private final Random random = new Random();

    @Override
    public String getTaskType() {
        return "report";
    }

    @Override
    public void handle(Task task) throws Exception {
        System.out.println("    → Generating report: " + task.getPayload());
        
        // Simulate work (2-4 seconds)
        Thread.sleep(2000 + random.nextInt(2000));
        
        // Simulate 20% failure rate for demo
        if (random.nextInt(10) < 2) {
            throw new Exception("Database connection failed");
        }
        
        System.out.println("    ✓ Report generated successfully!");
    }
}