package com.splitwise.service;

import com.splitwise.dto.ExpenseDTO;
import com.splitwise.dto.ExpenseSplitDTO;
import com.splitwise.dto.request.CreateExpenseRequest;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ForbiddenException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.Group;
import com.splitwise.model.User;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final SplitCalculatorService splitCalculatorService;
    private final SettlementService settlementService;

    public Expense createExpense(CreateExpenseRequest request, Long paidByUserId) {
        // The payer must belong to the group, and so must everyone the expense is split with.
        Group group = groupService.getGroupForMember(request.LGroupId(), paidByUserId);
        User paidBy = userService.getUserById(paidByUserId);

        String splitType = resolveSplitType(request.LSplitType());
        BigDecimal amount = request.LAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        Set<User> participants = loadParticipants(request.LParticipantIds(), group);

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setAmount(amount);
        expense.setDescription(request.LDescription());
        expense.setExpenseDate(LocalDateTime.now());
        expense.setParticipants(participants);

        // Splits are owned by the expense (cascade ALL), so they are saved together with it.
        Map<User, BigDecimal> amounts = splitCalculatorService.equalSplit(amount, participants);
        expense.getSplits().addAll(splitCalculatorService.createSplits(expense, amounts, splitType));
        Expense savedExpense = expenseRepository.save(expense);

        settlementService.generateSettlements(group);

        return savedExpense;
    }

    public Expense getExpenseForMember(Long expenseId, Long userId) {
        Expense expense = getExpenseById(expenseId);
        groupService.assertMember(expense.getGroup().getGroupId(), userId);
        return expense;
    }

    public List<Expense> getGroupExpenses(Long groupId, Long userId) {
        groupService.assertMember(groupId, userId);
        return expenseRepository.findByGroupGroupId(groupId);
    }

    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = getExpenseById(expenseId);

        if (!expense.getPaidBy().getUserId().equals(userId)) {
            throw new ForbiddenException("Only the person who paid can delete this expense");
        }

        Group group = expense.getGroup();
        expenseRepository.delete(expense);
        expenseRepository.flush();

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

    private Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
    }

    private String resolveSplitType(String requested) {
        if (requested == null || requested.isBlank()) {
            return SplitCalculatorService.EQUAL;
        }
        String normalized = requested.trim().toUpperCase(Locale.ROOT);
        if (!SplitCalculatorService.EQUAL.equals(normalized)) {
            throw new BadRequestException(
                    "Unsupported split type '" + requested + "'. Only EQUAL is currently supported");
        }
        return normalized;
    }

    /** Loads all participants in one query and checks they exist and belong to the group. */
    private Set<User> loadParticipants(List<Long> participantIds, Group group) {
        Set<Long> requestedIds = new HashSet<>(participantIds);
        List<User> found = userRepository.findAllById(requestedIds);

        if (found.size() != requestedIds.size()) {
            Set<Long> missing = new HashSet<>(requestedIds);
            found.forEach(u -> missing.remove(u.getUserId()));
            throw new ResourceNotFoundException("Participant not found with id: " + missing);
        }

        Set<Long> memberIds = group.getMembers().stream().map(User::getUserId).collect(Collectors.toSet());
        Set<Long> outsiders = new HashSet<>(requestedIds);
        outsiders.removeAll(memberIds);
        if (!outsiders.isEmpty()) {
            throw new BadRequestException("Participants must be members of the group; not members: " + outsiders);
        }

        return new HashSet<>(found);
    }
}
