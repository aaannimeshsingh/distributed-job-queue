package com.jobqueue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;

/**
 * Enhanced Job class with dependency support and better typing
 */
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
    
    // New fields
    private JobType jobType;
    private List<Long> dependencies;  // Job IDs this job depends on
    private String tags;  // Comma-separated tags for filtering
    private boolean critical;  // Flag for critical jobs

    // Constructor for creating new jobs
    public Job(JsonObject payload) {
        this.payload = payload;
        this.status = "pending";
        this.priority = 0;
        this.attempts = 0;
        this.maxAttempts = 3;
        this.createdAt = LocalDateTime.now();
        this.scheduledAt = LocalDateTime.now();
        this.dependencies = new ArrayList<>();
        this.critical = false;
        
        // Try to extract job type from payload
        if (payload.has("type")) {
            this.jobType = JobType.fromCode(payload.get("type").getAsString());
        } else {
            this.jobType = JobType.GENERIC;
        }
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
        this.dependencies = new ArrayList<>();
        
        // Extract job type
        if (payload != null && payload.has("type")) {
            this.jobType = JobType.fromCode(payload.get("type").getAsString());
        } else {
            this.jobType = JobType.GENERIC;
        }
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
    
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    
    public List<Long> getDependencies() { return dependencies; }
    public void setDependencies(List<Long> dependencies) { this.dependencies = dependencies; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }
    
    // Utility methods
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }
    
    public void addDependency(Long jobId) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        dependencies.add(jobId);
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", type=" + jobType +
                ", status='" + status + '\'' +
                ", priority=" + priority +
                ", attempts=" + attempts +
                ", critical=" + critical +
                ", dependencies=" + dependencies +
                '}';
    }
}