# GymIQ — Backend

Sistema de gestão de academia — TCC.
Stack: Java 17 · Spring Boot 3.2 · MySQL 8 · JWT · Docker

---

## Pré-requisito

Apenas o **Docker Desktop** instalado e rodando.
Não precisa de Java, Maven ou MySQL na máquina.

---

## Como rodar

```bash
# 1. Clone o repositório
git clone <URL_DO_REPO>
cd gymiq-backend

# 2. Suba os containers (primeira vez ~3 min)
docker compose up --build
```

Pronto. A API estará em **http://localhost:8080**

Para rodar em background: `docker compose up --build -d`  
Para parar: `docker compose down`  
Para apagar tudo e recomeçar: `docker compose down -v`

---

## Usuário admin criado automaticamente

| Campo | Valor |
|-------|-------|
| Email | `admin@gymiq.com` |
| Senha | `gymiq@2026` |

---

## Testando no Postman

**Login:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@gymiq.com",
  "password": "gymiq@2026"
}
```

Use o token retornado no header das próximas requisições:
```
Authorization: Bearer <token>
```
