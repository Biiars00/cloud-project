package com.tech.feedback_platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportResponse {
    private Double mediaAvaliacoes;
    private Map<String, Long> quantidadePorDia;
    private Map<String, Long> quantidadePorUrgencia;
}