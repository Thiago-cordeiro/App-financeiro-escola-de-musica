package com.escolamusica.gestao_pagamentos_api.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException() {
        super("Dados inválidos");
    }
}
