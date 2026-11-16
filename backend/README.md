# Kamci backend

Spring Boot microservices behind the Kamci debt-tracking mobile app. Java 17, Spring Boot 4.0, Spring Cloud 2025.1, MongoDB, RabbitMQ, Redis.

## Services

| Module | Port | Role |
|---|---|---|
| `auth-service` | 8082 | Registration, login, JWT access/refresh tokens, password reset, households, account deletion, RevenueCat subscription webhook. Redis backs rate limiting and the token blacklist. |
| `debt-service` | 8081 | Debts, categories, payments, income, reminder and weekly-digest schedulers (ShedLock). |
| `notification-service` | 8083 | RabbitMQ consumers that send FCM push and e-mail; device token registry. |
| `cache-service` | 8084 | Hourly exchange-rate fetch, cached in Redis. |
| `common-module` | - | Shared event classes, RabbitMQ topology, exception handling, request logging. |

Public routing, JWT validation and rate limiting live in a separate API gateway (not in this module). Paths containing `/internal/` are for service-to-service calls only, and the gateway must reject them.

## Event flow

Producers publish to the `notification.exchange` direct exchange; each event has its own durable queue with a dead-letter queue behind `notification.dlx`.

```
auth-service  --UserRegistered, PasswordReset, HouseholdInvite-->  notification.exchange
debt-service  --DebtAdded, DebtReminder, CategoryMemberAdded, WeeklyDigest-->  notification.exchange
notification.exchange --> <event>.queue --> notification-service consumer --> FCM push and/or e-mail
                                  \--(3 attempts, exponential backoff)--> <event>.dlq --> one retry, then discarded and logged
```

Service-to-service HTTP (OpenFeign): `debt-service` calls `auth/internal/...` (user lookup, household members) and `cache-service`; `auth-service` calls the `internal/account-data` endpoints of the other services on account deletion. Every Feign client has a fallback factory.

## Run it locally

Needs Docker only. No config server, Eureka or Firebase key.

```bash
docker compose -f docker-compose.local.yml up --build
```

Services start with the `local` profile: push notifications are logged and skipped, e-mail goes to [Mailpit](http://localhost:8025) (SMTP `localhost:1025`), and the RevenueCat webhook and admin endpoints use throw-away secrets defined in `auth-service/src/main/resources/application-local.yml`. Swagger UI is at `http://localhost:<port>/swagger-ui.html` on each service. Never use the `local` profile in production.

### Running `backend/docker-compose.yml` on its own

This file starts the business services plus their own MongoDB, RabbitMQ and Redis, and expects the config server and Eureka from the root stack.

1. Start the root stack first: `docker compose up -d --wait` in the repository root. It creates the `kamci-network` network that this file joins as an `external` network, so without it `docker compose` fails with a missing-network error.
2. Create `backend/.env` (the services load it through `env_file`). There is no `backend/.env.example`; copy the root one: `cp ../.env.example .env`.

For a quick trial, `docker-compose.local.yml` above needs neither.

## Tests

```bash
mvn verify          # unit + MockMvc tests for all modules, JaCoCo report per module
```

Tests are mock-based (Mockito, MockMvc standalone) and need no Docker. Coverage reports: `<module>/target/site/jacoco/index.html`.

## Configuration

Outside the `local` profile each service reads its settings from a Spring Cloud Config server (`CONFIG_SERVER_URL`) and registers with Eureka (`EUREKA_URI`). Secrets (`JWT_SECRET`, `MONGO_DB_URI`, RabbitMQ and SMTP credentials, `ADMIN_SECRET_KEY`, `REVENUECAT_WEBHOOK_SECRET`, `GOOGLE_APPLICATION_CREDENTIALS`) come from the environment.
