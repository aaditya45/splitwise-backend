package com.splitwise.dto.response;

import lombok.Builder;

@Builder
public record ApiResponse<T>(boolean success, String message, T data, int statusCode) {
}
