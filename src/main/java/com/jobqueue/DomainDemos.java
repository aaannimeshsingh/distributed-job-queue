package com.jobqueue;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Domain-specific demonstrations
 * Run these to showcase Financial, Hardware, or Security workflows
 */
public class DomainDemos {
    private static final Logger logger = LoggerFactory.getLogger(DomainDemos.class);

    public static void main(String[] args) {
        if (args.length > 0) {
            String domain = args[0].toLowerCase();
            switch (domain) {
                case "financial" -> runFinancialDemo();
                case "hardware" -> runHardwareDemo();
                case "security" -> runSecurityDemo();
                case "all" -> runAllDomains();
                default -> {
                    System.out.println("Usage: java DomainDemos [financial|hardware|security|all]");
                    runAllDomains();
                }
            }
        } else {
            runAllDomains();
        }
    }

    /**
     * FINANCIAL DOMAIN DEMO (Fidelity)
     * Demonstrates: Trade settlement, risk calculation, fraud detection
     */
    public static void runFinancialDemo() {
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║     FINANCIAL SERVICES DEMO (Fidelity)     ║");
        logger.info("╚════════════════════════════════════════════╝\n");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }

        JobMetrics metrics = new JobMetrics();
        JobProducer producer = new JobProducer();

        logger.info("=== Submitting Financial Jobs ===");

        // Trade settlement jobs
        for (int i = 1; i <= 5; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "trade_settlement");
            job.addProperty("tradeId", "TRADE-2025-" + i);
            job.addProperty("amount", 50000 + i * 10000);
            job.addProperty("currency", "USD");
            producer.submitJob(job, 9); // High priority
        }

        // Risk calculation jobs
        for (int i = 1; i <= 3; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "risk_calculation");
            job.addProperty("portfolio", "PORTFOLIO-" + i);
            job.addProperty("assets", 100 + i * 50);
            producer.submitJob(job, 7);
        }

        // Fraud detection jobs
        for (int i = 1; i <= 4; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "fraud_detection");
            job.addProperty("transactionId", "TXN-" + (1000 + i));
            job.addProperty("amount", 5000 + i * 1000);
            producer.submitJob(job, 10); // Critical priority
        }

        // Regulatory report
        JsonObject regJob = new JsonObject();
        regJob.addProperty("type", "regulatory_report");
        regJob.addProperty("reportType", "SEC-10K");
        regJob.addProperty("quarter", "Q4-2024");
        producer.submitJob(regJob, 8);

        logger.info("Submitted {} financial jobs\n", 13);

        runWorkers(metrics, 3, 30);

        metrics.printReport();
        DatabaseManager.close();
    }

    /**
     * HARDWARE DOMAIN DEMO (Intel)
     * Demonstrates: RTL synthesis, timing analysis, power analysis
     */
    public static void runHardwareDemo() {
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║     HARDWARE/EDA WORKFLOW DEMO (Intel)     ║");
        logger.info("╚════════════════════════════════════════════╝\n");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }

        JobMetrics metrics = new JobMetrics();
        JobProducer producer = new JobProducer();

        logger.info("=== Simulating Chip Design Workflow ===");

        // Chip design workflow: Synthesis -> P&R -> Analysis
        String[] modules = {"cpu_core", "gpu_shader", "memory_controller"};

        for (String module : modules) {
            logger.info("Creating workflow for module: {}", module);

            // Stage 1: RTL Synthesis
            JsonObject synthJob = new JsonObject();
            synthJob.addProperty("type", "rtl_synthesis");
            synthJob.addProperty("module", module);
            synthJob.addProperty("rtlFiles", module + ".v");
            producer.submitJob(synthJob, 8);

            // Stage 2: Place and Route
            JsonObject prJob = new JsonObject();
            prJob.addProperty("type", "place_and_route");
            prJob.addProperty("design", module);
            prJob.addProperty("targetFreq", "2.5GHz");
            producer.submitJob(prJob, 7);

            // Stage 3: Timing Analysis
            JsonObject timingJob = new JsonObject();
            timingJob.addProperty("type", "timing_analysis");
            timingJob.addProperty("design", module);
            timingJob.addProperty("corners", "ss,tt,ff");
            producer.submitJob(timingJob, 9); // Critical

            // Stage 4: Power Analysis (parallel)
            JsonObject powerJob = new JsonObject();
            powerJob.addProperty("type", "power_analysis");
            powerJob.addProperty("design", module);
            producer.submitJob(powerJob, 7);

            // Stage 5: DRC Check
            JsonObject drcJob = new JsonObject();
            drcJob.addProperty("type", "drc_check");
            drcJob.addProperty("layout", module + "_layout.gds");
            producer.submitJob(drcJob, 6);
        }

        logger.info("Submitted {} hardware jobs (5 stages × 3 modules)\n", 15);

        runWorkers(metrics, 3, 35);

        metrics.printReport();
        DatabaseManager.close();
    }

    /**
     * SECURITY DOMAIN DEMO (Intel Security)
     * Demonstrates: Vulnerability scanning, threat detection, compliance
     */
    public static void runSecurityDemo() {
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║     SECURITY OPERATIONS DEMO (Intel)       ║");
        logger.info("╚════════════════════════════════════════════╝\n");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }

        JobMetrics metrics = new JobMetrics();
        JobProducer producer = new JobProducer();

        logger.info("=== Submitting Security Jobs ===");

        // Vulnerability scans
        String[] systems = {"prod-web-01", "prod-db-01", "prod-api-01", "prod-auth-01"};
        for (String system : systems) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "vulnerability_scan");
            job.addProperty("target", system);
            job.addProperty("scanType", "comprehensive");
            producer.submitJob(job, 9); // High priority
        }

        // Log analysis jobs
        for (int i = 1; i <= 3; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "log_analysis");
            job.addProperty("logSource", "firewall-" + i);
            job.addProperty("logCount", 50000 + i * 10000);
            producer.submitJob(job, 6);
        }

        // Threat detection
        JsonObject threatJob = new JsonObject();
        threatJob.addProperty("type", "threat_detection");
        threatJob.addProperty("source", "network_traffic");
        threatJob.addProperty("duration", "1h");
        producer.submitJob(threatJob, 10); // Critical

        // Incident response (simulated alert)
        JsonObject incidentJob = new JsonObject();
        incidentJob.addProperty("type", "incident_response");
        incidentJob.addProperty("incidentId", "INC-2025-001");
        incidentJob.addProperty("severity", "high");
        producer.submitJob(incidentJob, 10); // Critical

        // Compliance checks
        String[] standards = {"SOC2", "ISO27001", "NIST"};
        for (String standard : standards) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "compliance_check");
            job.addProperty("standard", standard);
            producer.submitJob(job, 5);
        }

        logger.info("Submitted {} security jobs\n", 12);

        runWorkers(metrics, 3, 30);

        metrics.printReport();
        DatabaseManager.close();
    }

    /**
     * Run all domain demos in sequence
     */
    public static void runAllDomains() {
        logger.info("╔════════════════════════════════════════════╗");
        logger.info("║     MULTI-DOMAIN DEMONSTRATION             ║");
        logger.info("║  Financial | Hardware/EDA | Security       ║");
        logger.info("╚════════════════════════════════════════════╝\n");

        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            return;
        }

        JobMetrics metrics = new JobMetrics();
        JobProducer producer = new JobProducer();

        logger.info("=== Submitting Mixed Domain Jobs ===");

        // Financial
        for (int i = 1; i <= 3; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "trade_settlement");
            job.addProperty("tradeId", "TRADE-" + i);
            producer.submitJob(job, 9);
        }

        // Hardware
        for (int i = 1; i <= 3; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "timing_analysis");
            job.addProperty("design", "chip_" + i);
            producer.submitJob(job, 8);
        }

        // Security
        for (int i = 1; i <= 3; i++) {
            JsonObject job = new JsonObject();
            job.addProperty("type", "vulnerability_scan");
            job.addProperty("target", "system-" + i);
            producer.submitJob(job, 10);
        }

        logger.info("Submitted 9 jobs across all domains\n");

        runWorkers(metrics, 3, 20);

        metrics.printReport();
        JobMetrics.DatabaseStats dbStats = JobMetrics.getDatabaseStats();
        dbStats.print();

        DatabaseManager.close();
    }

    /**
     * Helper method to run workers
     */
    private static void runWorkers(JobMetrics metrics, int numWorkers, int seconds) {
        logger.info("=== Starting {} Workers ===", numWorkers);
        
        List<JobConsumer> consumers = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

        for (int i = 1; i <= numWorkers; i++) {
            JobConsumer consumer = new JobConsumer("Worker-" + i, metrics);
            consumers.add(consumer);
            executor.submit(consumer::start);
        }

        logger.info("\n⏳ Processing jobs for {} seconds...\n", seconds);

        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

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

        logger.info("\n");
    }
}