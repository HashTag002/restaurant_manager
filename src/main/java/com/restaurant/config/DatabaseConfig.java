package com.restaurant.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String DEFAULT_URL      = "jdbc:postgresql://localhost:5432/restaurant_db";
    private static final String DEFAULT_USER     = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    private static String url      = System.getenv().getOrDefault("DB_URL",      DEFAULT_URL);
    private static String user     = System.getenv().getOrDefault("DB_USER",     DEFAULT_USER);
    private static String password = System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver PostgreSQL introuvable", e);
            }
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public static void setCredentials(String dbUrl, String dbUser, String dbPassword) {
        url      = dbUrl;
        user     = dbUser;
        password = dbPassword;
        connection = null;
    }

    public static boolean testConnection() {
        try {
            Connection c = getConnection();
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}
