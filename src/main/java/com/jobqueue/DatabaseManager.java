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
            
            // Check for DATABASE_URL first (Render/Heroku style)
            String databaseUrl = System.getenv("DATABASE_URL");
            
            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                // Production: Use DATABASE_URL from environment
                logger.info("Using DATABASE_URL from environment");
                
                // Convert postgres:// to jdbc:postgresql:// if needed
                if (databaseUrl.startsWith("postgres://")) {
                    databaseUrl = databaseUrl.replace("postgres://", "jdbc:postgresql://");
                }
                
                config.setJdbcUrl(databaseUrl);
                // Note: Username and password are in the URL
            } else {
                // Local development: Use localhost
                logger.info("Using localhost database for development");
                String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/jobqueue");
                String dbUser = System.getenv().getOrDefault("DB_USER", "animeshsingh");
                String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "");
                
                config.setJdbcUrl(dbUrl);
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
            }
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
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
