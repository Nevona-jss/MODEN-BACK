package com.moden.modenapi.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for all REST API responses.
 * Provides consistent structure for frontend integration.
 *
 * Example:
 * {
 *   "success": true,
 *   "message": "Operation completed",
 *   "data": {...}
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage<T> {
    private boolean success;
    private String message;
    private T data;

    // ✅ Convenience static methods for clean controller responses

    public static <T> ResponseMessage<T> success(String message, T data) {
        return ResponseMessage.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseMessage<T> success(T data) {
        return ResponseMessage.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ResponseMessage<T> failure(String message) {
        return ResponseMessage.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
