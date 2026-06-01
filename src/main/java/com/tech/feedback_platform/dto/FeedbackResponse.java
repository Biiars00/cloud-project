package com.tech.feedback_platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private String id;
    private String descricao;
    private Integer nota;
    private String urgencia;
    private String dataEnvio;
}