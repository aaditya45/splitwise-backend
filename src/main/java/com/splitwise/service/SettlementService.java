package com.splitwise.service;

import com.splitwise.model.Settlement;
import com.splitwise.model.User;
import com.splitwise.model.Group;
import com.splitwise.model.Expense;
import com.splitwise.repository.SettlementRepository;
import com.splitwise.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SettlementService {
    
    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    
    /**
     * Calculate all balances in a group based on expenses
     * Returns map of user balances (positive = owed to them, negative = they owe)
     */
    public Map<User, BigDecimal> calculateBalances(Group group) {
        Map<User, BigDecimal> balances = new HashMap<>();
        
        // Initialize all group members with 0 balance
        for (User member : group.getMembers()) {
            balances.put(member, BigDecimal.ZERO);
        }
        
        // Query expenses directly from database (avoid lazy-loading issues)
        List<Expense> expenses = expenseRepository.findByGroupGroupId(group.getGroupId());
        log.info("Found {} expenses for group {}", expenses.size(), group.getGroupId());
        
        // Process each expense
        for (Expense expense : expenses) {
            log.info("Processing expense {}: amount={}, paidBy={}, splits={}", 
                expense.getExpenseId(), expense.getAmount(), 
                expense.getPaidBy().getName(), expense.getSplits().size());
            
            User paidBy = expense.getPaidBy();
            
            // Add the amount paid by user
            balances.put(paidBy, balances.get(paidBy).add(expense.getAmount()));
            
            // Subtract amount owed by each participant
            expense.getSplits().forEach(split -> {
                User user = split.getUser();
                log.info("  Split: {} owes {}", user.getName(), split.getAmount());
                balances.put(user, balances.get(user).subtract(split.getAmount()));
            });
        }
        
        return balances;
    }
    
    /**
     * Generate settlement records from balances
     * Creates PENDING settlements for users who owe money
     */
    public List<Settlement> generateSettlements(Group group) {
        log.info("=== Generating settlements for group: {} ===", group.getGroupId());
        
        Map<User, BigDecimal> balances = calculateBalances(group);
        log.info("Calculated balances: {}", balances.size());
        balances.forEach((user, balance) -> 
            log.info("User {} ({}) balance: {}", user.getUserId(), user.getName(), balance)
        );
        
        List<Settlement> settlements = new ArrayList<>();
        
        // Clear existing pending settlements for this group
        List<Settlement> existing = settlementRepository.findByGroupGroupId(group.getGroupId());
        existing.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .forEach(settlementRepository::delete);
        
        // Create new settlements
        List<User> users = new ArrayList<>(balances.keySet());
        log.info("Processing {} users for settlement", users.size());
        
        for (int i = 0; i < users.size(); i++) {
            User debtor = users.get(i);
            BigDecimal debtorBalance = balances.get(debtor);
            
            if (debtorBalance.compareTo(BigDecimal.ZERO) < 0) { // Owes money
                log.info("User {} owes: {}", debtor.getName(), debtorBalance.negate());
                BigDecimal amountToPay = debtorBalance.negate();
                
                for (int j = 0; j < users.size(); j++) { // Check ALL users
                    if (i == j) continue; // Skip self
                    
                    User creditor = users.get(j);
                    BigDecimal creditorBalance = balances.get(creditor);
                    
                    if (creditorBalance.compareTo(BigDecimal.ZERO) > 0) { // Should receive money
                        BigDecimal settlementAmount = amountToPay.min(creditorBalance);
                        log.info("Creating settlement: {} owes {} -> {} (amount: {})", 
                            debtor.getName(), creditor.getName(), creditor.getName(), settlementAmount);
                        
                        Settlement settlement = new Settlement();
                        settlement.setGroup(group);
                        settlement.setDebtor(debtor);
                        settlement.setCreditor(creditor);
                        settlement.setAmount(settlementAmount);
                        settlement.setStatus("PENDING");
                        
                        settlements.add(settlement);
                        
                        amountToPay = amountToPay.subtract(settlementAmount);
                        balances.put(creditor, creditorBalance.subtract(settlementAmount));
                        
                        if (amountToPay.compareTo(BigDecimal.ZERO) <= 0) {
                            break;
                        }
                    }
                }
            }
        }
        
        log.info("Saving {} settlements", settlements.size());
        settlements.forEach(s -> log.info("  Settlement: {} owes {} (amount: {}, status: {})", 
            s.getDebtor().getName(), s.getCreditor().getName(), s.getAmount(), s.getStatus()));
        
        List<Settlement> saved = settlementRepository.saveAll(settlements);
        log.info("=== Settlement generation complete (saved {}) ===", saved.size());
        
        return saved;
    }
    
    /**
     * Get pending settlements for a user in a group
     */
    public List<Settlement> getUserPendingSettlements(Long userId, Long groupId) {
        log.info("=== Getting pending settlements for user {} in group {} ===", userId, groupId);
        
        List<Settlement> owes = settlementRepository
                .findByDebtorUserIdAndGroupGroupId(userId, groupId);
        log.info("User owes {} settlements", owes.size());
        owes.forEach(s -> log.info("  Owes: {} -> {} ({})", 
            s.getDebtor().getName(), s.getCreditor().getName(), s.getStatus()));
        
        List<Settlement> owed = settlementRepository
                .findByCreditorUserIdAndGroupGroupId(userId, groupId);
        log.info("User is owed {} settlements", owed.size());
        owed.forEach(s -> log.info("  Owed: {} <- {} ({})", 
            s.getCreditor().getName(), s.getDebtor().getName(), s.getStatus()));
        
        List<Settlement> all = new ArrayList<>(owes);
        all.addAll(owed);
        
        List<Settlement> pending = all.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .collect(Collectors.toList());
        
        log.info("Total pending settlements: {}", pending.size());
        return pending;
    }
    
    /**
     * Mark a settlement as paid
     */
    public void settlePayment(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        
        settlement.setStatus("PAID");
        settlementRepository.save(settlement);
    }
}
