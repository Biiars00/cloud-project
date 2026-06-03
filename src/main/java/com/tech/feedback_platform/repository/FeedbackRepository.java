package com.tech.feedback_platform.repository;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.tech.feedback_platform.entity.Feedback;

import java.util.ArrayList;
import java.util.List;

public class FeedbackRepository {

    private TableClient getTableClient() {
        String connectionString = System.getenv("AzureWebJobsStorage");
        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalStateException("A variável de ambiente AzureWebJobsStorage não foi configurada.");
        }

        TableServiceClient serviceClient = new TableServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        serviceClient.createTableIfNotExists("feedbacks");

        return serviceClient.getTableClient("feedbacks");
    }

    public void save(Feedback feedback) {
        TableClient tableClient = getTableClient();

        TableEntity entity = new TableEntity("Alunos", feedback.getId())
                .addProperty("descricao", feedback.getDescricao())
                .addProperty("nota", feedback.getNota())
                .addProperty("urgencia", feedback.getUrgencia())
                .addProperty("dataEnvio", feedback.getDataEnvio());

        tableClient.createEntity(entity);
    }

    public List<Feedback> findAll() {
        TableClient tableClient = getTableClient();
        List<Feedback> feedbacks = new ArrayList<>();

        tableClient.listEntities().forEach(entity -> {
            Feedback feedback = Feedback.builder()
                    .id(entity.getRowKey())
                    .descricao((String) entity.getProperty("descricao"))
                    .nota(Integer.parseInt(entity.getProperty("nota").toString()))
                    .urgencia((String) entity.getProperty("urgencia"))
                    .dataEnvio((String) entity.getProperty("dataEnvio"))
                    .build();
            feedbacks.add(feedback);
        });

        return feedbacks;
    }
}