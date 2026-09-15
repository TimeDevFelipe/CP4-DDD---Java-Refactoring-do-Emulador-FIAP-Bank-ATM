package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryJdbcImpl implements AccountRepository {
    private final ConnectionFactory connectionFactory;

    public AccountRepositoryJdbcImpl() {
        this.connectionFactory = new ConnectionFactory();
        seedData();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return find("SELECT id, number, pin, balance, daily_limit, withdrawn_today, blocked, failed_attempts " +
                "FROM tb_account WHERE id = ?", id.toString());
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return find("SELECT id, number, pin, balance, daily_limit, withdrawn_today, blocked, failed_attempts " +
                "FROM tb_account WHERE number = ?", accountNumber);
    }

    private Optional<Account> find(String sql, String value) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                Account account = new Account(UUID.fromString(result.getString("id")),
                        result.getString("number"), result.getString("pin"),
                        Money.of(result.getBigDecimal("balance")), Money.of(result.getBigDecimal("daily_limit")),
                        Money.of(result.getBigDecimal("withdrawn_today")), result.getBoolean("blocked"),
                        result.getInt("failed_attempts"));
                loadTransactions(connection, account);
                return Optional.of(account);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erro ao consultar a conta.", exception);
        }
    }

    private void loadTransactions(Connection connection, Account account) throws SQLException {
        String sql = "SELECT id, type, amount, description, created_at FROM tb_transaction " +
                "WHERE account_id = ? ORDER BY created_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getId().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    account.seedTransaction(new Transaction(UUID.fromString(result.getString("id")),
                            LocalDateTime.parse(result.getString("created_at")),
                            TransactionType.valueOf(result.getString("type")),
                            Money.of(result.getBigDecimal("amount")), result.getString("description")));
                }
            }
        }
    }

    @Override
    public void save(Account account) {
        String accountSql = "INSERT INTO tb_account VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET number=excluded.number, pin=excluded.pin, balance=excluded.balance, " +
                "daily_limit=excluded.daily_limit, withdrawn_today=excluded.withdrawn_today, blocked=excluded.blocked, failed_attempts=excluded.failed_attempts";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(accountSql)) {
            statement.setString(1, account.getId().toString());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getAccountNumber().equals("12345") ? "1234" : "5678");
            statement.setBigDecimal(4, account.getBalance().getAmount());
            statement.setBigDecimal(5, account.getDailyWithdrawalLimit().getAmount());
            statement.setBigDecimal(6, account.getTotalWithdrawnToday().getAmount());
            statement.setBoolean(7, account.isBlocked());
            statement.setInt(8, account.getFailedAttempts());
            statement.executeUpdate();
            saveTransactions(connection, account);
        } catch (SQLException exception) {
            throw new IllegalStateException("Erro ao salvar a conta.", exception);
        }
    }

    private void saveTransactions(Connection connection, Account account) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM tb_transaction WHERE account_id = ?")) {
            delete.setString(1, account.getId().toString());
            delete.executeUpdate();
        }
        String sql = "INSERT INTO tb_transaction VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            account.getTransactions().forEach(transaction -> {
                try {
                    statement.setString(1, transaction.getId().toString());
                    statement.setString(2, account.getId().toString());
                    statement.setString(3, transaction.getType().name());
                    statement.setBigDecimal(4, transaction.getAmount().getAmount());
                    statement.setString(5, transaction.getDescription());
                    statement.setString(6, transaction.getTimestamp().toString());
                    statement.addBatch();
                } catch (SQLException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            statement.executeBatch();
        }
    }

    private void seedData() {
        if (findByAccountNumber("12345").isPresent()) return;
        save(new Account(UUID.randomUUID(), "12345", "1234", Money.of(5000), Money.of(1500)));
        save(new Account(UUID.randomUUID(), "67890", "5678", Money.of(1200), Money.of(1000)));
        save(new Account(UUID.randomUUID(), "99999", "9999", Money.of(50), Money.of(500)));
    }
}
