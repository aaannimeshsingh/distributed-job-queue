package com.jobqueue;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiWorkerDemo {
    private static final Logger logger = LoggerFactory.getLogger(MultiWorkerDemo.class);

    public static void main(String[] args) {
        logger.info("=== Multi-Worker Job Queue Demo ===\n");

        // Test database
        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }
        logger.info("✓ Database connected\n");

        // Create producer
        JobProducer producer = new JobProducer();

        // Submit 20 jobs of various types
        logger.info("=== Submitting 20 Jobs ===");
        for (int i = 1; i <= 20; i++) {
            JsonObject job = new JsonObject();
            
            if (i % 3 == 0) {
                job.addProperty("type", "email");
                job.addProperty("recipient", "user" + i + "@example.com");
                job.addProperty("subject", "Message " + i);
                producer.submitJob(job, i % 5); // Varying priorities
            } else if (i % 3 == 1) {
                job.addProperty("type", "process_data");
                job.addProperty("data", "Dataset-" + i);
                producer.submitJob(job, i % 5);
            } else {
                job.addProperty("type", "generate_report");
                job.addProperty("reportName", "Report-" + i);
                producer.submitJob(job, i % 5);
            }
        }
        logger.info("Submitted 20 jobs");
        logger.info("Pending jobs: {}\n", producer.getPendingJobCount());

        // Create 5 concurrent workers
        logger.info("=== Starting 5 Concurrent Workers ===");
        int numWorkers = 5;
        List<JobConsumer> consumers = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= numWorkers; i++) {
            JobConsumer consumer = new JobConsumer("Worker-" + i);
            consumers.add(consumer);
            
            Thread thread = new Thread(consumer::start, "Worker-" + i);
            threads.add(thread);
            thread.start();
            logger.info("Started Worker-{}", i);
        }

        // Let them work for 30 seconds
        logger.info("\n⏳ Processing jobs for 30 seconds...\n");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop all workers
        logger.info("\n=== Stopping All Workers ===");
        for (JobConsumer consumer : consumers) {
            consumer.stop();
        }

        // Wait for threads to finish
        for (Thread thread : threads) {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Final statistics
        logger.info("\n=== Final Statistics ===");
        logger.info("Remaining pending jobs: {}", producer.getPendingJobCount());
        
        // Get completion stats from database
        try {
            var stats = getJobStats();
            logger.info("Total jobs processed: {}", stats[0]);
            logger.info("Successful completions: {}", stats[1]);
            logger.info("Failed jobs: {}", stats[2]);
            logger.info("Still pending: {}", stats[3]);
        } catch (Exception e) {
            logger.error("Failed to get stats", e);
        }

        // Cleanup
        DatabaseManager.close();
        logger.info("\n=== Demo Complete ===");
    }

    private static int[] getJobStats() throws Exception {
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM jobs) as total, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'completed') as completed, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'failed') as failed, " +
                     "(SELECT COUNT(*) FROM jobs WHERE status = 'pending') as pending";
        
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return new int[]{
                    rs.getInt("total"),
                    rs.getInt("completed"),
                    rs.getInt("failed"),
                    rs.getInt("pending")
                };
            }
        }
        return new int[]{0, 0, 0, 0};
    }
}