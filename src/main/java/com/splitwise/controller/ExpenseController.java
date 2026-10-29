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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseDTO>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Expense expense = expenseService.createExpense(request, userDetails.getUserId());
        ExpenseDTO expenseDTO = expenseService.convertToDTO(expense);

        ApiResponse<ExpenseDTO> response = ApiResponse.<ExpenseDTO>builder()
                .success(true)
                .message("Expense created successfully")
                .data(expenseDTO)
                .statusCode(HttpStatus.CREATED.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getExpense(
            @PathVariable Long expenseId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Expense expense = expenseService.getExpenseForMember(expenseId, userDetails.getUserId());
        ExpenseDTO expenseDTO = expenseService.convertToDTO(expense);

        ApiResponse<ExpenseDTO> response = ApiResponse.<ExpenseDTO>builder()
                .success(true)
                .message("Expense fetched successfully")
                .data(expenseDTO)
                .statusCode(HttpStatus.OK.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<ExpenseDTO>>> getGroupExpenses(
            @PathVariable Long groupId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Expense> expenses = expenseService.getGroupExpenses(groupId, userDetails.getUserId());
        List<ExpenseDTO> expenseDTOs = expenses.stream()
                .map(expenseService::convertToDTO)
                .collect(Collectors.toList());

        ApiResponse<List<ExpenseDTO>> response = ApiResponse.<List<ExpenseDTO>>builder()
                .success(true)
                .message("Group expenses fetched successfully")
                .data(expenseDTOs)
                .statusCode(HttpStatus.OK.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<String>> deleteExpense(
            @PathVariable Long expenseId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        expenseService.deleteExpense(expenseId, userDetails.getUserId());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Expense deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
