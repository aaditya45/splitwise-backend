package com.splitwise.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record GroupDTO(Long groupId, String name, String description, String groupImage, UserDTO createdBy,
                      Set<UserDTO> members, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
