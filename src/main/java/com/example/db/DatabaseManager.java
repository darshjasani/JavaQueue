package com.example.db;

import com.example.model.Task;
import com.example.model.TaskStatus;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    
    // DB_CLOSE_ON_EXIT=FALSE ensures clean shutdown
    private static final String DB_URL = "jdbc:h2:./data/javaqueue;DB_CLOSE_ON_EXIT=FALSE";
    private Connection connection;

    public void init() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, "sa", "");
        createTables();
        System.out.println("[DB] Database initialized");
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id VARCHAR(8) PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                payload TEXT,
                status VARCHAR(20) NOT NULL,
                retry_count INT DEFAULT 0,
                max_retries INT DEFAULT 3,
                created_at TIMESTAMP,
                execute_at TIMESTAMP,
                error_message TEXT
            )
            """;
        connection.createStatement().execute(sql);
    }

    // Save new task
    public void save(Task task) throws SQLException {
        String sql = """
            MERGE INTO tasks (id, type, payload, status, retry_count, max_retries, 
                             created_at, execute_at, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, task.getId());
        ps.setString(2, task.getType());
        ps.setString(3, task.getPayload());
        ps.setString(4, task.getStatus().name());
        ps.setInt(5, task.getRetryCount());
        ps.setInt(6, task.getMaxRetries());
        ps.setTimestamp(7, Timestamp.valueOf(task.getCreatedAt()));
        ps.setTimestamp(8, Timestamp.valueOf(task.getExecuteAt()));
        ps.setString(9, task.getErrorMessage());
        ps.executeUpdate();
    }

    // Update task status
    public void update(Task task) throws SQLException {
        save(task); // MERGE handles update
    }

    // Delete completed task
    public void delete(String taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, taskId);
        ps.executeUpdate();
    }

    // Get all pending tasks ready to execute
    public List<Task> getPendingTasks() throws SQLException {
        String sql = """
            SELECT * FROM tasks 
            WHERE status = 'PENDING' AND execute_at <= ? 
            ORDER BY execute_at
            """;
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        return resultToTasks(ps.executeQuery());
    }

    // Get all tasks (for monitoring)
    public List<Task> getAllTasks() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        return resultToTasks(connection.createStatement().executeQuery(sql));
    }

    // Get tasks by status
    public List<Task> getTasksByStatus(TaskStatus status) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE status = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, status.name());
        return resultToTasks(ps.executeQuery());
    }

    // Convert ResultSet to Task list
    private List<Task> resultToTasks(ResultSet rs) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        while (rs.next()) {
            tasks.add(new Task(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("payload"),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_count"),
                rs.getInt("max_retries"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("execute_at").toLocalDateTime(),
                rs.getString("error_message")
            ));
        }
        return tasks;
    }

    public void close() throws SQLException {
        if (connection != null) connection.close();
        System.out.println("[DB] Database closed");
    }
}