# JavaQueue

A lightweight, resilient task queue system built in Java 21 featuring concurrent worker pools, exponential backoff retry, dead letter queue, and REST API.

## Features

- **Concurrent Processing** - Multi-threaded worker pool for parallel task execution
- **Retry with Exponential Backoff** - Failed tasks retry with increasing delays (1s → 2s → 4s)
- **Dead Letter Queue (DLQ)** - Permanently failed tasks stored for monitoring/manual retry
- **REST API** - Submit and monitor tasks via HTTP endpoints
- **Extensible Handlers** - Easy to add new task types
- **Graceful Shutdown** - Clean shutdown with Ctrl+C

## Architecture

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│   REST API   │────▶│   Task Queue    │────▶│   Workers    │
│   (Javalin)  │     │ (BlockingQueue) │     │ (Thread Pool)│
└──────────────┘     └─────────────────┘     └──────┬───────┘
                              │                     │
                              ▼                     ▼
                     ┌─────────────────┐     ┌──────────────┐
                     │ Dead Letter Q   │◀────│ Retry Logic  │
                     │ (Failed Tasks)  │     │ (Exp Backoff)│
                     └─────────────────┘     └──────────────┘
```

## Project Structure

```
src/main/java/com/example/
├── App.java                    # Entry point
├── api/
│   └── TaskServer.java         # REST API endpoints
├── model/
│   ├── Task.java               # Task entity
│   └── TaskStatus.java         # Status enum
├── queue/
│   ├── TaskQueue.java          # Queue interface
│   ├── InMemoryTaskQueue.java  # Queue implementation
│   └── DeadLetterQueue.java    # Failed tasks storage
├── worker/
│   ├── TaskHandler.java        # Handler interface
│   ├── Worker.java             # Task processor
│   └── WorkerPool.java         # Thread pool manager
├── retry/
│   ├── RetryStrategy.java      # Retry interface
│   └── ExponentialBackoff.java # Backoff implementation
└── handlers/
    ├── EmailTaskHandler.java   # Email task processor
    └── ReportTaskHandler.java  # Report task processor
```

## Tech Stack

- Java 21
- Maven
- Javalin (REST API)
- Jackson (JSON)

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+

### Installation

```bash
# Clone repository
git clone https://github.com/yourusername/javaqueue.git
cd javaqueue

# Build
mvn clean install

# Run
mvn exec:java -Dexec.mainClass="com.example.App"
```

### Verify

```bash
curl http://localhost:8080/health
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check with queue stats |
| POST | `/tasks/submit` | Submit a new task |
| GET | `/tasks` | View pending tasks |
| GET | `/dlq` | View dead letter queue |

## Usage Examples

### Submit Email Task

```bash
curl -X POST http://localhost:8080/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"type":"email","payload":"user@example.com"}'
```

Response:
```json
{"status":"success","message":"Task submitted","taskId":"abc123"}
```

### Submit Report Task

```bash
curl -X POST http://localhost:8080/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"type":"report","payload":"Monthly Sales Report"}'
```

### Health Check

```bash
curl http://localhost:8080/health
```

Response:
```json
{"status":"healthy","pendingTasks":0,"deadTasks":0}
```

### View Dead Letter Queue

```bash
curl http://localhost:8080/dlq
```

## How It Works

1. **Task Submission** - Client sends POST request to `/tasks/submit`
2. **Queue** - Task added to thread-safe BlockingQueue
3. **Worker** - Available worker picks up task from queue
4. **Handler** - Worker finds appropriate handler by task type
5. **Processing** - Handler executes task logic
6. **Success** - Task marked COMPLETED
7. **Failure** - Retry with exponential backoff (max 3 attempts)
8. **Permanent Failure** - Task moved to Dead Letter Queue

### Retry Strategy

| Attempt | Delay |
|---------|-------|
| 1st retry | ~1 second |
| 2nd retry | ~2 seconds |
| 3rd retry | ~4 seconds |
| After 3rd | → Dead Letter Queue |

## Adding Custom Handlers

```java
public class MyTaskHandler implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "mytask";
    }
    
    @Override
    public void handle(Task task) throws Exception {
        // Your logic here
        System.out.println("Processing: " + task.getPayload());
    }
}
```

Register in `App.java`:
```java
pool.registerHandler(new MyTaskHandler());
```