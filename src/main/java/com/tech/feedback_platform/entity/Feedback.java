package com.tech.feedback_platform.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    private String id;
    private String descricao;
    private Integer nota;
    private String urgencia; // OK, ATENÇÃO, CRÍTICA
    private String dataEnvio;
}