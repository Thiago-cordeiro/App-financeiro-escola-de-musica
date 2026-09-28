package com.escolamusica.gestao_pagamentos_api.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, path, Instant.now());
    }
}
