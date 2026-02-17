package com.jobqueue;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class JobQueueAPI {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueAPI.class);
    private static final Gson gson = new Gson();
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws IOException {

        // Test database connection
        if (!DatabaseManager.testConnection()) {
            logger.error("Failed to connect to database");
            System.exit(1);
        }
        logger.info("✅ Database connection successful");

        // Start workers using JobConsumer
        logger.info("🔧 Starting 3 background workers...");
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            Thread workerThread = new Thread(() -> {
                try {
                    JobConsumer consumer = new JobConsumer(String.valueOf(workerId));
                    consumer.start();
                } catch (Exception e) {
                    logger.error("Worker-{} failed to start: {}", workerId, e.getMessage());
                }
            }, "Worker-" + i);
            workerThread.setDaemon(true);
            workerThread.start();
            logger.info("✅ Started Worker-{}", i);
        }

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/health",          new CorsEnabledHandler(new HealthHandler()));
        server.createContext("/api/jobs/submit", new CorsEnabledHandler(new SubmitJobHandler()));
        server.createContext("/api/jobs/stats",  new CorsEnabledHandler(new StatsHandler()));
        server.createContext("/api/metrics",     new CorsEnabledHandler(new MetricsHandler()));
        server.setExecutor(null);
        server.start();

        logger.info("🚀 Job Queue API started on port {}", PORT);
        logger.info("📊 Endpoints:");
        logger.info("  - GET  /health              - Health check");
        logger.info("  - POST /api/jobs/submit     - Submit new job");
        logger.info("  - GET  /api/jobs/stats      - Database statistics");
        logger.info("  - GET  /api/metrics         - System metrics");
        logger.info("👷 Background workers: 3 active");
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("database", DatabaseManager.testConnection() ? "connected" : "disconnected");
            sendJsonResponse(exchange, 200, response);
        }
    }

    static class SubmitJobHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject jsonPayload = gson.fromJson(body, JsonObject.class);

                // Build Job using the full constructor
                int priority = jsonPayload.has("priority") ? jsonPayload.get("priority").getAsInt() : 5;
                LocalDateTime now = LocalDateTime.now();
                Job job = new Job(null, jsonPayload, "pending", priority, 0, 3, now, now, null, null, null);

                JobProducer producer = new JobProducer();
                Long jobId = producer.submitJob(job);

                if (jobId != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("jobId", jobId);
                    sendJsonResponse(exchange, 200, response);
                } else {
                    sendJsonResponse(exchange, 500, Map.of("success", false, "error", "Failed to submit job"));
                }
            } catch (Exception e) {
                logger.error("Error submitting job", e);
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> stats = getJobStats();
                sendJsonResponse(exchange, 200, stats);
            } catch (Exception e) {
                logger.error("Error getting stats", e);
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                long uptime = (System.currentTimeMillis() - startTime) / 1000;
                Map<String, Object> stats = getJobStats();

                int completed = ((Number) stats.get("completed")).intValue();
                int failed    = ((Number) stats.get("failed")).intValue();
                int total     = completed + failed;

                double successRate = total > 0 ? (completed * 100.0 / total) : 0.0;
                double throughput  = uptime > 0 ? (completed / (double) uptime) : 0.0;

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("jobsProcessed", completed);
                metrics.put("successRate", successRate);
                metrics.put("throughput", throughput);
                metrics.put("uptimeSeconds", uptime);
                sendJsonResponse(exchange, 200, metrics);
            } catch (Exception e) {
                logger.error("Error getting metrics", e);
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private static Map<String, Object> getJobStats() throws SQLException {
        String sql = "SELECT status, COUNT(*) as count FROM jobs GROUP BY status";
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", 0);
        stats.put("processing", 0);
        stats.put("completed", 0);
        stats.put("failed", 0);
        stats.put("total", 0);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int total = 0;
            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                stats.put(status, count);
                total += count;
            }
            stats.put("total", total);
        }
        return stats;
    }

    static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
