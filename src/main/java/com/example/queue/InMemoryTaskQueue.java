package com.example.queue;

import com.example.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryTaskQueue implements TaskQueue {
    
    private final BlockingQueue<Task> queue;

    public InMemoryTaskQueue() {
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void submit(Task task) {
        queue.offer(task);
        System.out.println("[QUEUE] Task submitted: " + task);
    }

    @Override
    public Task poll() throws InterruptedException {
        // Blocks until a task is available
        return queue.take();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public List<Task> getAllPending() {
        return new ArrayList<>(queue);
    }
}