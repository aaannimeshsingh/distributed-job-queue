package com.jobqueue;

import java.time.LocalDateTime;
import com.google.gson.JsonObject;

public class Job {
    private Long id;
    private JsonObject payload;
    private String status;
    private int priority;
    private int attempts;
    private int maxAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String error;

    // Constructor for creating new jobs
    public Job(JsonObject payload) {
        this.payload = payload;
        this.status = "pending";
        this.priority = 0;
        this.attempts = 0;
        this.maxAttempts = 3;
        this.createdAt = LocalDateTime.now();
        this.scheduledAt = LocalDateTime.now();
    }

    // Constructor for loading jobs from database
    public Job(Long id, JsonObject payload, String status, int priority, 
               int attempts, int maxAttempts, LocalDateTime createdAt,
               LocalDateTime scheduledAt, LocalDateTime startedAt,
               LocalDateTime completedAt, String error) {
        this.id = id;
        this.payload = payload;
        this.status = status;
        this.priority = priority;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.createdAt = createdAt;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.error = error;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public JsonObject getPayload() { return payload; }
    public void setPayload(JsonObject payload) { this.payload = payload; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", priority=" + priority +
                ", attempts=" + attempts +
                ", payload=" + payload +
                '}';
    }
}