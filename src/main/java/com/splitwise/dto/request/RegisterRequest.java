package com.splitwise.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank(message = "Name is required")
        String name,
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
        String email,
    @NotBlank(message = "Password is required")
        String password,
    @NotBlank(message = "Phone is required")
        String phone
) {
}
