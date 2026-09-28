package com.escolamusica.gestao_pagamentos_api.dtos;

import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import com.escolamusica.gestao_pagamentos_api.models.Role;

public record TestAdminResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active
) {
    public static TestAdminResponse from(Administrador administrador) {
        return new TestAdminResponse(
                administrador.getIdAdministrador(),
                administrador.getNome(),
                administrador.getEmail(),
                administrador.getRole(),
                administrador.getAtivo()
        );
    }
}
