package com.escolamusica.gestao_pagamentos_api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenExpiration,
        String publicKey,
        String privateKey,
        boolean generateKeyPair
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("O issuer JWT deve ser configurado");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("A audience JWT deve ser configurada");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isZero() || accessTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("A expiração do JWT deve ser positiva");
        }
    }
}
