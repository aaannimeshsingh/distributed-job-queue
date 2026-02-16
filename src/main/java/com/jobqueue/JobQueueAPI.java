package com.jobqueue;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JobQueueAPI {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueAPI.class);
    private static final Gson gson = new Gson();
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    public static void main(String[] args) throws IOException {
        // Test database connection
        if (!DatabaseManager.testConnection()) {
            logger.error("Failed to connect to database");
            System.exit(1);
        }

        // Start workers in background threads
        logger.info("🔧 Starting 3 background workers...");
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            Thread workerThread = new Thread(() -> {
                JobProcessor worker = new JobProcessor(workerId);
                worker.start();
            }, "Worker-" + i);
            workerThread.setDaemon(true);
            workerThread.start();
            logger.info("✅ Started Worker-{}", i);
        }

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Health check
        server.createContext("/health", new CorsEnabledHandler(new HealthHandler()));
        
        // Job submission
        server.createContext("/api/jobs/submit", new CorsEnabledHandler(new SubmitJobHandler()));
        
        // Job statistics
        server.createContext("/api/jobs/stats", new CorsEnabledHandler(new StatsHandler()));
        
        // System metrics
        server.createContext("/api/metrics", new CorsEnabledHandler(new MetricsHandler()));
        
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

    // CORS Handler
    static class CorsEnabledHandler implements HttpHandler {
        private final HttpHandler delegate;

        public CorsEnabledHandler(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            delegate.handle(exchange);
        }
    }

    // Health check handler
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> response = Map.of(
                "status", "healthy",
                "database", DatabaseManager.testConnection() ? "connected" : "disconnected"
            );
            sendJsonResponse(exchange, 200, response);
        }
    }

    // Submit job handler
    static class SubmitJobHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> payload = gson.fromJson(body, Map.class);
                
                long jobId = JobProducer.submitJob(payload);
                sendJsonResponse(exchange, 200, Map.of("success", true, "jobId", jobId));
            } catch (Exception e) {
                logger.error("Error submitting job", e);
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    // Stats handler
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> stats = JobProducer.getStats();
                sendJsonResponse(exchange, 200, stats);
            } catch (Exception e) {
                logger.error("Error getting stats", e);
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    // Metrics handler
    static class MetricsHandler implements HttpHandler {
        private static final long startTime = System.currentTimeMillis();
        private static int jobsProcessed = 0;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            Map<String, Object> stats = JobProducer.getStats();
            
            int completed = ((Number) stats.get("completed")).intValue();
            int failed = ((Number) stats.get("failed")).intValue();
            int total = completed + failed;
            
            double successRate = total > 0 ? (completed * 100.0 / total) : 0.0;
            double throughput = uptime > 0 ? (completed / (double) uptime) : 0.0;
            
            Map<String, Object> metrics = Map.of(
                "jobsProcessed", completed,
                "successRate", successRate,
                "throughput", throughput,
                "uptimeSeconds", uptime
            );
            
            sendJsonResponse(exchange, 200, metrics);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
