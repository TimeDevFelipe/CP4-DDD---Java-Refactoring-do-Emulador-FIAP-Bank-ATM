package com.fiap.bank.atm.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionFactory {
    private final String url;

    public ConnectionFactory() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Driver SQLite não encontrado.", exception);
        }
        try {
            Files.createDirectories(Path.of("data"));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível criar a pasta do banco.", exception);
        }
        this.url = "jdbc:sqlite:data/fiap-bank.db";
        createTables();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTables() {
        String accountSql = "CREATE TABLE IF NOT EXISTS tb_account (" +
                "id VARCHAR(36) PRIMARY KEY, number VARCHAR(20) NOT NULL UNIQUE," +
                "pin VARCHAR(10) NOT NULL, balance DECIMAL(15,2) NOT NULL," +
                "daily_limit DECIMAL(15,2) NOT NULL, withdrawn_today DECIMAL(15,2) NOT NULL," +
                "blocked INTEGER NOT NULL, failed_attempts INTEGER NOT NULL)";
        String transactionSql = "CREATE TABLE IF NOT EXISTS tb_transaction (" +
                "id VARCHAR(36) PRIMARY KEY, account_id VARCHAR(36) NOT NULL," +
                "type VARCHAR(20) NOT NULL, amount DECIMAL(15,2) NOT NULL," +
                "description VARCHAR(200) NOT NULL, created_at TIMESTAMP NOT NULL)";
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(accountSql);
            statement.execute(transactionSql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível preparar o banco.", exception);
        }
    }
}
