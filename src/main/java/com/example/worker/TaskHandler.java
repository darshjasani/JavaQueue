package com.example.worker;

import com.example.model.Task;

public interface TaskHandler {
    
    // What task type this handler processes
    String getTaskType();
    
    // Process the task - throw exception if failed
    void handle(Task task) throws Exception;
}