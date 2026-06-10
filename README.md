# GymIQ API

Backend do GymIQ, sistema web para gestao de academias com controle de alunos,
matriculas, pagamentos, presencas, fichas de treino, dashboards gerenciais,
auditoria e modulo de alertas de retencao.

O projeto foi desenvolvido como parte de um Projeto Final de Curso, seguindo uma
arquitetura monolitica modular, com separacao por camadas e dominios de negocio.

## Stack

- Java 17
- Spring Boot 3.2.4
- Spring Web
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway
- Spring Security + JWT
- Spring Validation
- Spring Mail
- Spring AOP
- JaCoCo
- Docker
- GitHub Actions
- AWS EC2

## Principais recursos

- Autenticacao com JWT.
- Autorizacao por perfis: `ADMIN`, `RECEPTION`, `INSTRUCTOR` e `STUDENT`.
- Cadastro e gestao de alunos, instrutores, usuarios administrativos, planos e exercicios.
- Matriculas com controle de status: `ACTIVE`, `SUSPENDED` e `CANCELED`.
- Geracao automatica de pagamentos na criacao/renovacao de matriculas.
- Pagamentos com status: `PENDING`, `PAID`, `OVERDUE` e `CANCELED`.
- Controle de presenca por check-in, sem check-out.
- Limite de ate 4 check-ins diarios por aluno.
- Fichas de treino organizadas por blocos, como Treino A, Treino B e Treino C.
- Dashboards financeiro, operacional e de retencao.
- Alertas de retencao com score de risco baseado em inatividade, frequencia e pagamentos vencidos.
- Auditoria por AOP para rastrear operacoes sensiveis.
- Recuperacao de senha por e-mail via SMTP.
- Jobs internos agendados pelo Spring Scheduler.
- Paginacao, ordenacao e filtros nas principais listagens.
- Versionamento do banco com Flyway.
- Testes unitarios e verificacao de cobertura com JaCoCo.

## Arquitetura

O backend segue o padrao monolitico modular. Os modulos ficam no mesmo deploy,
mas sao organizados por responsabilidades:

```text
controller  -> entrada HTTP e contratos REST
service     -> regras de negocio e transacoes
repository  -> persistencia com Spring Data JPA
entity      -> modelo de dados JPA
dto         -> objetos de request/response
security    -> JWT, filtros e configuracao de seguranca
exception   -> tratamento global de erros
scheduler   -> rotinas automaticas internas
aop         -> auditoria de operacoes
```

Essa estrutura facilita manutencao e permite evolucao futura para servicos
independentes caso o volume de usuarios e dados aumente.

## Seguranca e LGPD

O projeto aplica controles voltados a seguranca e protecao de dados pessoais:

- JWT com expiracao configuravel por variavel de ambiente.
- Controle de acesso por role com Spring Security.
- Hash de senhas com BCrypt.
- Criptografia de dados pessoais sensiveis com AES/GCM.
- HMAC-SHA256 para busca deterministica por e-mail e CPF.
- CORS restrito aos dominios autorizados.
- Aceite LGPD no cadastro.
- Anonimizacao de dados pessoais de alunos.
- Preservacao de historico financeiro e operacional apos anonimizacao.
- Logs de auditoria para operacoes sensiveis.

Campos sensiveis como e-mail, CPF, data de nascimento, telefone e endereco sao
protegidos antes da persistencia.

## Identificadores

As entidades principais do dominio utilizam UUID como identificador publico,
reduzindo risco de enumeracao sequencial de registros:

- `User`
- `Student`
- `Instructor`
- `Enrollment`
- `Payment`
- `Presence`
- `WorkoutSheet`
- `WorkoutBlock`
- `WorkoutSheetExercise`
- `RetentionAlert`
- `PasswordResetToken`

Entidades de catalogo ou controle tecnico, como `Plan`, `Exercise` e
`AuditLog`, mantem identificadores sequenciais.

## Requisitos

Para rodar localmente:

- Java 17
- Maven
- PostgreSQL
- Variaveis de ambiente configuradas

Para rodar com Docker:

- Docker Desktop
- Docker Compose

## Variaveis de ambiente

O projeto usa variaveis de ambiente para evitar credenciais fixas no codigo.

Exemplo:

```env
DB_URL=jdbc:postgresql://localhost:5432/gymiq
DB_USERNAME=postgres
DB_PASSWORD=postgres

SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0
SPRING_FLYWAY_VALIDATE_ON_MIGRATE=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

JWT_SECRET=troque-por-uma-chave-segura-com-pelo-menos-32-caracteres
JWT_EXPIRATION_MS=3600000

PII_ENCRYPTION_KEY=troque-por-uma-chave-com-pelo-menos-32-caracteres
PII_HASH_SECRET=troque-por-uma-chave-com-pelo-menos-32-caracteres

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=suporte@gymiq.com
MAIL_PASSWORD=sua_senha_de_app
MAIL_FROM=suporte@gymiq.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

PASSWORD_RESET_FRONTEND_URL=https://gym-iq-web.vercel.app/reset-password
PASSWORD_RESET_EXPIRATION_MINUTES=30
PASSWORD_RESET_RESEND_COOLDOWN_MINUTES=15

CPF_VALIDATION_MODE=LOCAL

PAYMENTS_REFRESH_OVERDUE_CRON="0 30 * * * *"
PAYMENTS_GENERATE_MONTHLY_CRON="0 40 * * * *"
RETENTION_GENERATE_ACTIVE_STUDENTS_CRON="0 55 * * * *"
```

Observacoes:

- `JWT_EXPIRATION_MS=3600000` equivale a 1 hora.
- `CPF_VALIDATION_MODE` aceita `LOCAL`, `EXTERNAL` ou `DISABLED`.
- Em `.env` Linux, valores de cron precisam ficar entre aspas por conterem espacos.
- Os crons usam o formato do Spring com 6 campos:
  `segundo minuto hora dia mes dia-da-semana`.
- Os jobs executam com timezone `America/Sao_Paulo`.

## Como rodar com Docker

```bash
docker compose up --build
```

A API ficara disponivel em:

```text
http://localhost:8080
```

Comandos uteis:

```bash
docker compose up --build -d
docker compose down
docker compose down -v
```

## Como rodar localmente

Configure as variaveis de ambiente e execute:

```bash
mvn spring-boot:run
```

Ou gere o `.jar`:

```bash
mvn clean package
java -Duser.timezone=America/Sao_Paulo -jar target/gymiq-backend-0.0.1-SNAPSHOT.jar
```

## Banco de dados

O projeto usa PostgreSQL e Flyway.

As migrations ficam em:

```text
src/main/resources/db/migration
```

Migrations atuais:

```text
V1__initial_schema.sql
V2__remove_presence_check_out.sql
V3__limit_plan_fields.sql
V4__limit_workout_sheet_exercise_fields.sql
V5__require_instructor_specialty.sql
V6__create_workout_blocks.sql
V7__prevent_multiple_open_enrollments.sql
V8__add_lgpd_policy_metadata.sql
```

Configuracao recomendada:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

## Usuario administrador inicial

O `DataInitializer` cria o usuario administrador inicial caso ele ainda nao
exista.

| Campo | Valor |
| --- | --- |
| Email | `admin@gymiq.com` |
| Senha | `gymiq@2026` |

Tambem sao criados exercicios base quando a tabela de exercicios ainda esta
vazia.

## Autenticacao

Login:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@gymiq.com",
  "password": "gymiq@2026"
}
```

Use o token retornado nas proximas requisicoes:

```http
Authorization: Bearer <token>
```

Recuperacao de senha:

```http
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

## Endpoints principais

### Auth

```http
POST /api/auth/login
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

### Usuarios administrativos

```http
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
```

### Alunos

```http
GET    /api/students?status=ACTIVE|INACTIVE|ALL
GET    /api/students/search?q=&status=ACTIVE|INACTIVE|ALL
GET    /api/students/options?q=
GET    /api/students/me
GET    /api/students/{id}
POST   /api/students
PUT    /api/students/{id}
PATCH  /api/students/{id}/inactive
PATCH  /api/students/{id}/active
GET    /api/students/{id}/personal-data/deletion-eligibility
GET    /api/students/me/personal-data/deletion-eligibility
DELETE /api/students/{id}/personal-data
DELETE /api/students/me/personal-data
```

### Instrutores

```http
GET    /api/instructors?status=ACTIVE|INACTIVE|ALL
GET    /api/instructors/search?q=&status=ACTIVE|INACTIVE|ALL
GET    /api/instructors/me
GET    /api/instructors/{id}
POST   /api/instructors
PUT    /api/instructors/{id}
PATCH  /api/instructors/{id}/inactive
PATCH  /api/instructors/{id}/activate
DELETE /api/instructors/{id}
```

Para exclusao fisica, o instrutor deve estar inativo e nao pode estar vinculado
a fichas de treino.

### Planos

```http
GET    /api/plans?status=ACTIVE|INACTIVE|ALL&q=
GET    /api/plans/{id}
POST   /api/plans
PUT    /api/plans/{id}
PATCH  /api/plans/{id}/deactivate
PATCH  /api/plans/{id}/activate
DELETE /api/plans/{id}
```

### Exercicios

```http
GET    /api/exercises
GET    /api/exercises/search?q=
GET    /api/exercises/{id}
POST   /api/exercises
PUT    /api/exercises/{id}
DELETE /api/exercises/{id}
```

### Matriculas

```http
GET   /api/enrollments?status=ACTIVE|SUSPENDED|CANCELED
GET   /api/enrollments/{id}
GET   /api/enrollments/student/{studentId}
GET   /api/enrollments/student/{studentId}/active
GET   /api/enrollments/me
GET   /api/enrollments/me/active
POST  /api/enrollments
POST  /api/enrollments/{id}/renew?newPlanId=
PATCH /api/enrollments/{id}/status?newStatus=
```

### Pagamentos

```http
GET   /api/payments?status=PENDING|PAID|OVERDUE|CANCELED
GET   /api/payments/{id}
GET   /api/payments/student/{studentId}?status=PENDING|PAID|OVERDUE|CANCELED
GET   /api/payments/enrollment/{enrollmentId}?status=PENDING|PAID|OVERDUE|CANCELED
GET   /api/payments/me?status=PENDING|PAID|OVERDUE|CANCELED
PATCH /api/payments/{id}/pay
PATCH /api/payments/{id}/status?newStatus=
PATCH /api/payments/refresh-overdue
```

### Presencas

```http
POST /api/presences
POST /api/presences/self-check-in
GET  /api/presences
GET  /api/presences/me
GET  /api/presences/{id}
GET  /api/presences/student/{studentId}
GET  /api/presences/date/{date}
```

### Fichas de treino

```http
GET    /api/workout-sheets
GET    /api/workout-sheets/me?onlyActive=true|false
GET    /api/workout-sheets/instructor/me
GET    /api/workout-sheets/{id}
GET    /api/workout-sheets/student/{studentId}?onlyActive=true|false
GET    /api/workout-sheets/instructor/{instructorId}
POST   /api/workout-sheets
PUT    /api/workout-sheets/{id}
PATCH  /api/workout-sheets/{id}/inactive
PATCH  /api/workout-sheets/{id}/activate
DELETE /api/workout-sheets/{id}
```

Blocos e exercicios da ficha:

```http
POST   /api/workout-sheets/{workoutSheetId}/blocks
GET    /api/workout-sheets/{workoutSheetId}/blocks
GET    /api/workout-blocks/{id}
PUT    /api/workout-blocks/{id}
DELETE /api/workout-blocks/{id}

POST   /api/workout-sheets/{workoutSheetId}/exercises
GET    /api/workout-sheets/{workoutSheetId}/exercises
POST   /api/workout-blocks/{workoutBlockId}/exercises
GET    /api/workout-blocks/{workoutBlockId}/exercises
PUT    /api/workout-sheet-exercises/{id}
DELETE /api/workout-sheet-exercises/{id}
```

### Retencao

```http
GET   /api/dashboard/retention
GET   /api/retention-alerts/open
GET   /api/retention-alerts/student/{studentId}
GET   /api/retention-alerts/{id}
POST  /api/retention-alerts/student/{studentId}/generate
POST  /api/retention-alerts/generate-active-students
GET   /api/retention-alerts/generate-active-students/status
POST  /api/retention-alerts/generate-overdue-students
PATCH /api/retention-alerts/{id}/resolve
```

### Dashboards

```http
GET /api/dashboard/retention
GET /api/dashboard/financial?startDate=2026-06-01&endDate=2026-06-30
GET /api/dashboard/operations?startDate=2026-06-01&endDate=2026-06-30
```

### Auditoria

```http
GET /api/audit-logs?actorUserId=&action=&resourceType=&resourceId=&from=&to=
GET /api/audit-logs/filter-options
```

Filtros por ator e por recurso devem ser feitos pelo endpoint principal
`GET /api/audit-logs`, usando `actorUserId`, `resourceType` e `resourceId`.

### Health check

```http
GET /api/health
GET /health
```

## Filtros e paginacao

As listagens usam paginacao e ordenacao do Spring:

```http
?page=0&size=10&sort=createdAt,desc
```

Filtros principais:

- Alunos: `status`, `q`
- Instrutores: `status`, `q`
- Planos: `status`, `q`
- Matriculas: `status`
- Pagamentos: `status`
- Fichas de treino: `onlyActive`
- Dashboards financeiro/operacional: `startDate`, `endDate`
- Auditoria: `actorUserId`, `action`, `resourceType`, `resourceId`, `from`, `to`

## Jobs internos

Os jobs rodam internamente com Spring Scheduler:

```text
src/main/java/com/gymiq/scheduler/GymIqJobScheduler.java
```

Rotinas:

- `refresh-overdue`: atualiza pagamentos vencidos para `OVERDUE`.
- `generate-monthly`: gera mensalidades recorrentes.
- `generate-retention-alerts`: gera alertas de retencao para alunos ativos.

Os jobs nao dependem de GitHub Actions, Render Cron ou requisicoes externas para
rodar.

## Deploy na AWS

O deploy automatizado esta configurado em:

```text
.github/workflows/deploy.yml
```

Fluxo:

1. Push nas branches `main` ou `dev`.
2. GitHub Actions configura Java 17.
3. Executa `mvn clean package`.
4. Envia o `.jar` para a EC2.
5. Reinicia a aplicacao com timezone `America/Sao_Paulo`.

Comando usado na EC2:

```bash
java -Duser.timezone=America/Sao_Paulo -jar gymiq-backend-0.0.1-SNAPSHOT.jar
```

As credenciais da AWS ficam em GitHub Secrets e as variaveis da aplicacao ficam
no `.env` da instancia.

## Testes

Executar testes:

```bash
mvn test
```

Executar build completo com testes e verificacao JaCoCo:

```bash
mvn clean package
```

Gerar relatorio de cobertura:

```bash
mvn test
```

Relatorio:

```text
target/site/jacoco/index.html
```

O `pom.xml` possui JaCoCo configurado para verificar cobertura minima de linhas
no bundle.

## Boas praticas aplicadas

- Separacao entre controller, service, repository, DTO e entity.
- Validacoes de entrada com Jakarta Validation.
- Tratamento global de excecoes.
- Uso de transacoes na camada de service.
- EntityGraph em consultas de pagamento para evitar N+1.
- Jobs idempotentes para rotinas financeiras e retencao.
- Configuracao por variaveis de ambiente.
- Migrations versionadas com Flyway.
- Auditoria por AOP.
- Dados sensiveis protegidos antes da persistencia.

## Melhorias futuras

- Cookies HttpOnly para armazenamento seguro dos tokens de autenticacao.
- Integracao com gateway de pagamento para Pix, boleto ou link de pagamento.
- Notificacoes automaticas para alunos inadimplentes ou em risco de cancelamento.
- Reconhecimento facial para automatizar check-in.
- Ampliacao dos testes automatizados no frontend.
- Evolucao gradual para microsservicos caso haja aumento significativo de escala.
