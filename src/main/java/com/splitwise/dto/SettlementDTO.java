package com.splitwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDTO {
    private Long settlementId;
    private Long groupId;
    private UserDTO debtor;
    private UserDTO creditor;
    private BigDecimal amount;
    private String status; // PENDING, PAID, SETTLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
