package com.tech.feedback_platform.dto;

import java.time.LocalDateTime;

public record FeedbackResponse(
    String id,
    String descricao,
    Integer nota,
    String urgencia,
    LocalDateTime dataEnvio
) {
}
