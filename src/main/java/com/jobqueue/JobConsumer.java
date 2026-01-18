package com.jobqueue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class JobConsumer {
    private static final Logger logger = LoggerFactory.getLogger(JobConsumer.class);
    private final String workerId;
    private final JobMetrics metrics;
    private volatile boolean running = true;

    // Constructor with metrics
    public JobConsumer(String workerId, JobMetrics metrics) {
        this.workerId = workerId;
        this.metrics = metrics;
    }

    // Constructor without metrics (backward compatibility)
    public JobConsumer(String workerId) {
        this.workerId = workerId;
        this.metrics = null;
    }

    /**
     * Fetch the next available job from the queue
     * Uses SELECT FOR UPDATE SKIP LOCKED for concurrency safety
     */
    private Job fetchNextJob() {
        String sql = "SELECT id, payload, status, priority, attempts, max_attempts, " +
                     "created_at, scheduled_at, started_at, completed_at, error " +
                     "FROM jobs " +
                     "WHERE status = 'pending' AND scheduled_at <= NOW() " +
                     "ORDER BY priority DESC, created_at ASC " +
                     "LIMIT 1 " +
                     "FOR UPDATE SKIP LOCKED";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long id = rs.getLong("id");
                JsonObject payload = JsonParser.parseString(rs.getString("payload")).getAsJsonObject();
                String status = rs.getString("status");
                int priority = rs.getInt("priority");
                int attempts = rs.getInt("attempts");
                int maxAttempts = rs.getInt("max_attempts");
                
                Timestamp createdTs = rs.getTimestamp("created_at");
                Timestamp scheduledTs = rs.getTimestamp("scheduled_at");
                Timestamp startedTs = rs.getTimestamp("started_at");
                Timestamp completedTs = rs.getTimestamp("completed_at");
                
                LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
                LocalDateTime scheduledAt = scheduledTs != null ? scheduledTs.toLocalDateTime() : null;
                LocalDateTime startedAt = startedTs != null ? startedTs.toLocalDateTime() : null;
                LocalDateTime completedAt = completedTs != null ? completedTs.toLocalDateTime() : null;
                
                String error = rs.getString("error");

                return new Job(id, payload, status, priority, attempts, maxAttempts,
                             createdAt, scheduledAt, startedAt, completedAt, error);
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch next job", e);
        }
        return null;
    }

    /**
     * Mark job as processing
     */
    private void markJobProcessing(Long jobId) throws SQLException {
        String sql = "UPDATE jobs SET status = 'processing', started_at = ?, attempts = attempts + 1 " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, jobId);
            stmt.executeUpdate();
        }
    }

    /**
     * Mark job as completed
     */
    private void markJobCompleted(Long jobId) throws SQLException {
        String sql = "UPDATE jobs SET status = 'completed', completed_at = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, jobId);
            stmt.executeUpdate();
        }
    }

    /**
     * Mark job as failed
     */
    private void markJobFailed(Long jobId, String error, boolean canRetry) throws SQLException {
        String sql;
        if (canRetry) {
            sql = "UPDATE jobs SET status = 'pending', error = ? WHERE id = ?";
        } else {
            sql = "UPDATE jobs SET status = 'failed', error = ?, completed_at = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, error);
            if (canRetry) {
                stmt.setLong(2, jobId);
            } else {
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setLong(3, jobId);
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Process a single job
     * This is where your actual business logic goes
     */
    private void processJob(Job job) throws Exception {
        logger.info("[{}] Processing job: ID={}, Payload={}", workerId, job.getId(), job.getPayload());

        // BUSINESS LOGIC GOES HERE
        // For demo, we'll simulate different job types
        JsonObject payload = job.getPayload();
        String jobType = payload.has("type") ? payload.get("type").getAsString() : "unknown";

        switch (jobType) {
            case "email":
                // Simulate sending email
                Thread.sleep(100);
                if (payload.has("recipient")) {
                    logger.info("[{}] Email sent to: {}", workerId, payload.get("recipient").getAsString());
                }
                break;
            
            case "process_data":
                // Simulate data processing
                Thread.sleep(150);
                if (payload.has("data")) {
                    logger.info("[{}] Processed data: {}", workerId, payload.get("data"));
                }
                break;
            
            case "generate_report":
                // Simulate report generation
                Thread.sleep(200);
                if (payload.has("reportName")) {
                    logger.info("[{}] Generated report: {}", workerId, payload.get("reportName").getAsString());
                }
                break;
            
            case "generic":
                // Generic job
                Thread.sleep(50);
                logger.info("[{}] Processed generic job", workerId);
                break;
            
            default:
                logger.info("[{}] Processing unknown job type", workerId);
                Thread.sleep(100);
        }
    }

    /**
     * Start consuming jobs
     */
    public void start() {
        logger.info("[{}] Consumer started", workerId);

        while (running) {
            try {
                Job job = fetchNextJob();
                
                if (job == null) {
                    // No jobs available, wait before polling again
                    Thread.sleep(1000);
                    continue;
                }

                // Mark as processing
                markJobProcessing(job.getId());
                
                // Track metrics if available
                if (metrics != null) {
                    metrics.recordJobStart();
                }
                
                long startTime = System.currentTimeMillis();

                try {
                    // Process the job
                    processJob(job);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    // Mark as completed
                    markJobCompleted(job.getId());
                    
                    // Track success metrics
                    if (metrics != null) {
                        metrics.recordJobSuccess(processingTime);
                    }
                    
                    logger.info("[{}] Job completed: ID={} in {}ms", workerId, job.getId(), processingTime);
                    
                } catch (Exception e) {
                    // Job failed
                    logger.error("[{}] Job failed: ID={}", workerId, job.getId(), e);
                    
                    // Check if we can retry
                    boolean canRetry = job.getAttempts() + 1 < job.getMaxAttempts();
                    markJobFailed(job.getId(), e.getMessage(), canRetry);
                    
                    // Track failure metrics
                    if (metrics != null) {
                        metrics.recordJobFailure();
                    }
                    
                    if (canRetry) {
                        logger.info("[{}] Job will be retried: ID={}, Attempt={}/{}", 
                                  workerId, job.getId(), job.getAttempts() + 1, job.getMaxAttempts());
                    } else {
                        logger.error("[{}] Job permanently failed: ID={}", workerId, job.getId());
                    }
                }

            } catch (InterruptedException e) {
                logger.info("[{}] Consumer interrupted", workerId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("[{}] Unexpected error in consumer loop", workerId, e);
            }
        }

        logger.info("[{}] Consumer stopped", workerId);
    }

    /**
     * Stop the consumer
     */
    public void stop() {
        logger.info("[{}] Stopping consumer...", workerId);
        running = false;
    }
}