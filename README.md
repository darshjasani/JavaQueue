# JavaQueue

A lightweight, resilient task queue system built in Java 21 featuring concurrent worker pools, exponential backoff retry, dead letter queue, persistence, and REST API.

## Features

- **Concurrent Processing** - Multi-threaded worker pool for parallel task execution
- **Retry with Exponential Backoff** - Failed tasks retry with increasing delays (1s → 2s → 4s)
- **Dead Letter Queue (DLQ)** - Permanently failed tasks stored for monitoring
- **Persistence** - Tasks survive restarts using H2 database
- **Delayed Tasks** - Schedule tasks to execute later
- **REST API** - Submit and monitor tasks via HTTP endpoints
- **Graceful Shutdown** - Clean shutdown with Ctrl+C

## Architecture

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│   REST API   │────▶│   Task Queue    │────▶│   Workers    │
│   (Javalin)  │     │  (Persistent)   │     │ (Thread Pool)│
└──────────────┘     └────────┬────────┘     └──────┬───────┘
                              │                     │
                              ▼                     ▼
                     ┌─────────────────┐     ┌──────────────┐
                     │   H2 Database   │     │ Retry Logic  │
                     │   (Storage)     │     │ (Exp Backoff)│
                     └─────────────────┘     └──────┬───────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │ Dead Letter Q│
                                            └──────────────┘
```

## Project Structure

```
src/main/java/com/example/
├── App.java                    # Entry point
├── api/
│   └── TaskServer.java         # REST API endpoints
├── db/
│   └── DatabaseManager.java    # H2 database operations
├── model/
│   ├── Task.java               # Task entity
│   └── TaskStatus.java         # Status enum
├── queue/
│   ├── TaskQueue.java          # Queue interface
│   ├── InMemoryTaskQueue.java  # In-memory implementation
│   ├── PersistentTaskQueue.java# Persistent implementation
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
- H2 Database (Persistence)

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+

### Installation

```bash
git clone https://github.com/yourusername/javaqueue.git
cd javaqueue
mvn clean install
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

### Submit Immediate Task

```bash
curl -X POST http://localhost:8080/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"type":"email","payload":"user@example.com"}'
```

### Submit Delayed Task

```bash
# Execute after 30 seconds
curl -X POST http://localhost:8080/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"type":"email","payload":"user@example.com","delay":"30"}'
```

### Health Check

```bash
curl http://localhost:8080/health
```

Response:
```json
{"status":"healthy","pendingTasks":0,"deadTasks":0}
```

## How It Works

1. **Submit** - Task received via REST API
2. **Persist** - Task saved to H2 database
3. **Queue** - Task added to in-memory queue (or waits if delayed)
4. **Process** - Worker picks up and executes task
5. **Retry** - On failure, retry with exponential backoff (max 3)
6. **Complete** - Task removed from database
7. **DLQ** - Permanently failed tasks go to Dead Letter Queue

### Retry Strategy

| Attempt | Delay |
|---------|-------|
| 1st | ~1 second |
| 2nd | ~2 seconds |
| 3rd | ~4 seconds |
| After 3rd | → Dead Letter Queue |

### Persistence

Tasks are stored in `./data/javaqueue.mv.db`. On restart, pending tasks automatically resume processing.

## Adding Custom Handlers

```java
public class MyTaskHandler implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "mytask";
    }
    
    @Override
    public void handle(Task task) throws Exception {
        System.out.println("Processing: " + task.getPayload());
    }
}
```

Register in `App.java`:
```java
pool.registerHandler(new MyTaskHandler());
```

## License

MIT