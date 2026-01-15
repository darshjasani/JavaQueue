package com.example.api;

import com.example.model.Task;
import com.example.queue.DeadLetterQueue;
import com.example.queue.PersistentTaskQueue;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class TaskServer {

    private final Javalin app;
    private final PersistentTaskQueue taskQueue;
    private final DeadLetterQueue dlq;

    public TaskServer(int port, PersistentTaskQueue taskQueue, DeadLetterQueue dlq) {
        this.taskQueue = taskQueue;
        this.dlq = dlq;
        
        this.app = Javalin.create()
            .post("/tasks/submit", this::handleSubmit)
            .get("/tasks", this::handleTasks)
            .get("/dlq", this::handleDLQ)
            .get("/health", this::handleHealth);
        
        app.start(port);
        
        System.out.println("[API] Server started on http://localhost:" + port);
        System.out.println("[API] Endpoints:");
        System.out.println("      POST /tasks/submit - Submit a task");
        System.out.println("      GET  /tasks        - View pending tasks");
        System.out.println("      GET  /dlq          - View dead letter queue");
        System.out.println("      GET  /health       - Health check");
    }

    public void stop() {
        app.stop();
        System.out.println("[API] Server stopped");
    }

    // POST /tasks/submit - supports optional delay in seconds
    private void handleSubmit(Context ctx) {
        String type = ctx.formParam("type");
        String payload = ctx.formParam("payload");
        String delayStr = ctx.formParam("delay");
        
        // Try JSON body
        if (type == null) {
            try {
                var json = ctx.bodyAsClass(TaskRequest.class);
                type = json.type;
                payload = json.payload;
                delayStr = json.delay;
            } catch (Exception e) {
                ctx.status(400).json(new Response("error", "Invalid request"));
                return;
            }
        }

        if (type == null || type.isEmpty()) {
            ctx.status(400).json(new Response("error", "Missing 'type' field"));
            return;
        }

        Task task = new Task(type, payload != null ? payload : "");
        
        // Check for delay
        if (delayStr != null && !delayStr.isEmpty()) {
            try {
                long delay = Long.parseLong(delayStr);
                taskQueue.submitDelayed(task, delay);
                ctx.status(201).json(new SubmitResponse("success", 
                    "Task scheduled (delay: " + delay + "s)", task.getId()));
                return;
            } catch (NumberFormatException e) {
                ctx.status(400).json(new Response("error", "Invalid delay value"));
                return;
            }
        }
        
        taskQueue.submit(task);
        ctx.status(201).json(new SubmitResponse("success", "Task submitted", task.getId()));
    }

    // GET /tasks
    private void handleTasks(Context ctx) {
        ctx.json(taskQueue.getAllPending());
    }

    // GET /dlq
    private void handleDLQ(Context ctx) {
        ctx.json(dlq.getAll());
    }

    // GET /health
    private void handleHealth(Context ctx) {
        ctx.json(new HealthResponse("healthy", taskQueue.size(), dlq.size()));
    }

    // DTOs
    record TaskRequest(String type, String payload, String delay) {}
    record Response(String status, String message) {}
    record SubmitResponse(String status, String message, String taskId) {}
    record HealthResponse(String status, int pendingTasks, int deadTasks) {}
}