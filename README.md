# JavaQueue

A lightweight, resilient task queue system built in Java 21 featuring concurrent worker pools, exponential backoff retry, dead letter queue, persistence, REST API, and real-time metrics dashboard.

## Features

- **Concurrent Processing** - Multi-threaded worker pool for parallel task execution
- **Retry with Exponential Backoff** - Failed tasks retry with increasing delays (1s → 2s → 4s)
- **Dead Letter Queue (DLQ)** - Permanently failed tasks stored for monitoring
- **Persistence** - Tasks survive restarts using H2 database
- **Delayed Tasks** - Schedule tasks to execute later
- **REST API** - Submit and monitor tasks via HTTP endpoints
- **Real-time Dashboard** - Web UI for monitoring and task submission
- **Metrics Tracking** - Success rate, avg processing time, uptime stats
- **Graceful Shutdown** - Clean shutdown with Ctrl+C

## Screenshots

Dashboard available at `http://localhost:8080`:
- Live metrics (submitted, completed, failed, success rate)
- Submit tasks with optional delay
- View pending tasks and DLQ in real-time
- Auto-refreshes every 2 seconds

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
                     ┌─────────────────┐     ┌──────────────┐
                     │    Dashboard    │     │ Dead Letter Q│
                     │   (Metrics UI)  │     │              │
                     └─────────────────┘     └──────────────┘
```

## Project Structure

```
src/main/java/com/example/
├── App.java                    # Entry point
├── api/
│   └── TaskServer.java         # REST API + Dashboard
├── db/
│   └── DatabaseManager.java    # H2 database operations
├── metrics/
│   └── MetricsCollector.java   # Stats tracking
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

### Open Dashboard

Visit http://localhost:8080 in your browser.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Dashboard UI |
| GET | `/health` | Health check with queue stats |
| GET | `/metrics` | Metrics JSON |
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
curl -X POST http://localhost:8080/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{"type":"email","payload":"user@example.com","delay":"30"}'
```

### Health Check

```bash
curl http://localhost:8080/health
```

### Get Metrics

```bash
curl http://localhost:8080/metrics
```

Response:
```json
{
  "submitted": 10,
  "completed": 8,
  "failed": 1,
  "successRate": 88.9,
  "avgProcessingMs": 1250.5,
  "uptimeSeconds": 3600
}
```

## How It Works

1. **Submit** - Task received via REST API or Dashboard
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

### Metrics

| Metric | Description |
|--------|-------------|
| Submitted | Total tasks submitted |
| Completed | Successfully processed tasks |
| Failed | Tasks that failed permanently |
| Success Rate | Completed / (Completed + Failed) |
| Avg Time | Average processing time in ms |
| Uptime | Server uptime |

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