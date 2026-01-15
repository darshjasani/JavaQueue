package com.example.api;

import com.example.metrics.MetricsCollector;
import com.example.model.Task;
import com.example.queue.DeadLetterQueue;
import com.example.queue.PersistentTaskQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

public class TaskServer {

    private final Javalin app;
    private final PersistentTaskQueue taskQueue;
    private final DeadLetterQueue dlq;
    private final MetricsCollector metrics = MetricsCollector.getInstance();

    public TaskServer(int port, PersistentTaskQueue taskQueue, DeadLetterQueue dlq) {
        this.taskQueue = taskQueue;
        this.dlq = dlq;
        
        // Configure Jackson for LocalDateTime support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        this.app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper));
        })
            .get("/", this::handleDashboard)
            .get("/metrics", this::handleMetrics)
            .post("/tasks/submit", this::handleSubmit)
            .get("/tasks", this::handleTasks)
            .get("/dlq", this::handleDLQ)
            .get("/health", this::handleHealth);
        
        app.start(port);
        
        System.out.println("[API] Server started on http://localhost:" + port);
        System.out.println("[API] Endpoints:");
        System.out.println("      GET  /             - Dashboard");
        System.out.println("      GET  /metrics      - Metrics JSON");
        System.out.println("      POST /tasks/submit - Submit a task");
        System.out.println("      GET  /tasks        - View pending tasks");
        System.out.println("      GET  /dlq          - View dead letter queue");
        System.out.println("      GET  /health       - Health check");
    }

    public void stop() {
        app.stop();
        System.out.println("[API] Server stopped");
    }

    // GET / - Dashboard
    private void handleDashboard(Context ctx) {
        ctx.html(getDashboardHtml());
    }
    
    private String getDashboardHtml() {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>JavaQueue Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f172a; color: #e2e8f0; padding: 2rem; }
        h1 { color: #38bdf8; margin-bottom: 2rem; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
        .card { background: #1e293b; padding: 1.5rem; border-radius: 0.5rem; border: 1px solid #334155; }
        .card h3 { color: #94a3b8; font-size: 0.875rem; margin-bottom: 0.5rem; }
        .card .value { font-size: 2rem; font-weight: bold; color: #f1f5f9; }
        .card .value.success { color: #4ade80; }
        .card .value.error { color: #f87171; }
        .card .value.info { color: #38bdf8; }
        .section { background: #1e293b; padding: 1.5rem; border-radius: 0.5rem; margin-bottom: 1rem; border: 1px solid #334155; }
        .section h2 { color: #38bdf8; margin-bottom: 1rem; font-size: 1.25rem; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 0.75rem; border-bottom: 1px solid #334155; }
        th { color: #94a3b8; font-weight: 500; }
        .empty { color: #64748b; font-style: italic; padding: 1rem; }
        .refresh { color: #64748b; font-size: 0.875rem; margin-top: 1rem; }
        #taskForm { display: flex; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
        input, select, button { padding: 0.5rem 1rem; border-radius: 0.25rem; border: 1px solid #334155; background: #0f172a; color: #e2e8f0; }
        button { background: #38bdf8; color: #0f172a; cursor: pointer; font-weight: 500; }
        button:hover { background: #0ea5e9; }
        .status { padding: 0.5rem; margin-top: 0.5rem; border-radius: 0.25rem; display: none; }
        .status.show { display: block; }
        .status.success { background: #166534; }
        .status.error { background: #991b1b; }
    </style>
</head>
<body>
    <h1>JavaQueue Dashboard</h1>
    
    <div class="grid">
        <div class="card">
            <h3>SUBMITTED</h3>
            <div class="value info" id="submitted">0</div>
        </div>
        <div class="card">
            <h3>COMPLETED</h3>
            <div class="value success" id="completed">0</div>
        </div>
        <div class="card">
            <h3>FAILED</h3>
            <div class="value error" id="failed">0</div>
        </div>
        <div class="card">
            <h3>SUCCESS RATE</h3>
            <div class="value" id="rate">0%</div>
        </div>
        <div class="card">
            <h3>AVG TIME</h3>
            <div class="value" id="avgTime">0ms</div>
        </div>
        <div class="card">
            <h3>UPTIME</h3>
            <div class="value" id="uptime">0s</div>
        </div>
    </div>

    <div class="section">
        <h2>Submit Task</h2>
        <div id="taskForm">
            <select id="taskType">
                <option value="email">Email</option>
                <option value="report">Report</option>
            </select>
            <input type="text" id="payload" placeholder="Payload (e.g. user@example.com)">
            <input type="number" id="delay" placeholder="Delay (sec)" min="0" style="width:100px;">
            <button id="submitBtn">Submit</button>
        </div>
        <div id="statusMsg" class="status"></div>
    </div>

    <div class="section">
        <h2>Pending Tasks (<span id="pendingCount">0</span>)</h2>
        <table>
            <thead><tr><th>ID</th><th>Type</th><th>Payload</th><th>Status</th><th>Retries</th></tr></thead>
            <tbody id="pendingTasks"><tr><td colspan="5" class="empty">No pending tasks</td></tr></tbody>
        </table>
    </div>

    <div class="section">
        <h2>Dead Letter Queue (<span id="dlqCount">0</span>)</h2>
        <table>
            <thead><tr><th>ID</th><th>Type</th><th>Error</th><th>Retries</th></tr></thead>
            <tbody id="dlqTasks"><tr><td colspan="4" class="empty">No failed tasks</td></tr></tbody>
        </table>
    </div>

    <p class="refresh">Auto-refreshes every 2 seconds</p>

    <script>
        async function refresh() {
            try {
                var metricsRes = await fetch('/metrics');
                var metrics = await metricsRes.json();
                document.getElementById('submitted').textContent = metrics.submitted;
                document.getElementById('completed').textContent = metrics.completed;
                document.getElementById('failed').textContent = metrics.failed;
                document.getElementById('rate').textContent = metrics.successRate.toFixed(1) + '%';
                document.getElementById('avgTime').textContent = metrics.avgProcessingMs.toFixed(0) + 'ms';
                document.getElementById('uptime').textContent = formatUptime(metrics.uptimeSeconds);

                var tasksRes = await fetch('/tasks');
                var tasks = await tasksRes.json();
                document.getElementById('pendingCount').textContent = tasks.length;
                if (tasks.length > 0) {
                    document.getElementById('pendingTasks').innerHTML = tasks.map(function(t) {
                        return '<tr><td>' + t.id + '</td><td>' + t.type + '</td><td>' + (t.payload || '-') + '</td><td>' + t.status + '</td><td>' + t.retryCount + '/' + t.maxRetries + '</td></tr>';
                    }).join('');
                } else {
                    document.getElementById('pendingTasks').innerHTML = '<tr><td colspan="5" class="empty">No pending tasks</td></tr>';
                }

                var dlqRes = await fetch('/dlq');
                var dlq = await dlqRes.json();
                document.getElementById('dlqCount').textContent = dlq.length;
                if (dlq.length > 0) {
                    document.getElementById('dlqTasks').innerHTML = dlq.map(function(t) {
                        return '<tr><td>' + t.id + '</td><td>' + t.type + '</td><td>' + (t.errorMessage || '-') + '</td><td>' + t.retryCount + '/' + t.maxRetries + '</td></tr>';
                    }).join('');
                } else {
                    document.getElementById('dlqTasks').innerHTML = '<tr><td colspan="4" class="empty">No failed tasks</td></tr>';
                }
            } catch (e) {
                console.error('Refresh error:', e);
            }
        }

        function formatUptime(seconds) {
            var h = Math.floor(seconds / 3600);
            var m = Math.floor((seconds % 3600) / 60);
            var s = seconds % 60;
            return h + 'h ' + m + 'm ' + s + 's';
        }

        document.getElementById('submitBtn').addEventListener('click', async function() {
            var type = document.getElementById('taskType').value;
            var payload = document.getElementById('payload').value;
            var delay = document.getElementById('delay').value;
            
            var body = { type: type, payload: payload };
            if (delay && delay !== '') {
                body.delay = delay;
            }

            try {
                var response = await fetch('/tasks/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                var result = await response.json();
                
                var statusEl = document.getElementById('statusMsg');
                statusEl.textContent = result.message + ' (ID: ' + result.taskId + ')';
                statusEl.className = 'status show success';
                
                document.getElementById('payload').value = '';
                document.getElementById('delay').value = '';
                
                setTimeout(function() { statusEl.className = 'status'; }, 3000);
                refresh();
            } catch (e) {
                var statusEl = document.getElementById('statusMsg');
                statusEl.textContent = 'Error: ' + e.message;
                statusEl.className = 'status show error';
            }
        });

        refresh();
        setInterval(refresh, 2000);
    </script>
</body>
</html>
""";
    }

    // GET /metrics
    private void handleMetrics(Context ctx) {
        ctx.json(new MetricsResponse(
            metrics.getTasksSubmitted(),
            metrics.getTasksCompleted(),
            metrics.getTasksFailed(),
            metrics.getSuccessRate(),
            metrics.getAvgProcessingTimeMs(),
            metrics.getUptimeSeconds()
        ));
    }

    // POST /tasks/submit
    private void handleSubmit(Context ctx) {
        String type = null;
        String payload = null;
        String delayStr = null;
        
        try {
            var json = ctx.bodyAsClass(TaskRequest.class);
            type = json.type;
            payload = json.payload;
            delayStr = json.delay;
        } catch (Exception e) {
            ctx.status(400).json(new Response("error", "Invalid request"));
            return;
        }

        if (type == null || type.isEmpty()) {
            ctx.status(400).json(new Response("error", "Missing 'type' field"));
            return;
        }

        Task task = new Task(type, payload != null ? payload : "");
        metrics.recordSubmit();
        
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
    record MetricsResponse(long submitted, long completed, long failed, 
                          double successRate, double avgProcessingMs, long uptimeSeconds) {}
}