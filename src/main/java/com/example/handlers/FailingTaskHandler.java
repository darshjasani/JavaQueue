package com.example.handlers;

import com.example.model.Task;
import com.example.worker.TaskHandler;

// Test handler that always fails - for DLQ testing
public class FailingTaskHandler implements TaskHandler {

    @Override
    public String getTaskType() {
        return "failing";
    }

    @Override
    public void handle(Task task) throws Exception {
        System.out.println("    → Attempting task: " + task.getPayload());
        throw new Exception("Simulated failure - always fails");
    }
}