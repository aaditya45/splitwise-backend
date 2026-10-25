package com.splitwise.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateGroupRequest(
    @NotBlank(message = "Group name is required")
        String name,
        String description,
        String groupImage,
        List<Long> memberIds
) {
}
