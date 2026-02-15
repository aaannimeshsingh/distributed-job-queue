package com.jobqueue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            
            // Try JDBC_DATABASE_URL first (pre-formatted)
            String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
            
            if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
                logger.info("✅ Using JDBC_DATABASE_URL directly");
                config.setJdbcUrl(jdbcUrl);
            } else {
                // Fallback to DATABASE_URL with conversion
                String databaseUrl = System.getenv("DATABASE_URL");
                
                if (databaseUrl != null && !databaseUrl.isEmpty()) {
                    logger.info("🔧 Converting DATABASE_URL to JDBC format");
                    
                    // Add full hostname if missing
                    if (databaseUrl.contains("dpg-") && !databaseUrl.contains(".render.com")) {
                        databaseUrl = databaseUrl.replace("@dpg-", "@dpg-");
                        databaseUrl = databaseUrl.replaceFirst("@([^/]+)", "@$1.oregon-postgres.render.com");
                        logger.info("✅ Added full hostname");
                    }
                    
                    // Convert to JDBC format
                    if (databaseUrl.startsWith("postgresql://")) {
                        jdbcUrl = "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
                    } else if (databaseUrl.startsWith("postgres://")) {
                        jdbcUrl = "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
                    } else {
                        jdbcUrl = databaseUrl;
                    }
                    
                    config.setJdbcUrl(jdbcUrl);
                    logger.info("✅ Converted to JDBC format");
                } else {
                    // Local development
                    logger.info("📍 Using localhost for development");
                    jdbcUrl = "jdbc:postgresql://localhost:5432/jobqueue";
                    config.setJdbcUrl(jdbcUrl);
                    config.setUsername(System.getenv().getOrDefault("DB_USER", "animeshsingh"));
                    config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", ""));
                }
            }
            
            logger.info("🎯 Final JDBC URL: " + jdbcUrl.substring(0, Math.min(50, jdbcUrl.length())) + "...");
            
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            logger.info("✅ Database connection pool initialized successfully");
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}