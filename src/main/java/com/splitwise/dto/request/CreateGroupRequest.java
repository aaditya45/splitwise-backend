package com.splitwise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(max = 255, message = "Group name must be at most 255 characters") String name,
        String description,
        String groupImage,
        List<Long> memberIds
) {
}
