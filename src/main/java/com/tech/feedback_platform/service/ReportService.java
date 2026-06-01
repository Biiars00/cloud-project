package com.tech.feedback_platform.service;

import com.tech.feedback_platform.dto.WeeklyReportResponse;
import com.tech.feedback_platform.entity.Feedback;
import com.tech.feedback_platform.repository.FeedbackRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {

    private final FeedbackRepository repository = new FeedbackRepository();
    private final NotificationService notificationService = new NotificationService();

    public WeeklyReportResponse gerarRelatorio() {
        List<Feedback> feedbacks = repository.findAll();

        if (feedbacks.isEmpty()) {
            return new WeeklyReportResponse(0.0, Map.of(), Map.of());
        }

        double media = feedbacks.stream()
                .mapToInt(Feedback::getNota)
                .average()
                .orElse(0.0);

        Map<String, Long> porDia = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getDataEnvio().substring(0, 10),
                        Collectors.counting()
                ));

        Map<String, Long> porUrgencia = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        Feedback::getUrgencia,
                        Collectors.counting()
                ));

        String mensagem = """
                Relatório Periódico de Satisfação de Alunos
                
                Média Geral de Avaliações: %.2f
                
                Volume de Feedbacks Diários:
                %s
                
                Distribuição Analítica por Gravidade:
                %s
                """.formatted(media, porDia, porUrgencia);

        notificationService.enviarEmail("Consolidado Periódico - Plataforma de Feedback", mensagem);

        return new WeeklyReportResponse(media, porDia, porUrgencia);
    }
}