package com.tech.feedback_platform.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class NotificationService {

    public void enviarEmail(String assunto, String corpo) {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv("SMTP_PORT");
        final String usuario = System.getenv("SMTP_USER");
        final String senha = System.getenv("SMTP_PASSWORD");
        String destinatario = System.getenv("MAIL_TO");

        // Se as configurações de e-mail não estiverem prontas, previne que a gravação do log caia
        if (host == null || usuario == null) {
            System.out.println("====== SIMULAÇÃO DE EMAIL CLOUD ======");
            System.out.println("Assunto: " + assunto);
            System.out.println("Corpo:\n" + corpo);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, senha);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(usuario));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(assunto);
            message.setText(corpo);
            Transport.send(message);
        } catch (MessagingException e) {
            System.err.println("Erro ao disparar notificação Cloud por email: " + e.getMessage());
        }
    }
}