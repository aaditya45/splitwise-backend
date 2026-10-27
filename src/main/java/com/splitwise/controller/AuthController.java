package com.splitwise.controller;

import com.splitwise.dto.request.LoginRequest;
import com.splitwise.dto.request.RegisterRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.dto.response.AuthResponse;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.model.User;
import com.splitwise.security.JwtTokenProvider;
import com.splitwise.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", toAuthResponse(token, user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String email = UserService.normalizeEmail(request.email());

        // Wrong credentials throw BadCredentialsException, which the GlobalExceptionHandler maps to 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(ApiResponse.ok("Login successful", toAuthResponse(token, user)));
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(token, "Bearer", user.getUserId(), user.getEmail(), user.getName());
    }
}
