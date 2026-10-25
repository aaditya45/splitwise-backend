package com.splitwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementDTO(Long settlementId, Long groupId, UserDTO debtor, UserDTO creditor,
                            BigDecimal amount, String status, LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
}
