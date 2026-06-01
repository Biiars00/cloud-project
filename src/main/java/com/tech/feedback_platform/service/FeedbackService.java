package com.tech.feedback_platform.service;

import com.tech.feedback_platform.dto.FeedbackRequest;
import com.tech.feedback_platform.dto.FeedbackResponse;
import com.tech.feedback_platform.entity.Feedback;
import com.tech.feedback_platform.repository.FeedbackRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class FeedbackService {

    private final FeedbackRepository repository = new FeedbackRepository();
    private final NotificationService notificationService = new NotificationService();

    public FeedbackResponse criar(FeedbackRequest request) {
        String urgencia = calcularUrgencia(request.getNota());
        String id = UUID.randomUUID().toString();
        String dataEnvioStr = LocalDateTime.now().toString();

        Feedback feedback = Feedback.builder()
                .id(id)
                .descricao(request.getDescricao())
                .nota(request.getNota())
                .urgencia(urgencia)
                .dataEnvio(dataEnvioStr)
                .build();

        repository.save(feedback);

        if ("URGENTE".equals(urgencia)) {
            String corpoEmail = """
                    Novo feedback crítico recebido:
                    
                    Descrição: %s
                    Urgência: %s
                    Data de Envio: %s
                    """.formatted(feedback.getDescricao(), feedback.getUrgencia(), feedback.getDataEnvio());

            notificationService.enviarEmail("Alerta - Feedback Crítico Recebido", corpoEmail);
        }

        return FeedbackResponse.builder()
                .id(feedback.getId())
                .descricao(feedback.getDescricao())
                .nota(feedback.getNota())
                .urgencia(feedback.getUrgencia())
                .dataEnvio(feedback.getDataEnvio())
                .build();
    }

    private String calcularUrgencia(Integer nota) {
        if (nota <= 4) {
            return "URGENTE";
        } else if (nota <= 7) {
            return "MEDIA";
        }
        return "NORMAL";
    }
}