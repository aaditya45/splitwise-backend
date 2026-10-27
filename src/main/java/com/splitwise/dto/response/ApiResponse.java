package com.splitwise.dto.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ApiResponse<T>(boolean success, String message, T data, int statusCode) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(true, message, data, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return of(true, message, null, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(true, message, data, HttpStatus.CREATED);
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return of(false, message, null, status);
    }

    public static <T> ApiResponse<T> error(String message, T data, HttpStatus status) {
        return of(false, message, data, status);
    }

    private static <T> ApiResponse<T> of(boolean success, String message, T data, HttpStatus status) {
        return new ApiResponse<>(success, message, data, status.value());
    }
}
