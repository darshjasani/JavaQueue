package com.example.queue;

import com.example.db.DatabaseManager;
import com.example.model.Task;
import com.example.model.TaskStatus;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PersistentTaskQueue implements TaskQueue {
    
    private final BlockingQueue<Task> queue;
    private final DatabaseManager db;
    private final ScheduledExecutorService scheduler;

    public PersistentTaskQueue(DatabaseManager db) {
        this.queue = new LinkedBlockingQueue<>();
        this.db = db;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Load pending tasks from DB on startup
        loadPendingTasks();
        
        // Check for delayed tasks every second
        scheduler.scheduleAtFixedRate(this::checkDelayedTasks, 1, 1, TimeUnit.SECONDS);
    }

    // Load pending tasks from database (recovery after restart)
    private void loadPendingTasks() {
        try {
            List<Task> tasks = db.getPendingTasks();
            for (Task task : tasks) {
                if (task.isReady()) {
                    queue.offer(task);
                }
            }
            System.out.println("[QUEUE] Loaded " + tasks.size() + " pending tasks from database");
        } catch (SQLException e) {
            System.err.println("[QUEUE] Failed to load tasks: " + e.getMessage());
        }
    }

    // Check for delayed tasks that are now ready
    private void checkDelayedTasks() {
        try {
            List<Task> ready = db.getPendingTasks();
            for (Task task : ready) {
                if (!queueContains(task.getId())) {
                    queue.offer(task);
                }
            }
        } catch (SQLException e) {
            // Silent - will retry next second
        }
    }

    // Check if task already in memory queue
    private boolean queueContains(String taskId) {
        return queue.stream().anyMatch(t -> t.getId().equals(taskId));
    }

    @Override
    public void submit(Task task) {
        try {
            db.save(task);
            if (task.isReady()) {
                queue.offer(task);
            }
            System.out.println("[QUEUE] Task submitted: " + task);
        } catch (SQLException e) {
            System.err.println("[QUEUE] Failed to save task: " + e.getMessage());
        }
    }

    // Submit with delay
    public void submitDelayed(Task task, long delaySeconds) {
        task.setExecuteAt(LocalDateTime.now().plusSeconds(delaySeconds));
        try {
            db.save(task);
            System.out.println("[QUEUE] Delayed task submitted (executes in " + delaySeconds + "s): " + task);
        } catch (SQLException e) {
            System.err.println("[QUEUE] Failed to save task: " + e.getMessage());
        }
    }

    @Override
    public Task poll() throws InterruptedException {
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
        try {
            return db.getTasksByStatus(TaskStatus.PENDING);
        } catch (SQLException e) {
            return List.of();
        }
    }

    // Update task in database
    public void updateTask(Task task) {
        try {
            db.update(task);
        } catch (SQLException e) {
            System.err.println("[QUEUE] Failed to update task: " + e.getMessage());
        }
    }

    // Remove completed task from database
    public void removeTask(String taskId) {
        try {
            db.delete(taskId);
        } catch (SQLException e) {
            System.err.println("[QUEUE] Failed to delete task: " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}