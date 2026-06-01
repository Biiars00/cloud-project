# Feedback Platform

## Objetivo

Plataforma de coleta de feedbacks para cursos online utilizando arquitetura Serverless na AWS.

## Tecnologias

* Java 21
* Spring Boot
* AWS Lambda
* API Gateway
* DynamoDB
* SNS
* EventBridge
* CloudWatch
* GitHub Actions

## Endpoints

### Criar Feedback

POST /avaliacoes

```json
{
  "descricao": "A aula foi muito rápida",
  "nota": 3
}
```

### Listar Feedbacks

GET /avaliacoes

### Relatório Semanal

GET /relatorios/semanal

## Critérios de Urgência

* 0 a 4 → URGENTE
* 5 a 7 → MEDIA
* 8 a 10 → NORMAL

## Arquitetura

API Gateway → Lambda → DynamoDB

Feedback crítico → SNS → Email

EventBridge → WeeklyReportLambda → SNS

CloudWatch monitora todas as execuções.

## Deploy

```bash
mvn clean package
sam build
sam deploy
```

## Monitoramento

CloudWatch Logs

CloudWatch Metrics

Actuator

/actuator/health

/actuator/metrics
