package com.escolamusica.gestao_pagamentos_api.bootstrap;

import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import com.escolamusica.gestao_pagamentos_api.models.Role;
import com.escolamusica.gestao_pagamentos_api.repositories.AdministradorRepository;
import com.escolamusica.gestao_pagamentos_api.security.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminBootstrapProperties properties;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            AdminBootstrapProperties properties,
            AdministradorRepository administradorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateProperties();
        String normalizedEmail = EmailNormalizer.normalize(properties.email());

        if (administradorRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            LOGGER.info("O administrador inicial já existe; bootstrap ignorado");
            return;
        }

        Administrador administrador = new Administrador();
        administrador.setNome(properties.name().trim());
        administrador.setEmail(normalizedEmail);
        administrador.setSenhaHash(passwordEncoder.encode(properties.password()));
        administrador.setRole(Role.ADMIN);
        administrador.setAtivo(true);
        administradorRepository.save(administrador);

        LOGGER.info("Administrador inicial criado com sucesso");
    }

    private void validateProperties() {
        if (properties.name() == null || properties.name().isBlank()) {
            throw new IllegalStateException("ADMIN_INITIAL_NAME deve ser informado");
        }
        if (properties.email() == null || properties.email().isBlank() || !properties.email().contains("@")) {
            throw new IllegalStateException("ADMIN_INITIAL_EMAIL deve conter um email válido");
        }
        if (properties.password() == null) {
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD deve ser informado");
        }

        int passwordBytes = properties.password().getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 8 || passwordBytes > 72) {
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD deve possuir entre 8 e 72 bytes UTF-8");
        }
    }
}
