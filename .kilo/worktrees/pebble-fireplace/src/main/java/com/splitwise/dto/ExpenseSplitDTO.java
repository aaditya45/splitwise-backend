package com.splitwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplitDTO {
    private Long splitId;
    private UserDTO user;
    private BigDecimal amount;
    private String splitType; // EQUAL, PERCENTAGE, ITEMIZE
}
