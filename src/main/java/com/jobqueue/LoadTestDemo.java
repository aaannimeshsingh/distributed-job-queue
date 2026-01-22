package com.jobqueue;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadTestDemo {
    private static final Logger logger = LoggerFactory.getLogger(LoadTestDemo.class);

    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║     LOAD TEST - 1000 JOBS, 10 WORKERS     ║");
        logger.info("╚════════════════════════════════════════════╝\n");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }

        // Create metrics tracker
        JobMetrics metrics = new JobMetrics();
        
        // Create producer
        JobProducer producer = new JobProducer();

        // Submit 1000 jobs
        logger.info("=== Submitting 1000 Jobs ===");
        long submitStart = System.currentTimeMillis();
        
        for (int i = 1; i <= 1000; i++) {
            JsonObject job = new JsonObject();
            
            // Mix of job types - FIXED: Traditional switch statement
            String type;
            switch (i % 4) {
                case 0:
                    type = "email";
                    break;
                case 1:
                    type = "process_data";
                    break;
                case 2:
                    type = "generate_report";
                    break;
                default:
                    type = "generic";
                    break;
            }
            
            job.addProperty("type", type);
            job.addProperty("id", "job-" + i);
            job.addProperty("data", "Test data for job " + i);
            
            // Varying priorities
            int priority = i % 10;
            producer.submitJob(job, priority);
            
            // Log progress every 100 jobs
            if (i % 100 == 0) {
                logger.info("Submitted {} jobs...", i);
            }
        }
        
        long submitTime = System.currentTimeMillis() - submitStart;
        logger.info("✓ Submitted 1000 jobs in {}ms ({} jobs/sec)\n", 
                   submitTime, (int)(1000.0 / (submitTime / 1000.0)));

        // Start 10 concurrent workers
        logger.info("=== Starting 10 Workers ===");
        int numWorkers = 10;
        List<JobConsumer> consumers = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

        for (int i = 1; i <= numWorkers; i++) {
            JobConsumer consumer = new JobConsumer("Worker-" + i, metrics);
            consumers.add(consumer);
            executor.submit(consumer::start);
        }

        // Monitor progress
        logger.info("\n⏳ Processing jobs...\n");
        
        Thread monitorThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5000); // Every 5 seconds
                    
                    logger.info("📊 Progress: {} jobs processed, {} jobs/sec", 
                               metrics.getJobsProcessed(), 
                               String.format("%.2f", metrics.getJobsPerSecond()));
                    
                    // Check if done
                    int pending = producer.getPendingJobCount();
                    if (pending == 0 && metrics.getJobsProcessed() >= 1000) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitorThread.start();

        // Wait for completion or timeout (60 seconds)
        try {
            long maxWait = 60000; // 60 seconds
            long startWait = System.currentTimeMillis();
            
            while (producer.getPendingJobCount() > 0) {
                Thread.sleep(1000);
                
                if (System.currentTimeMillis() - startWait > maxWait) {
                    logger.warn("Timeout reached!");
                    break;
                }
            }
            
            // Give a moment for last jobs to complete
            Thread.sleep(2000);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop monitoring
        monitorThread.interrupt();

        // Stop all workers
        logger.info("\n=== Stopping Workers ===");
        for (JobConsumer consumer : consumers) {
            consumer.stop();
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Final report
        logger.info("\n");
        metrics.printReport();
        
        JobMetrics.DatabaseStats dbStats = JobMetrics.getDatabaseStats();
        dbStats.print();

        // Cleanup
        DatabaseManager.close();
        
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║           LOAD TEST COMPLETE               ║");
        logger.info("╚════════════════════════════════════════════╝");
    }
}