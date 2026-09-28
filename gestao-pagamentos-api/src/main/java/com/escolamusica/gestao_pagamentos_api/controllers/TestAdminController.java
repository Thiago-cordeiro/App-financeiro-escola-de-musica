package com.escolamusica.gestao_pagamentos_api.controllers;

import com.escolamusica.gestao_pagamentos_api.dtos.CreateTestAdminRequest;
import com.escolamusica.gestao_pagamentos_api.dtos.TestAdminResponse;
import com.escolamusica.gestao_pagamentos_api.exception.ApiError;
import com.escolamusica.gestao_pagamentos_api.services.TestAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/admins")
@ConditionalOnProperty(prefix = "app.test-admin", name = "enabled", havingValue = "true")
@Tag(name = "Teste", description = "Recursos públicos exclusivos do ambiente de desenvolvimento")
@SecurityRequirements
public class TestAdminController {

    private final TestAdminService testAdminService;

    public TestAdminController(TestAdminService testAdminService) {
        this.testAdminService = testAdminService;
    }

    @PostMapping
    @Operation(
            summary = "Cria um administrador de teste",
            description = "Endpoint público temporário. Disponível somente quando TEST_ADMIN_ENDPOINT_ENABLED=true."
    )
    @ApiResponse(responseCode = "201", description = "Administrador criado")
    @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Email já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    public ResponseEntity<TestAdminResponse> create(@Valid @RequestBody CreateTestAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testAdminService.create(request));
    }
}
