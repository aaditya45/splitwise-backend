package com.splitwise.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name is required")
        String name,
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
        String email,
    @NotBlank(message = "Password is required")
        String password,
    @Size(max = 20, message = "Phone can be at most 20 characters")
        String phone
) {
}
