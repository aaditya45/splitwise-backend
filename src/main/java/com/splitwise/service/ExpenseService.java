package com.splitwise.service;

import com.splitwise.dto.ExpenseDTO;
import com.splitwise.dto.ExpenseSplitDTO;
import com.splitwise.dto.request.CreateExpenseRequest;
import com.splitwise.exception.ForbiddenException;
import com.splitwise.exception.NotFoundException;
import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.ExpenseSplitRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.splitwise.event.ExpenseCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final SplitCalculatorService splitCalculatorService;
    private final SettlementService settlementService;
    private final ApplicationEventPublisher eventPublisher;

    
    public Expense createExpense(CreateExpenseRequest request, Long paidByUserId) {
        Group group = groupService.requireMember(request.groupId(), paidByUserId);
        User paidBy = userRepository.findById(paidByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only equal splits are supported: the request carries no percentages or item amounts
        String splitType = request.splitType() != null ? request.splitType().toUpperCase() : "EQUAL";
        if (!"EQUAL".equals(splitType)) {
            throw new RuntimeException("Unsupported split type: " + request.splitType() + ". Only EQUAL is supported");
        }

        // Validate all participants exist and belong to the group
        Set<User> participants = new HashSet<>();
        for (Long participantId : request.participantIds()) {
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new RuntimeException("Participant not found with id: " + participantId));
            if (!groupService.isMember(group, participantId)) {
                throw new RuntimeException("Participant " + participantId + " is not a member of this group");
            }
            participants.add(participant);
        }
        
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setAmount(request.amount());
        expense.setDescription(request.description());
        expense.setExpenseDate(LocalDateTime.now());
        expense.setParticipants(participants);
        
        // Save expense first
        Expense savedExpense = expenseRepository.save(expense);
        
        // Calculate and create splits
        Map<User, BigDecimal> splits = splitCalculatorService.equalSplit(request.amount(), participants);
        Set<ExpenseSplit> expenseSplits = splitCalculatorService.createSplits(savedExpense, splits, splitType);
        
        expenseSplitRepository.saveAll(expenseSplits);
        savedExpense.setSplits(expenseSplits);
        
        // RELOAD group with fresh data from DB (includes all expenses)
        Group refreshedGroup = groupService.getGroupById(request.groupId());
        
        // Recalculate settlements
        settlementService.generateSettlements(refreshedGroup);

        publishExpenseCreated(savedExpense, refreshedGroup, paidBy);
        
        return savedExpense;
    }

    private void publishExpenseCreated(Expense expense, Group group, User actor) {
        List<ExpenseCreatedEvent.Recipient> recipients = group.getMembers().stream()
                .map(member -> new ExpenseCreatedEvent.Recipient(
                        member.getUserId(), member.getEmail(), member.getName()))
                .toList();

        eventPublisher.publishEvent(new ExpenseCreatedEvent(
                expense.getExpenseId(),
                group.getGroupId(),
                group.getName(),
                expense.getDescription(),
                expense.getAmount(),
                actor.getUserId(),
                actor.getName(),
                recipients));
    }
    
    public Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found with id: " + expenseId));
    }

    public Expense getExpenseForMember(Long expenseId, Long userId) {
        Expense expense = getExpenseById(expenseId);
        groupService.requireMember(expense.getGroup().getGroupId(), userId);
        return expense;
    }

    public List<Expense> getGroupExpenses(Long groupId, Long userId) {
        groupService.requireMember(groupId, userId);
        return expenseRepository.findByGroupGroupId(groupId);
    }
    
    public List<Expense> getUserExpenses(Long userId) {
        return expenseRepository.findByPaidByUserId(userId);
    }
    
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = getExpenseById(expenseId);
        
        // Verify the user paid this expense
        if (!expense.getPaidBy().getUserId().equals(userId)) {
            throw new ForbiddenException("Only the person who paid can delete this expense");
        }
        
        Group group = expense.getGroup();
        expenseRepository.delete(expense);
        
        // Recalculate settlements after deletion
        settlementService.generateSettlements(group);
    }
    
    public ExpenseDTO convertToDTO(Expense expense) {
        Set<ExpenseSplitDTO> splitDTOs = expense.getSplits().stream()
                .map(split -> new ExpenseSplitDTO(
                        split.getSplitId(),
                        userService.convertToDTO(split.getUser()),
                        split.getAmount(),
                        split.getSplitType()
                ))
                .collect(Collectors.toSet());
        
        return new ExpenseDTO(
                expense.getExpenseId(),
                expense.getGroup().getGroupId(),
                userService.convertToDTO(expense.getPaidBy()),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getParticipants().stream()
                        .map(userService::convertToDTO)
                        .collect(Collectors.toSet()),
                splitDTOs,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
