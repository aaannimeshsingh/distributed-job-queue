package com.jobqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicLong;

public class JobMetrics {
    private static final Logger logger = LoggerFactory.getLogger(JobMetrics.class);
    
    private final AtomicLong jobsProcessed = new AtomicLong(0);
    private final AtomicLong jobsSucceeded = new AtomicLong(0);
    private final AtomicLong jobsFailed = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final long startTime;

    public JobMetrics() {
        this.startTime = System.currentTimeMillis();
    }

    public void recordJobStart() {
        jobsProcessed.incrementAndGet();
    }

    public void recordJobSuccess(long processingTimeMs) {
        jobsSucceeded.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
    }

    public void recordJobFailure() {
        jobsFailed.incrementAndGet();
    }

    public long getJobsProcessed() {
        return jobsProcessed.get();
    }

    public long getJobsSucceeded() {
        return jobsSucceeded.get();
    }

    public long getJobsFailed() {
        return jobsFailed.get();
    }

    public double getSuccessRate() {
        long total = jobsProcessed.get();
        return total == 0 ? 0 : (double) jobsSucceeded.get() / total * 100;
    }

    public double getAverageProcessingTime() {
        long succeeded = jobsSucceeded.get();
        return succeeded == 0 ? 0 : (double) totalProcessingTime.get() / succeeded;
    }

    public double getJobsPerSecond() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed == 0 ? 0 : (double) jobsProcessed.get() / (elapsed / 1000.0);
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * Get database-level statistics
     */
    public static DatabaseStats getDatabaseStats() {
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM jobs) as total, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'completed') as completed, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'failed') as failed, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'pending') as pending, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'processing') as processing, " +
                     "(SELECT AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) FROM jobs WHERE status = 'completed') as avg_duration";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return new DatabaseStats(
                    rs.getInt("total"),
                    rs.getInt("completed"),
                    rs.getInt("failed"),
                    rs.getInt("pending"),
                    rs.getInt("processing"),
                    rs.getDouble("avg_duration")
                );
            }
        } catch (Exception e) {
            logger.error("Failed to get database stats", e);
        }
        return new DatabaseStats(0, 0, 0, 0, 0, 0.0);
    }

    public void printReport() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║        PERFORMANCE METRICS REPORT          ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.printf("║ Uptime:               %8d seconds    ║%n", getUptimeSeconds());
        System.out.printf("║ Jobs Processed:       %8d            ║%n", getJobsProcessed());
        System.out.printf("║ Jobs Succeeded:       %8d            ║%n", getJobsSucceeded());
        System.out.printf("║ Jobs Failed:          %8d            ║%n", getJobsFailed());
        System.out.printf("║ Success Rate:         %8.2f%%          ║%n", getSuccessRate());
        System.out.printf("║ Avg Processing Time:  %8.2f ms        ║%n", getAverageProcessingTime());
        System.out.printf("║ Throughput:           %8.2f jobs/sec  ║%n", getJobsPerSecond());
        System.out.println("╚════════════════════════════════════════════╝\n");
    }

    public static class DatabaseStats {
        public final int total;
        public final int completed;
        public final int failed;
        public final int pending;
        public final int processing;
        public final double avgDuration;

        public DatabaseStats(int total, int completed, int failed, int pending, 
                           int processing, double avgDuration) {
            this.total = total;
            this.completed = completed;
            this.failed = failed;
            this.pending = pending;
            this.processing = processing;
            this.avgDuration = avgDuration;
        }

        public void print() {
            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║         DATABASE STATISTICS                ║");
            System.out.println("╠════════════════════════════════════════════╣");
            System.out.printf("║ Total Jobs:           %8d            ║%n", total);
            System.out.printf("║ Completed:            %8d            ║%n", completed);
            System.out.printf("║ Failed:               %8d            ║%n", failed);
            System.out.printf("║ Pending:              %8d            ║%n", pending);
            System.out.printf("║ Processing:           %8d            ║%n", processing);
            System.out.printf("║ Avg Duration:         %8.2f sec      ║%n", avgDuration);
            System.out.println("╚════════════════════════════════════════════╝\n");
        }
    }
}