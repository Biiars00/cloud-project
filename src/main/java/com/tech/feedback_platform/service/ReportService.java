package com.tech.feedback_platform.service;

import com.tech.feedback_platform.entity.Feedback;
import com.tech.feedback_platform.dto.WeeklyReportResponse;
import com.tech.feedback_platform.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final FeedbackRepository repository;
    private final NotificationService notificationService;

    public WeeklyReportResponse gerarRelatorio() {

        var feedbacks = repository.findAll();

        double media = feedbacks.stream()
                .mapToInt(Feedback::getNota)
                .average()
                .orElse(0);

        Map<String, Long> porDia =
                feedbacks.stream()
                        .collect(Collectors.groupingBy(
                                feedback ->
                                        feedback.getDataEnvio()
                                                .toLocalDate()
                                                .toString(),
                                Collectors.counting()
                        ));

        Map<String, Long> porUrgencia =
                feedbacks.stream()
                        .collect(Collectors.groupingBy(
                                feedback ->
                                        feedback.getUrgencia().name(),
                                Collectors.counting()
                        ));

        String mensagem = """
        Média: %.2f

        Por Dia:
        %s

        Por Urgência:
        %s
        """
                .formatted(
                        media,
                        porDia,
                        porUrgencia
                );

        notificationService.enviarRelatorio(mensagem);

        return new WeeklyReportResponse(
                media,
                porDia,
                porUrgencia
        );
    }
}
