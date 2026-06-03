# Feedback Platform

Plataforma serverless para coleta e análise de feedbacks de cursos online. A aplicação recebe avaliações, persiste os dados em **Azure Table Storage**, classifica a urgência automaticamente, dispara alertas por e-mail para feedbacks críticos e gera relatórios periódicos consolidados por meio de uma função agendada.

---

## Sumário

- [Objetivo](#objetivo)
- [Pré-requisitos](#pre-requisitos)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Execução local](#execução-local)
- [Deploy na nuvem](#deploy-na-nuvem)
- [API](#api)
- [Modelo de dados](#modelo-de-dados)
- [Regras de negócio](#regras-de-negócio)
- [Monitoramento](#monitoramento)

---

## Objetivo

1. **Registrar feedbacks** enviados por alunos.
2. **Classificar automaticamente** o nível de urgência com base na nota.
3. **Notificar por e-mail** quando um feedback for classificado como urgente.
4. **Consolidar métricas** (média, volume por dia e distribuição por urgência) e enviar um relatório periódico por e-mail.

---

## 🛠️ Pré-requisitos Locais

1. **Java Development Kit (JDK) 21**
2. **Apache Maven 3.9+**
3. **Azure CLI (`az`)** -> [Link para Download](https://aka.ms/installazurecliwindows)
4. **Azure Functions Core Tools (`func-cli`)** -> Necessário para testes locais.

---

## Estrutura do projeto

```
feedback-platform/
├── .github/workflows/deploy.yml   # Pipeline CI/CD
├── main.bicep                     # Infraestrutura como código
├── pom.xml                        # Dependências e build Maven
├── local.settings.json            # Config local (não versionado)
└── src/main/java/com/tech/feedback_platform/
    ├── function/
    │   ├── SubmitFeedbackFunction.java    # HTTP — receber feedback
    │   └── WeeklyReportFunction.java      # Timer — relatório periódico
    ├── dto/                               # Contratos de entrada/saída
    ├── service/NotificationService.java   # Envio de e-mail SMTP
    └── ...
```

---

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `FUNCTIONS_WORKER_RUNTIME` | Sim | Deve ser `java` |
| `FUNCTIONS_EXTENSION_VERSION` | Sim (Azure) | Versão do host; use `~4` |
| `AzureWebJobsStorage` | Sim | Connection string da Storage Account (runtime + Table Storage) |
| `SMTP_HOST` | Não* | Host SMTP (ex.: `smtp.gmail.com`) |
| `SMTP_PORT` | Não* | Porta SMTP (ex.: `587`) |
| `SMTP_USER` | Não* | Usuário autenticado no SMTP |
| `SMTP_PASSWORD` | Não* | Senha ou app password do SMTP |
| `MAIL_TO` | Não* | Destinatário dos e-mails |
| `APPINSIGHTS_INSTRUMENTATIONKEY` | Azure | Injetada pelo Bicep em produção |

\* Se `SMTP_HOST` ou `SMTP_USER` não estiverem definidos, o `NotificationService` **simula** o e-mail no log (`System.out`), útil para desenvolvimento local.

### Exemplo — `local.settings.json`

> Este arquivo está no `.gitignore`. Crie-o na raiz do projeto:

```json
{
  "IsEncrypted": false,
  "Values": {
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "FUNCTIONS_EXTENSION_VERSION": "~4",
    "AzureWebJobsStorage": "DefaultEndpointsProtocol=https;AccountName=<conta>;AccountKey=<chave>;EndpointSuffix=core.windows.net",
    "SMTP_HOST": "smtp.gmail.com",
    "SMTP_PORT": "587",
    "SMTP_USER": "seu-email@gmail.com",
    "SMTP_PASSWORD": "sua-app-password",
    "MAIL_TO": "destinatario@exemplo.com"
  }
}
```

### Secrets do GitHub Actions

Configure em **Settings → Secrets and variables → Actions**:

| Secret | Uso |
|--------|-----|
| `AZURE_CREDENTIALS` | JSON do Service Principal (`azure/login`) |
| `AZURE_SUBSCRIPTION_ID` | ID da assinatura Azure |
| `SMTP_HOST` | Parâmetro Bicep `smtpHost` |
| `SMTP_PORT` | Parâmetro Bicep `smtpPort` |
| `SMTP_USER` | Parâmetro Bicep `smtpUser` |
| `SMTP_PASSWORD` | Parâmetro Bicep `smtpPassword` |
| `MAIL_TO` | Parâmetro Bicep `mailTo` |

---

## Execução local

### 1. Configurar ambiente

1. Clone o repositório.
2. Crie o `local.settings.json` conforme o exemplo acima.
3. Garanta que a tabela `feedbacks` exista na Storage Account (o Bicep cria em produção; localmente pode usar o portal Azure ou Azurite).

### 2. Compilar e executar

```powershell
mvn clean package azure-functions:run "-Dmaven.test.skip=true"
```

O host sobe, por padrão, em:

```
http://localhost:7071
```

### 3. Testar o endpoint

```bash
curl -X POST http://localhost:7071/api/avaliacao \
  -H "Content-Type: application/json" \
  -d "{\"descricao\": \"A aula foi muito rápida\", \"nota\": 3}"
```

---

## Deploy na nuvem

### Automático (GitHub Actions)

A cada push na branch `master`, o workflow `.github/workflows/deploy.yml`:

1. Autentica na Azure.
2. Gera `params.json` com secrets SMTP.
3. Executa deploy do `main.bicep` (infraestrutura).
4. Compila o projeto (`mvn clean package azure-functions:package`).
5. Publica o pacote na Function App `fnapp-feedback-platform`.

### Manual (CLI)

```bash
# Login e deploy da infraestrutura
az login
az deployment group create \
  --resource-group rg-techchallenge4-prod \
  --template-file main.bicep \
  --parameters smtpHost='smtp.gmail.com' smtpPort='587' \
               smtpUser='usuario' smtpPassword='senha' mailTo='destino@email.com'

# Build e publicação do código
mvn clean package azure-functions:package
mvn azure-functions:deploy
```

### URL em produção

Após o deploy:

```
https://fnapp-feedback-platform.azurewebsites.net/api/avaliacao
```

---

## API

### Visão geral

| Função | Tipo | Rota | Descrição |
|--------|------|------|-----------|
| `SubmitFeedback` | HTTP `POST` | `/api/avaliacao` | Registra um novo feedback |
| `GenerateWeeklyReport` | Timer (cron) | — | Gera e envia relatório por e-mail (não expõe HTTP) |

> Não há endpoints HTTP de listagem ou consulta de relatório; o relatório é enviado exclusivamente por e-mail.

---

### `POST /api/avaliacao`

Registra um feedback de aluno.

**Headers**

| Header | Valor |
|--------|-------|
| `Content-Type` | `application/json` |

**Corpo da requisição**

```json
{
  "descricao": "A aula foi muito rápida",
  "nota": 3
}
```

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `descricao` | `string` | Não* | Texto livre do feedback |
| `nota` | `integer` | Sim | Nota de 0 a 10 |

**Respostas**

#### `201 Created` — sucesso

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "descricao": "A aula foi muito rápida",
  "nota": 3,
  "urgencia": "URGENTE",
  "dataEnvio": "2026-06-01T14:30:00.123456789"
}
```

#### `400 Bad Request` — corpo ausente

```
Corpo da requisição ausente.
```

#### `400 Bad Request` — nota inválida

```
A nota informada precisa estar contida entre 0 e 10.
```

#### `500 Internal Server Error` — falha inesperada

```
Erro interno: <mensagem>
```

**Exemplo — cURL (produção)**

```bash
curl -X POST "https://fnapp-feedback-platform.azurewebsites.net/api/avaliacao" \
  -H "Content-Type: application/json" \
  -d "{\"descricao\": \"Excelente conteúdo\", \"nota\": 9}"
```

**Exemplo — PowerShell**

```bash
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:7071/api/avaliacao" `
  -ContentType "application/json" `
  -Body '{"descricao":"Conteúdo difícil de acompanhar","nota":5}'
```

---

### `GenerateWeeklyReport` (Timer)

Função interna acionada por agendamento — **não possui endpoint REST**.

**Agendamento atual (cron NCRONTAB):**

```
0 */5 * * * *
```

Executa **a cada 5 minutos** (configuração de demonstração). Para relatório semanal real, altere em `WeeklyReportFunction.java` para algo como:

```
0 0 8 * * MON
```

(segundas-feiras às 08:00 UTC)

**Comportamento**

1. Lista todos os registros da tabela `feedbacks`.
2. Calcula média das notas, contagem por dia (`dataEnvio`) e por `urgencia`.
3. Envia e-mail com assunto `Consolidado Periódico - Plataforma de Feedback`.
4. Registra o JSON do relatório nos logs da Function App.

**Estrutura lógica do relatório** (`WeeklyReportResponse`):

```json
{
  "mediaAvaliacoes": 6.75,
  "quantidadePorDia": {
    "2026-06-01": 3,
    "2026-06-02": 1
  },
  "quantidadePorUrgencia": {
    "URGENTE": 1,
    "MEDIA": 2,
    "NORMAL": 1
  }
}
```

---

## Modelo de dados

### Azure Table Storage — tabela `feedbacks`

| Propriedade Azure Tables | Valor / origem |
|--------------------------|----------------|
| `PartitionKey` | Fixo: `Alunos` |
| `RowKey` | UUID gerado na criação |
| `descricao` | Texto do feedback |
| `nota` | Inteiro (0–10) |
| `urgencia` | `URGENTE`, `MEDIA` ou `NORMAL` |
| `dataEnvio` | `LocalDateTime` em formato ISO-8601 (`String`) |

---

## Regras de negócio

### Classificação de urgência

| Nota | Urgência |
|------|----------|
| 0 – 4 | `URGENTE` |
| 5 – 7 | `MEDIA` |
| 8 – 10 | `NORMAL` |

### Notificações por e-mail

| Evento | Condição | Assunto do e-mail |
|--------|----------|-------------------|
| Alerta crítico | `urgencia == URGENTE` no `POST` | `Alerta - Feedback Crítico Recebido` |
| Relatório periódico | Timer disparado e existem registros | `Consolidado Periódico - Plataforma de Feedback` |

---

## Monitoramento

- **Application Insights** (`appinsights-feedback-platform`): traces das functions, falhas e dependências.
- **Logs da Function App**: Portal Azure → Function App → **Log stream** ou **Application Insights → Logs**.

---

## Autora

Beatriz Ribeiro

