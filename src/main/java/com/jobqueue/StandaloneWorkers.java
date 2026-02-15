package com.jobqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class StandaloneWorkers {
    private static final Logger logger = LoggerFactory.getLogger(StandaloneWorkers.class);

    public static void main(String[] args) {
        logger.info("=== Starting Workers ===");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }
        logger.info("✓ Database connected");

        List<JobConsumer> consumers = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        JobMetrics metrics = new JobMetrics();

        for (int i = 1; i <= 5; i++) {
            JobConsumer consumer = new JobConsumer("Worker-" + i, metrics);
            consumers.add(consumer);
            Thread thread = new Thread(consumer::start);
            threads.add(thread);
            thread.start();
            logger.info("Started Worker-{}", i);
        }

        logger.info("⏳ Processing jobs... (Ctrl+C to stop)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping workers...");
            consumers.forEach(JobConsumer::stop);
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            consumers.forEach(JobConsumer::stop);
        }
        DatabaseManager.close();
    }
}
