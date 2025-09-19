package com.syos.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Enhanced Database Connection with auto-recovery and better error handling
 */
public class DatabaseConnection {
    private static final String PROPERTIES_FILE = "/application.properties";
    private static final int CONNECTION_TIMEOUT = 10; // seconds
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static DatabaseConnection instance;
    private Connection connection;
    private String url;
    private String username;
    private String password;

    private DatabaseConnection() {
        loadDatabaseConfig();
        establishConnection();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    private void loadDatabaseConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + PROPERTIES_FILE);
            }

            props.load(input);
            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");

            if (url == null || username == null || password == null) {
                throw new RuntimeException("Database configuration incomplete in " + PROPERTIES_FILE);
            }

            System.out.println("✅ Database configuration loaded successfully");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    private void establishConnection() {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // Load driver explicitly
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Create connection with properties
                Properties connectionProps = new Properties();
                connectionProps.setProperty("user", username);
                connectionProps.setProperty("password", password);
                connectionProps.setProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT * 1000));
                connectionProps.setProperty("socketTimeout", "60000");
                connectionProps.setProperty("autoReconnect", "true");
                connectionProps.setProperty("useSSL", "false");
                connectionProps.setProperty("allowPublicKeyRetrieval", "true");

                this.connection = DriverManager.getConnection(url, connectionProps);

                // Test the connection
                if (connection.isValid(CONNECTION_TIMEOUT)) {
                    System.out.println("✅ Database connection established (attempt " + attempt + ")");
                    return;
                } else {
                    throw new SQLException("Connection validation failed");
                }

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC Driver not found in classpath", e);
            } catch (SQLException e) {
                System.err.println("⚠️  Database connection attempt " + attempt + " failed: " + e.getMessage());

                if (attempt == MAX_RETRY_ATTEMPTS) {
                    System.err.println("❌ All connection attempts failed!");
                    printConnectionTroubleshootingTips(e);
                    throw new RuntimeException("Failed to establish database connection after " + MAX_RETRY_ATTEMPTS + " attempts", e);
                }

                // Wait before retry
                try {
                    Thread.sleep(1000 * attempt); // Progressive delay
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public Connection getConnection() {
        try {
            // Check if connection is still valid
            if (connection == null || connection.isClosed() || !connection.isValid(5)) {
                System.out.println("🔄 Connection invalid, re-establishing...");
                establishConnection();
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("⚠️  Connection check failed, re-establishing...");
            establishConnection();
            return connection;
        }
    }

    public boolean testConnection() {
        try {
            Connection testConn = getConnection();
            boolean isValid = testConn.isValid(CONNECTION_TIMEOUT);

            if (isValid) {
                // Test with a simple query
                var stmt = testConn.createStatement();
                var rs = stmt.executeQuery("SELECT 1 as test");
                boolean hasResult = rs.next() && rs.getInt("test") == 1;
                rs.close();
                stmt.close();

                if (hasResult) {
                    System.out.println("✅ Database connection test: PASSED");
                    return true;
                } else {
                    System.err.println("❌ Database query test failed");
                    return false;
                }
            } else {
                System.err.println("❌ Database connection test: FAILED");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("📤 Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error closing database connection: " + e.getMessage());
        }
    }

    private void printConnectionTroubleshootingTips(SQLException e) {
        System.err.println("\n🔧 Database Connection Troubleshooting:");
        System.err.println("=====================================");

        switch (e.getErrorCode()) {
            case 1049: // Unknown database
                System.err.println("💡 Database doesn't exist. Create it with:");
                System.err.println("   CREATE DATABASE syos_grocery_store;");
                break;
            case 1045: // Access denied
                System.err.println("💡 Access denied. Check your credentials in application.properties");
                break;
            case 0: // Connection refused
                System.err.println("💡 Connection refused. Check if MySQL is running:");
                System.err.println("   - brew services start mysql (macOS)");
                System.err.println("   - sudo systemctl start mysql (Linux)");
                System.err.println("   - net start mysql (Windows)");
                break;
            default:
                System.err.println("💡 General connection issue:");
                System.err.println("   - Verify MySQL is running on localhost:3306");
                System.err.println("   - Check firewall settings");
                System.err.println("   - Verify connection URL format");
        }

        System.err.println("\n📋 Current configuration:");
        System.err.println("   URL: " + url);
        System.err.println("   Username: " + username);
        System.err.println("   Password: " + (password != null ? "[SET]" : "[NOT SET]"));
    }

    // Utility method to run on application startup
    public static void initializeAndTest() {
        System.out.println("🚀 Initializing database connection...");
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();

        if (dbConnection.testConnection()) {
            System.out.println("✅ Database ready for application use");
        } else {
            System.err.println("❌ Database connection failed - application may not work correctly");
            throw new RuntimeException("Database initialization failed");
        }
    }
}