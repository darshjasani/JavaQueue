package com.example.handlers;

import com.example.model.Task;
import com.example.worker.TaskHandler;
import java.util.Random;

public class EmailTaskHandler implements TaskHandler {
    
    private final Random random = new Random();

    @Override
    public String getTaskType() {
        return "email";
    }

    @Override
    public void handle(Task task) throws Exception {
        System.out.println("    → Sending email: " + task.getPayload());
        
        // Simulate work (1-3 seconds)
        Thread.sleep(1000 + random.nextInt(2000));
        
        // Simulate 30% failure rate for demo
        if (random.nextInt(10) < 3) {
            throw new Exception("SMTP server timeout");
        }
        
        System.out.println("    ✓ Email sent successfully!");
    }
}