package com.jobqueue;

import com.google.gson.JsonObject;
import com.jobqueue.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * REST API for Job Queue System
 * Provides HTTP endpoints for job submission and monitoring
 */
public class JobQueueAPI {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueAPI.class);
    private static JobProducer producer;
    private static JobMetrics metrics;
    
    public static void main(String[] args) throws IOException {
        int port = 8080;
        
        // Initialize components
        if (!DatabaseManager.testConnection()) {
            logger.error("Database connection failed!");
            System.exit(1);
        }
        
        producer = new JobProducer();
        metrics = new JobMetrics();
        
        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Register endpoints with CORS support
        server.createContext("/health", new CorsEnabledHandler(new HealthCheckHandler()));
        server.createContext("/api/jobs/submit", new CorsEnabledHandler(new SubmitJobHandler()));
        server.createContext("/api/jobs/stats", new CorsEnabledHandler(new StatsHandler()));
        server.createContext("/api/metrics", new CorsEnabledHandler(new MetricsHandler()));
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        logger.info("🚀 Job Queue API started on port {}", port);
        logger.info("📊 Endpoints:");
        logger.info("  - GET  /health              - Health check");
        logger.info("  - POST /api/jobs/submit     - Submit new job");
        logger.info("  - GET  /api/jobs/stats      - Database statistics");
        logger.info("  - GET  /api/metrics         - System metrics");
    }
    
    // CORS wrapper handler
    static class CorsEnabledHandler implements HttpHandler {
        private final HttpHandler wrappedHandler;
        
        public CorsEnabledHandler(HttpHandler wrappedHandler) {
            this.wrappedHandler = wrappedHandler;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers to all responses
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            // Handle OPTIONS preflight request
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            // Delegate to wrapped handler
            wrappedHandler.handle(exchange);
        }
    }
    
    // Health check endpoint
    static class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"healthy\",\"database\":\"" + 
                (DatabaseManager.testConnection() ? "connected" : "disconnected") + 
                "\"}";
            sendResponse(exchange, 200, response);
        }
    }
    
    // Submit job endpoint
    static class SubmitJobHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject payload = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                
                // Get priority if specified
                int priority = payload.has("priority") ? payload.get("priority").getAsInt() : 5;
                
                // Submit job
                Long jobId = producer.submitJob(payload, priority);
                
                if (jobId != null) {
                    String response = "{\"success\":true,\"jobId\":" + jobId + "}";
                    sendResponse(exchange, 201, response);
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to submit job\"}");
                }
            } catch (Exception e) {
                logger.error("Error submitting job", e);
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    // Database stats endpoint
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JobMetrics.DatabaseStats stats = JobMetrics.getDatabaseStats();
            
            String response = String.format(
                "{\"total\":%d,\"completed\":%d,\"failed\":%d,\"pending\":%d,\"processing\":%d,\"avgDuration\":%.2f}",
                stats.total, stats.completed, stats.failed, stats.pending, stats.processing, stats.avgDuration
            );
            
            sendResponse(exchange, 200, response);
        }
    }
    
    // System metrics endpoint
    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = String.format(
                "{\"jobsProcessed\":%d,\"jobsSucceeded\":%d,\"jobsFailed\":%d,\"successRate\":%.2f,\"throughput\":%.2f,\"uptimeSeconds\":%d}",
                metrics.getJobsProcessed(),
                metrics.getJobsSucceeded(),
                metrics.getJobsFailed(),
                metrics.getSuccessRate(),
                metrics.getJobsPerSecond(),
                metrics.getUptimeSeconds()
            );
            
            sendResponse(exchange, 200, response);
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}