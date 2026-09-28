package com.escolamusica.gestao_pagamentos_api.security;

import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import com.escolamusica.gestao_pagamentos_api.models.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record SecurityUser(
        Long id,
        String username,
        String password,
        Role role,
        boolean enabled
) implements UserDetails {

    public static SecurityUser from(Administrador administrador) {
        return new SecurityUser(
                administrador.getIdAdministrador(),
                administrador.getEmail(),
                administrador.getSenhaHash(),
                administrador.getRole(),
                Boolean.TRUE.equals(administrador.getAtivo())
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
