package com.escolamusica.gestao_pagamentos_api.security;

import com.escolamusica.gestao_pagamentos_api.repositories.AdministradorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String INVALID_CREDENTIALS = "Credenciais inválidas";

    private final AdministradorRepository administradorRepository;

    public CustomUserDetailsService(AdministradorRepository administradorRepository) {
        this.administradorRepository = administradorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return administradorRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email))
                .map(SecurityUser::from)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
    }
}
