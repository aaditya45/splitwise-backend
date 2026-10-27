package com.splitwise.controller;

import com.splitwise.dto.ExpenseDTO;
import com.splitwise.dto.request.CreateExpenseRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.model.Expense;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseDTO>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Expense expense = expenseService.createExpense(request, currentUser.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Expense created successfully", expenseService.convertToDTO(expense)));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getExpense(
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Expense expense = expenseService.getExpenseForMember(expenseId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("Expense fetched successfully", expenseService.convertToDTO(expense)));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<ExpenseDTO>>> getGroupExpenses(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<ExpenseDTO> expenses = expenseService.getGroupExpenses(groupId, currentUser.getUserId()).stream()
                .map(expenseService::convertToDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Group expenses fetched successfully", expenses));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<String>> deleteExpense(
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        expenseService.deleteExpense(expenseId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("Expense deleted successfully"));
    }
}
