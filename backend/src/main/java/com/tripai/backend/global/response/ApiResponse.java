package com.tripai.backend.global.response;

public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> emptySuccess() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> failure(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
