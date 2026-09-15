package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDTO(String type, String description, BigDecimal amount,
                             LocalDateTime createdAt) {
}
