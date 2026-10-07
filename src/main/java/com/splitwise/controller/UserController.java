package com.splitwise.controller;

import com.splitwise.dto.UserDTO;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.model.User;
import com.splitwise.security.UserDetailsImpl;
import com.splitwise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getUserProfile(Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userService.getUserById(userDetails.getUserId());
            UserDTO userDTO = userService.convertToDTO(user);
            
            ApiResponse<UserDTO> response = ApiResponse.<UserDTO>builder()
                    .success(true)
                    .message("User profile fetched successfully")
                    .data(userDTO)
                    .statusCode(HttpStatus.OK.value())
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<UserDTO> response = ApiResponse.<UserDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            UserDTO userDTO = userService.convertToDTO(user);
            
            ApiResponse<UserDTO> response = ApiResponse.<UserDTO>builder()
                    .success(true)
                    .message("User fetched successfully")
                    .data(userDTO)
                    .statusCode(HttpStatus.OK.value())
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<UserDTO> response = ApiResponse.<UserDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build();
            
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
}
