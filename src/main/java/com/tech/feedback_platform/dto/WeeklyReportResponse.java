package com.tech.feedback_platform.dto;

import java.util.Map;

public record WeeklyReportResponse(
    Double mediaAvaliacoes,

    Map<String, Long> quantidadePorDia,

    Map<String, Long> quantidadePorUrgencia
) {
}
