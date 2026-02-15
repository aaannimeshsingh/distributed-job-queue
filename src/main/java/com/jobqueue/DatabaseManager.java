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
            
            String databaseUrl = System.getenv("DATABASE_URL");
            
            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                logger.info("🔧 Raw DATABASE_URL detected: {}", databaseUrl.substring(0, Math.min(20, databaseUrl.length())) + "...");
                
                // Convert postgresql:// to jdbc:postgresql://
                if (databaseUrl.startsWith("postgresql://")) {
                    String remainder = databaseUrl.substring("postgresql://".length());
                    databaseUrl = "jdbc:postgresql://" + remainder;
                    logger.info("✅ Converted to JDBC format");
                } else if (databaseUrl.startsWith("postgres://")) {
                    String remainder = databaseUrl.substring("postgres://".length());
                    databaseUrl = "jdbc:postgresql://" + remainder;
                    logger.info("✅ Converted postgres:// to JDBC format");
                } else if (!databaseUrl.startsWith("jdbc:")) {
                    databaseUrl = "jdbc:" + databaseUrl;
                    logger.info("✅ Added jdbc: prefix");
                }
                
                logger.info("🎯 Final JDBC URL: {}", databaseUrl.substring(0, Math.min(30, databaseUrl.length())) + "...");
                config.setJdbcUrl(databaseUrl);
                
            } else {
                logger.info("📍 Using localhost for development");
                String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/jobqueue");
                String dbUser = System.getenv().getOrDefault("DB_USER", "animeshsingh");
                String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "");
                
                config.setJdbcUrl(dbUrl);
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
            }
            
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
