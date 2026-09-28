package com.escolamusica.gestao_pagamentos_api.dtos;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
