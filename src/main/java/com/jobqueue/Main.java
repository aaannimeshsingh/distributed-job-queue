package com.jobqueue;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("=== Distributed Job Queue System Starting ===");

        // Test database connection
        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed! Exiting...");
            return;
        }
        logger.info("✓ Database connection successful");

        // Create producer
        JobProducer producer = new JobProducer();

        // Submit some test jobs
        logger.info("\n=== Submitting Jobs ===");
        
        // Job 1: Send email
        JsonObject emailJob = new JsonObject();
        emailJob.addProperty("type", "email");
        emailJob.addProperty("recipient", "user@example.com");
        emailJob.addProperty("subject", "Welcome!");
        emailJob.addProperty("body", "Thanks for signing up!");
        Long job1 = producer.submitJob(emailJob, 10); // High priority
        
        // Job 2: Process data
        JsonObject dataJob = new JsonObject();
        dataJob.addProperty("type", "process_data");
        dataJob.addProperty("data", "User analytics for 2025");
        Long job2 = producer.submitJob(dataJob, 5); // Medium priority
        
        // Job 3: Generate report
        JsonObject reportJob = new JsonObject();
        reportJob.addProperty("type", "generate_report");
        reportJob.addProperty("reportName", "Q1 Sales Report");
        Long job3 = producer.submitJob(reportJob, 1); // Low priority

        logger.info("Submitted {} jobs", 3);
        logger.info("Pending jobs: {}", producer.getPendingJobCount());

        // Start a consumer in a separate thread
        logger.info("\n=== Starting Consumer ===");
        JobConsumer consumer = new JobConsumer("Worker-1");
        
        Thread consumerThread = new Thread(() -> consumer.start());
        consumerThread.start();

        // Let it run for 15 seconds
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop consumer
        logger.info("\n=== Stopping Consumer ===");
        consumer.stop();
        
        try {
            consumerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Final stats
        logger.info("\n=== Final Stats ===");
        logger.info("Remaining pending jobs: {}", producer.getPendingJobCount());
        
        // Cleanup
        DatabaseManager.close();
        logger.info("\n=== System Shutdown Complete ===");
    }
}