package com.escolamusica.gestao_pagamentos_api.models;

public enum Role {
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
