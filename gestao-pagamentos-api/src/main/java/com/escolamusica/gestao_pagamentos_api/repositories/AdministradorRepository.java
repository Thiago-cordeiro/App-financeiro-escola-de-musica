package com.escolamusica.gestao_pagamentos_api.repositories;

import com.escolamusica.gestao_pagamentos_api.models.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
