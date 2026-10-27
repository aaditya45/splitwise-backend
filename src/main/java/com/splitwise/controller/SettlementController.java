package com.splitwise.controller;

import com.splitwise.dto.BalanceDTO;
import com.splitwise.dto.SettlementDTO;
import com.splitwise.dto.UserDTO;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.model.Settlement;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.SettlementService;
import com.splitwise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final UserService userService;

    @GetMapping("/group/{groupId}/user/pending")
    public ResponseEntity<ApiResponse<List<SettlementDTO>>> getUserPendingSettlements(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<SettlementDTO> settlements = settlementService
                .getUserPendingSettlements(currentUser.getUserId(), groupId).stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Pending settlements fetched successfully", settlements));
    }

    @PostMapping("/{settlementId}/pay")
    public ResponseEntity<ApiResponse<String>> settlePayment(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        settlementService.settlePayment(settlementId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("Payment settled successfully"));
    }

    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGroupSummary(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<BalanceDTO> balances = settlementService.getGroupBalances(groupId, currentUser.getUserId());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("groupId", groupId);
        summary.put("balances", balances);

        return ResponseEntity.ok(ApiResponse.ok("Group summary fetched successfully", summary));
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
                settlement.getStatus().name(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }
}
