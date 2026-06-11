package com.tech.feedback_platform.function;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.tech.feedback_platform.dto.WeeklyReportResponse;
import com.tech.feedback_platform.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class WeeklyReportFunction {
    private final NotificationService notificationService = new NotificationService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("GenerateWeeklyReport")
    public void run(
            @TimerTrigger(name = "weeklyReportTrigger", schedule = "0 0 0 1 */6 *") @SuppressWarnings("unused") String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("Geração automatizada de Relatório Periódico de Feedbacks iniciada.");

        try {
            String connectionString = System.getenv("AzureWebJobsStorage");
            if (connectionString == null || connectionString.isEmpty()) {
                throw new IllegalStateException("A variável de ambiente AzureWebJobsStorage não foi configurada.");
            }

            TableServiceClient serviceClient = new TableServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            serviceClient.createTableIfNotExists("feedbacks");

            TableClient tableClient = serviceClient.getTableClient("feedbacks");

            List<TableEntity> entities = new ArrayList<>();
            tableClient.listEntities().forEach(entities::add);

            if (entities.isEmpty()) {
                context.getLogger().info("Nenhum dado encontrado para gerar relatórios.");
                return;
            }

            double media = entities.stream()
                    .mapToInt(e -> (Integer) e.getProperty("nota"))
                    .average()
                    .orElse(0.0);

            Map<String, Long> porDia = entities.stream()
                    .collect(Collectors.groupingBy(e -> {
                        String dataStr = (String) e.getProperty("dataEnvio");
                        return dataStr.substring(0, 10);
                    }, Collectors.counting()));

            Map<String, Long> porUrgencia = entities.stream()
                    .collect(Collectors.groupingBy(e -> (String) e.getProperty("urgencia"), Collectors.counting()));

            WeeklyReportResponse report = new WeeklyReportResponse(media, porDia, porUrgencia);
            String relatorioJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

            String corpoEmail = "Relatório Periódico de Satisfação de Alunos\n\n" +
                    "Média Geral de Avaliações: " + String.format("%.2f", media) + "\n\n" +
                    "Volume de Feedbacks Diários:\n" + porDia + "\n\n" +
                    "Distribuição Analítica por Gravidade:\n" + porUrgencia;

            notificationService.enviarEmail("Consolidado Periódico - Plataforma de Feedback", corpoEmail);
            context.getLogger().info("Relatório distribuído via e-mail com sucesso. Resumo:\n" + relatorioJson);

        } catch (Exception e) {
            context.getLogger().severe("Erro na execução automática do relatório: " + e.getMessage());
        }
    }
}