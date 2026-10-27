package com.splitwise.controller;

import com.splitwise.dto.GroupDTO;
import com.splitwise.dto.request.CreateGroupRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.model.Group;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupDTO>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Group group = groupService.createGroup(request, currentUser.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Group created successfully", groupService.convertToDTO(group)));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDTO>> getGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Group group = groupService.getGroupForMember(groupId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("Group fetched successfully", groupService.convertToDTO(group)));
    }

    @GetMapping("/user/groups")
    public ResponseEntity<ApiResponse<List<GroupDTO>>> getUserGroups(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<GroupDTO> groups = groupService.getUserGroups(currentUser.getUserId()).stream()
                .map(groupService::convertToDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("User groups fetched successfully", groups));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<String>> addMemberToGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        groupService.addMemberToGroup(groupId, userId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully"));
    }
}
