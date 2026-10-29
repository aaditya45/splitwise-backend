package com.splitwise.service;

import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SplitCalculatorService {
    
    /**
     * Calculate equal split for an expense
     * Divides the amount equally among all participants. Leftover cents from rounding
     * go one each to the first participants by user ID, so the splits always sum to the amount.
     */
    public Map<User, BigDecimal> equalSplit(BigDecimal amount, Set<User> participants) {
        Map<User, BigDecimal> splits = new LinkedHashMap<>();

        if (participants.isEmpty()) {
            return splits;
        }

        BigDecimal count = BigDecimal.valueOf(participants.size());
        BigDecimal splitAmount = amount.divide(count, 2, RoundingMode.DOWN);
        BigDecimal cent = new BigDecimal("0.01");
        int leftoverCents = amount.subtract(splitAmount.multiply(count)).divide(cent).intValue();

        List<User> ordered = new ArrayList<>(participants);
        ordered.sort(Comparator.comparing(User::getUserId));
        for (int i = 0; i < ordered.size(); i++) {
            splits.put(ordered.get(i), i < leftoverCents ? splitAmount.add(cent) : splitAmount);
        }

        return splits;
    }
    
    /**
     * Calculate percentage-based split
     */
    public Map<User, BigDecimal> percentageSplit(BigDecimal amount, Map<User, BigDecimal> percentages) {
        Map<User, BigDecimal> splits = new HashMap<>();
        
        for (Map.Entry<User, BigDecimal> entry : percentages.entrySet()) {
            BigDecimal splitAmount = amount.multiply(entry.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            splits.put(entry.getKey(), splitAmount);
        }
        
        return splits;
    }
    
    /**
     * Create expense split records from calculated splits
     */
    public Set<ExpenseSplit> createSplits(Expense expense, Map<User, BigDecimal> splits, String splitType) {
        Set<ExpenseSplit> expenseSplits = new HashSet<>();
        
        for (Map.Entry<User, BigDecimal> entry : splits.entrySet()) {
            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(entry.getKey());
            split.setAmount(entry.getValue());
            split.setSplitType(splitType);
            expenseSplits.add(split);
        }
        
        return expenseSplits;
    }
}
