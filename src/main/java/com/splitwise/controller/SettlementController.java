package com.splitwise.controller;

import com.splitwise.dto.SettlementDTO;
import com.splitwise.dto.UserDTO;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.model.Group;
import com.splitwise.model.Settlement;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.GroupService;
import com.splitwise.service.SettlementService;
import com.splitwise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {
    
    private final SettlementService settlementService;
    private final UserService userService;
    private final GroupService groupService;
    
    @GetMapping("/group/{groupId}/user/pending")
    public ResponseEntity<ApiResponse<List<SettlementDTO>>> getUserPendingSettlements(
            @PathVariable Long groupId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        groupService.requireMember(groupId, userDetails.getUserId());
        List<Settlement> settlements = settlementService.getUserPendingSettlements(
                userDetails.getUserId(), groupId
        );
        
        List<SettlementDTO> settlementDTOs = settlements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        ApiResponse<List<SettlementDTO>> response = ApiResponse.<List<SettlementDTO>>builder()
                .success(true)
                .message("Pending settlements fetched successfully")
                .data(settlementDTOs)
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PostMapping("/{settlementId}/pay")
    public ResponseEntity<ApiResponse<String>> settlePayment(
            @PathVariable Long settlementId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        settlementService.settlePayment(settlementId, userDetails.getUserId());
        
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Payment settled successfully")
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGroupSummary(
            @PathVariable Long groupId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Group group = groupService.requireMember(groupId, userDetails.getUserId());

        // Positive balance = owed to them, negative = they owe
        List<Map<String, Object>> balances = settlementService.calculateBalances(group).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getUserId()))
                .map(entry -> {
                    Map<String, Object> balance = new HashMap<>();
                    balance.put("user", userService.convertToDTO(entry.getKey()));
                    balance.put("balance", entry.getValue());
                    return balance;
                })
                .collect(Collectors.toList());
        List<SettlementDTO> settlements = settlementService.getGroupPendingSettlements(groupId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("groupId", groupId);
        summary.put("balances", balances);
        summary.put("settlements", settlements);
        
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Group summary fetched successfully")
                .data(summary)
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    private SettlementDTO convertToDTO(Settlement settlement) {
        UserDTO debtorDTO = userService.convertToDTO(settlement.getDebtor());
        UserDTO creditorDTO = userService.convertToDTO(settlement.getCreditor());
        
        return new SettlementDTO(
                settlement.getSettlementId(),
                settlement.getGroup().getGroupId(),
                debtorDTO,
                creditorDTO,
                settlement.getAmount(),
                settlement.getStatus(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }
}
