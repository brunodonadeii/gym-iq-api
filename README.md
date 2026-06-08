# GymIQ - Backend

Sistema de gestao de academia desenvolvido para PFC/TCC.

Stack: Java 17 | Spring Boot 3.2 | PostgreSQL | Spring Security/JWT | Spring Data JPA/Hibernate | Flyway | Docker

---

## Pre-requisito

Para rodar com Docker:

- Docker Desktop instalado e rodando.

Para rodar localmente sem Docker:

- Java 17.
- Maven.
- PostgreSQL acessivel.
- Variaveis de ambiente configuradas.

---

## Como rodar

```bash
# 1. Clone o repositorio
git clone <URL_DO_REPO>
cd gymiq-backend

# 2. Suba os containers
docker compose up --build
```

Pronto. A API estara em **http://localhost:8080**.

Para rodar em background: `docker compose up --build -d`  
Para parar: `docker compose down`  
Para apagar tudo e recomecar: `docker compose down -v`

---

## Variaveis de ambiente

O backend usa variaveis de ambiente para evitar expor credenciais no codigo.

Exemplo:

```env
DB_URL=jdbc:postgresql://localhost:5432/gymiq
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=troque-por-uma-chave-segura-com-tamanho-suficiente
JWT_EXPIRATION_MS=86400000

PII_ENCRYPTION_KEY=troque-por-uma-chave-de-criptografia-com-32-caracteres-ou-mais
PII_HASH_SECRET=troque-por-uma-chave-de-hash-com-32-caracteres-ou-mais

SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=sua_senha_de_app
MAIL_FROM=seuemail@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

PASSWORD_RESET_FRONTEND_URL=https://gym-iq-web.vercel.app/reset-password
PASSWORD_RESET_EXPIRATION_MINUTES=30
PASSWORD_RESET_RESEND_COOLDOWN_MINUTES=15

CPF_VALIDATION_MODE=LOCAL

PAYMENTS_REFRESH_OVERDUE_CRON=0 50 3 * * *
PAYMENTS_GENERATE_MONTHLY_CRON=0 55 3 * * *
RETENTION_GENERATE_ACTIVE_STUDENTS_CRON=0 0 4 * * *
```

Observacoes:

- `PII_ENCRYPTION_KEY` e `PII_HASH_SECRET` protegem dados pessoais como e-mail, CPF, telefone, endereco e data de nascimento.
- `CPF_VALIDATION_MODE` aceita `LOCAL`, `EXTERNAL` ou `DISABLED`.
- As variaveis de e-mail sao usadas no fluxo de recuperacao de senha.
- Os crons dos jobs usam o formato do Spring com 6 campos: segundo, minuto, hora, dia, mes e dia da semana.
- Os jobs internos rodam com timezone `America/Sao_Paulo`.

---

## Banco de dados e migrations

O projeto usa Flyway para controle de alteracoes no banco.

As migrations ficam em:

```text
src/main/resources/db/migration
```

Configuracao principal:

```properties
spring.flyway.enabled=${SPRING_FLYWAY_ENABLED:true}
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
```

Em ambiente com banco ja existente, o projeto possui uma migration baseline:

```text
V1__baseline_existing_schema.sql
```

Novas alteracoes estruturais de banco devem ser criadas como novas migrations versionadas.

---

## Usuario admin criado automaticamente

| Campo | Valor |
|-------|-------|
| Email | `admin@gymiq.com` |
| Senha | `gymiq@2026` |

O usuario inicial e criado pelo `DataInitializer`.

---

## Testando no Postman

**Login:**

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@gymiq.com",
  "password": "gymiq@2026"
}
```

Use o token retornado no header das proximas requisicoes:

```http
Authorization: Bearer <token>
```

---

## Endpoints principais

Autenticacao:

```http
POST /api/auth/login
POST /api/auth/register
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Usuarios administrativos:

```http
GET /api/users
POST /api/users
PUT /api/users/{id}
DELETE /api/users/{id}
```

Alunos, instrutores e planos:

```http
GET /api/students
GET /api/instructors
GET /api/plans
```

Matriculas e pagamentos:

```http
GET /api/enrollments
POST /api/enrollments
GET /api/payments
PATCH /api/payments/{id}/pay
```

Presencas:

```http
POST /api/presences/self-check-in
GET /api/presences
GET /api/presences/me
```

Dashboards:

```http
GET /api/dashboard/retention
GET /api/dashboard/financial?startDate=2026-06-01&endDate=2026-06-30
GET /api/dashboard/operations?startDate=2026-06-01&endDate=2026-06-30
```

Auditoria:

```http
GET /api/audit-logs
GET /api/audit-logs/actor/{actorUserId}
GET /api/audit-logs/resource/{resourceType}/{resourceId}
```

---

## Jobs internos

Os jobs rodam internamente pelo Spring Scheduler.

Classe:

```text
src/main/java/com/gymiq/scheduler/GymIqJobScheduler.java
```

Rotinas:

- Atualizar pagamentos pendentes vencidos para `OVERDUE`.
- Gerar mensalidades automaticas.
- Gerar alertas de retencao para alunos ativos.

Os antigos endpoints externos de job foram removidos. Portanto, os jobs nao dependem mais de GitHub Actions, Render Cron ou chamadas externas para executar.

---

## Deploy

Em deploy manual por `.jar`, recomenda-se iniciar a aplicacao com timezone do Brasil:

```bash
java -Duser.timezone=America/Sao_Paulo -jar gymiq-backend-0.0.1-SNAPSHOT.jar
```

Em ambiente AWS/EC2, mantenha as variaveis sensiveis fora do repositorio, por exemplo em arquivo `.env` no servidor ou configuracao segura equivalente.

---

## Testes

Para compilar sem executar testes:

```bash
mvn -DskipTests compile
```

Para executar testes:

```bash
mvn test
```


