package com.tech.feedback_platform.service;

import com.tech.feedback_platform.entity.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${mail.to}")
    private String destinatario;

    public void enviarNotificacaoUrgente(Feedback feedback) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setTo(destinatario);

        email.setSubject("Feedback crítico");

        email.setText("""
                Novo feedback crítico recebido

                Descrição: %s
                Urgência: %s
                Data: %s
                """
                .formatted(
                        feedback.getDescricao(),
                        feedback.getUrgencia(),
                        feedback.getDataEnvio()
                ));

        mailSender.send(email);
    }

    public void enviarRelatorio(String mensagem) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setTo(destinatario);

        email.setSubject("Relatório Semanal");

        email.setText(mensagem);

        mailSender.send(email);
    }
}