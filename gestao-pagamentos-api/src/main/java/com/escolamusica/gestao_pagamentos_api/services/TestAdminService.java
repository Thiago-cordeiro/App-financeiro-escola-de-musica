package com.escolamusica.gestao_pagamentos_api.services;

import com.escolamusica.gestao_pagamentos_api.dtos.CreateTestAdminRequest;
import com.escolamusica.gestao_pagamentos_api.dtos.TestAdminResponse;
import com.escolamusica.gestao_pagamentos_api.exception.InvalidRequestException;
import com.escolamusica.gestao_pagamentos_api.exception.ResourceAlreadyExistsException;
import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import com.escolamusica.gestao_pagamentos_api.models.Role;
import com.escolamusica.gestao_pagamentos_api.repositories.AdministradorRepository;
import com.escolamusica.gestao_pagamentos_api.security.EmailNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class TestAdminService {

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    public TestAdminService(
            AdministradorRepository administradorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TestAdminResponse create(CreateTestAdminRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        if (administradorRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceAlreadyExistsException("Já existe um administrador com este email");
        }

        int passwordBytes = request.password().getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes > 72) {
            throw new InvalidRequestException();
        }

        Administrador administrador = new Administrador();
        administrador.setNome(request.name().trim());
        administrador.setEmail(normalizedEmail);
        administrador.setSenhaHash(passwordEncoder.encode(request.password()));
        administrador.setRole(Role.ADMIN);
        administrador.setAtivo(true);

        try {
            return TestAdminResponse.from(administradorRepository.saveAndFlush(administrador));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceAlreadyExistsException("Já existe um administrador com este email");
        }
    }
}
