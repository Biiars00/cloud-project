package com.tech.feedback_platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(
    @NotBlank
    String descricao,

    @Min(0)
    @Max(10)
    Integer nota
) {
}
