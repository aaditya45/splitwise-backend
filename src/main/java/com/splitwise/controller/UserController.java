package com.splitwise.controller;

import com.splitwise.dto.UserDTO;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getUserProfile(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        UserDTO user = userService.convertToDTO(userService.getUserById(currentUser.getUserId()));

        return ResponseEntity.ok(ApiResponse.ok("User profile fetched successfully", user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long userId) {
        UserDTO user = userService.convertToDTO(userService.getUserById(userId));

        return ResponseEntity.ok(ApiResponse.ok("User fetched successfully", user));
    }
}
