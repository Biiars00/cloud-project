package com.tech.feedback_platform.function;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.tech.feedback_platform.dto.FeedbackRequest;
import com.tech.feedback_platform.dto.FeedbackResponse;
import com.tech.feedback_platform.service.NotificationService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class SubmitFeedbackFunction {
    private final NotificationService notificationService = new NotificationService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FunctionName("SubmitFeedback")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "avaliacao")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Recebendo um novo feedback na Nuvem Azure.");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Corpo da requisição ausente.").build();
        }

        try {
            FeedbackRequest feedbackReq = objectMapper.readValue(request.getBody().get(), FeedbackRequest.class);

            if (feedbackReq.getNota() == null || feedbackReq.getNota() < 0 || feedbackReq.getNota() > 10) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("A nota informada precisa estar contida entre 0 e 10.").build();
            }

            String urgencia = "NORMAL";
            if (feedbackReq.getNota() <= 4) {
                urgencia = "URGENTE";
            } else if (feedbackReq.getNota() <= 7) {
                urgencia = "MEDIA";
            }

            String id = UUID.randomUUID().toString();
            String dataEnvioStr = LocalDateTime.now().toString();

            String connectionString = System.getenv("AzureWebJobsStorage");
            TableClient tableClient = new TableClientBuilder()
                    .connectionString(connectionString)
                    .tableName("feedbacks")
                    .buildClient();

            TableEntity entity = new TableEntity("Alunos", id)
                    .addProperty("descricao", feedbackReq.getDescricao())
                    .addProperty("nota", feedbackReq.getNota())
                    .addProperty("urgencia", urgencia)
                    .addProperty("dataEnvio", dataEnvioStr);

            tableClient.createEntity(entity);

            if ("URGENTE".equals(urgencia)) {
                String msgEmail = "Aviso de Feedback Crítico Urgente!\n\n" +
                        "Descrição: " + feedbackReq.getDescricao() + "\n" +
                        "Urgência: " + urgencia + "\n" +
                        "Data de Envio: " + dataEnvioStr;
                notificationService.enviarEmail("Alerta - Feedback Crítico Recebido", msgEmail);
            }

            FeedbackResponse responseBody = new FeedbackResponse(id, feedbackReq.getDescricao(), feedbackReq.getNota(), urgencia, dataEnvioStr);
            String jsonResponse = objectMapper.writeValueAsString(responseBody);

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(jsonResponse)
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Falha interna de processamento: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage()).build();
        }
    }
}
