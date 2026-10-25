package com.splitwise.dto;

import java.math.BigDecimal;

public record ExpenseSplitDTO(Long splitId, UserDTO user, BigDecimal amount, String splitType) {
}
