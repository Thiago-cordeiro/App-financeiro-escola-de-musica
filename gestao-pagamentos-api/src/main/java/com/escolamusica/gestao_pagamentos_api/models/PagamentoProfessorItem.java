package com.escolamusica.gestao_pagamentos_api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pagamento_professor_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pagamento_professor_item_pagamento_aluno",
                columnNames = "id_pagamento_aluno"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PagamentoProfessorItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pagamento_professor_item")
    private Long idPagamentoProfessorItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pagamento_professor", nullable = false)
    private PagamentoProfessor pagamentoProfessor;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pagamento_aluno", nullable = false, unique = true)
    private PagamentoAluno pagamentoAluno;

    @NotNull
    @PositiveOrZero
    @Column(name = "valor_repasse", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorRepasse;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
