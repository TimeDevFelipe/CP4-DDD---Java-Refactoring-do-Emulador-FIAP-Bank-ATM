package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.domain.exception.InvalidPinException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.util.List;
import java.util.Optional;

public class AtmService {
    private final AccountRepository accountRepository;
    private Account currentAccount;

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new InvalidPinException("Conta não encontrada."));
        try {
            account.authenticate(pin);
            currentAccount = account;
            return toAccountInfo(account);
        } catch (RuntimeException exception) {
            accountRepository.save(account);
            throw exception;
        }
    }

    public void withdraw(double amount) {
        ensureAuthenticated();
        currentAccount.withdraw(Money.of(amount));
        accountRepository.save(currentAccount);
    }

    public void deposit(double amount) {
        ensureAuthenticated();
        currentAccount.deposit(Money.of(amount));
        accountRepository.save(currentAccount);
    }

    public void transfer(String targetAccountNumber, double amount) {
        ensureAuthenticated();
        Account target = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));
        currentAccount.transfer(target, Money.of(amount));
        accountRepository.save(currentAccount);
        accountRepository.save(target);
    }

    public Optional<AccountInfoDTO> getCurrentAccount() {
        return Optional.ofNullable(currentAccount).map(this::toAccountInfo);
    }

    public Optional<AccountInfoDTO> getBalance() {
        return getCurrentAccount();
    }

    public List<TransactionDTO> getStatement() {
        ensureAuthenticated();
        return currentAccount.getTransactions().stream()
                .map(transaction -> new TransactionDTO(
                        transaction.getType().getDescription(),
                        transaction.getDescription(),
                        transaction.getAmount().getAmount(),
                        transaction.getTimestamp()))
                .toList();
    }

    public void logout() {
        currentAccount = null;
    }

    public boolean isAuthenticated() {
        return currentAccount != null;
    }

    private void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário está autenticado no momento.");
        }
    }

    private AccountInfoDTO toAccountInfo(Account account) {
        Money remaining = account.getDailyWithdrawalLimit().minus(account.getTotalWithdrawnToday());
        return new AccountInfoDTO(account.getId(), account.getAccountNumber(),
                account.getBalance().getAmount(), remaining.getAmount(), account.isBlocked());
    }
}
