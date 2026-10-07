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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    
    private final GroupService groupService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<GroupDTO>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Group group = groupService.createGroup(request, userDetails.getUserId());
        GroupDTO groupDTO = groupService.convertToDTO(group);

        ApiResponse<GroupDTO> response = ApiResponse.<GroupDTO>builder()
                .success(true)
                .message("Group created successfully")
                .data(groupDTO)
                .statusCode(HttpStatus.CREATED.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDTO>> getGroup(@PathVariable Long groupId) {
        Group group = groupService.getGroupById(groupId);
        GroupDTO groupDTO = groupService.convertToDTO(group);
        
        ApiResponse<GroupDTO> response = ApiResponse.<GroupDTO>builder()
                .success(true)
                .message("Group fetched successfully")
                .data(groupDTO)
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping("/user/groups")
    public ResponseEntity<ApiResponse<List<GroupDTO>>> getUserGroups(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Group> groups = groupService.getUserGroups(userDetails.getUserId());
        List<GroupDTO> groupDTOs = groups.stream()
                .map(groupService::convertToDTO)
                .collect(Collectors.toList());
        
        ApiResponse<List<GroupDTO>> response = ApiResponse.<List<GroupDTO>>builder()
                .success(true)
                .message("User groups fetched successfully")
                .data(groupDTOs)
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<String>> addMemberToGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        groupService.addMemberToGroup(groupId, userId);
        
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Member added successfully") 
                .statusCode(HttpStatus.OK.value())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}