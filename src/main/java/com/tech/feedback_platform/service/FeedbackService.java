package com.tech.feedback_platform.service;

import com.tech.feedback_platform.entity.Feedback;
import com.tech.feedback_platform.dto.FeedbackRequest;
import com.tech.feedback_platform.dto.FeedbackResponse;
import com.tech.feedback_platform.entity.UrgencyLevel;
import com.tech.feedback_platform.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository repository;
    private final NotificationService notificationService;

    public FeedbackResponse criar(FeedbackRequest request) {

        UrgencyLevel urgencia = calcularUrgencia(request.nota());

        Feedback feedback = new Feedback(
                UUID.randomUUID().toString(),
                request.descricao(),
                request.nota(),
                urgencia,
                LocalDateTime.now()
        );

        repository.save(feedback);

        if (urgencia == UrgencyLevel.URGENTE) {
            notificationService.enviarNotificacaoUrgente(feedback);
        }

        return converter(feedback);
    }

    public List<FeedbackResponse> listar() {

        return repository.findAll()
                .stream()
                .map(this::converter)
                .toList();
    }

    private FeedbackResponse converter(Feedback feedback) {

        return new FeedbackResponse(
                feedback.getId(),
                feedback.getDescricao(),
                feedback.getNota(),
                feedback.getUrgencia().name(),
                feedback.getDataEnvio()
        );
    }

    private UrgencyLevel calcularUrgencia(Integer nota) {

        if (nota <= 4) {
            return UrgencyLevel.URGENTE;
        }

        if (nota <= 7) {
            return UrgencyLevel.MEDIA;
        }

        return UrgencyLevel.NORMAL;
    }
}
