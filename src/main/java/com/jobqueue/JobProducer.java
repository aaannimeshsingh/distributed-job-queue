package com.jobqueue;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class JobProducer {
    private static final Logger logger = LoggerFactory.getLogger(JobProducer.class);

    /**
     * Submit a new job to the queue
     * @param job The job to submit
     * @return The job ID if successful, null otherwise
     */
    public Long submitJob(Job job) {
        String sql = "INSERT INTO jobs (payload, status, priority, attempts, max_attempts, " +
                     "created_at, scheduled_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, job.getPayload().toString(), java.sql.Types.OTHER);
            stmt.setString(2, job.getStatus());
            stmt.setInt(3, job.getPriority());
            stmt.setInt(4, job.getAttempts());
            stmt.setInt(5, job.getMaxAttempts());
            stmt.setTimestamp(6, Timestamp.valueOf(job.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(job.getScheduledAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long jobId = rs.getLong("id");
                job.setId(jobId);
                logger.info("Job submitted successfully: ID={}, Payload={}", jobId, job.getPayload());
                return jobId;
            }
        } catch (SQLException e) {
            logger.error("Failed to submit job: {}", job.getPayload(), e);
        }
        return null;
    }

    /**
     * Submit a job with custom priority
     * @param payload The job data
     * @param priority Job priority (higher = more important)
     * @return The job ID if successful
     */
    public Long submitJob(JsonObject payload, int priority) {
        Job job = new Job(payload);
        job.setPriority(priority);
        return submitJob(job);
    }

    /**
     * Submit a simple job with default priority
     * @param payload The job data
     * @return The job ID if successful
     */
    public Long submitJob(JsonObject payload) {
        return submitJob(new Job(payload));
    }

    /**
     * Get count of pending jobs
     * @return Number of pending jobs
     */
    public int getPendingJobCount() {
        String sql = "SELECT COUNT(*) FROM jobs WHERE status = 'pending'";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get pending job count", e);
        }
        return 0;
    }
}