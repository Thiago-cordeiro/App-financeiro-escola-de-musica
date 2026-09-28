package com.escolamusica.gestao_pagamentos_api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "status_pagamento")
@Getter
@Setter
@NoArgsConstructor
public class StatusPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_status_pagamento")
    private Long idStatusPagamento;

    @NotNull
    @Size(max = 50)
    @Column(name = "nome", nullable = false, unique = true, length = 50)
    private String nome;
}
