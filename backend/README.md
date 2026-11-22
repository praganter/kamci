# Kamci backend

Spring Boot microservices behind the Kamci debt-tracking mobile app. Java 17, Spring Boot 4.0, Spring Cloud 2025.1, MongoDB, RabbitMQ, Redis.

## Services

| Module | Port | Role |
|---|---|---|
| `auth-service` | 8082 | Registration, login, JWT access/refresh tokens, password reset, households, account deletion, in-app purchase verification (App Store and Google Play). Redis backs rate limiting and the token blacklist. |
| `debt-service` | 8081 | Debts, categories, payments, income, reminder and weekly-digest schedulers (ShedLock). |
| `notification-service` | 8083 | RabbitMQ consumers that send FCM push and e-mail; device token registry. |
| `cache-service` | 8084 | Hourly exchange-rate fetch, cached in Redis. |
| `common-module` | - | Shared event classes, RabbitMQ topology, exception handling, request logging. |

Public routing, JWT validation and rate limiting live in a separate API gateway (not in this module). Paths containing `/internal/` are for service-to-service calls only: the gateway must reject them, and each service additionally requires the shared `X-Internal-Token` header (`INTERNAL_API_TOKEN`, checked in `common-module`'s `InternalApiConfig`; Feign clients add it automatically). Without the variable a public dev default is used and a warning is logged.

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

Services start with the `local` profile: push notifications are logged and skipped, e-mail goes to [Mailpit](http://localhost:8025) (SMTP `localhost:1025`), and the admin endpoints use a throw-away secret defined in `auth-service/src/main/resources/application-local.yml`. Swagger UI is at `http://localhost:<port>/swagger-ui.html` on each service. Never use the `local` profile in production.

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

Outside the `local` profile each service reads its settings from a Spring Cloud Config server (`CONFIG_SERVER_URL`) and registers with Eureka (`EUREKA_URI`). Secrets (`JWT_SECRET`, `MONGO_DB_URI`, RabbitMQ and SMTP credentials, `ADMIN_SECRET_KEY`, `INTERNAL_API_TOKEN`, `GOOGLE_APPLICATION_CREDENTIALS` for push, and the in-app purchase settings below) come from the environment.

### In-app purchases (auth-service)

`auth-service` verifies purchases itself, with no third-party purchase service:

| Endpoint | Caller | Authenticated by |
|---|---|---|
| `POST /subscription/verify` | the mobile app, after a purchase | the user's JWT (checked at the gateway) |
| `POST /subscription/apple/webhook` | Apple App Store Server Notifications V2 | the JWS signature, chain pinned to Apple Root CA G3 |
| `POST /subscription/google/webhook` | Google Play real-time developer notifications via a Pub/Sub push subscription | the Pub/Sub OIDC token (`Authorization: Bearer`) |

Every setting is optional so the service starts without them. A path whose settings are missing answers with an explicit error instead: `503 error.subscription.apple_not_configured` / `google_not_configured` for `verify` and the Apple webhook, and `403` for the Google webhook (it never accepts an unauthenticated notification).

| Variable | Needed for | Meaning |
|---|---|---|
| `APPLE_BUNDLE_ID` | Apple verify and webhook | bundle id of the iOS app; other apps' receipts and notifications are rejected |
| `APPLE_SHARED_SECRET` (or `apple.store.shared-secret`) | Apple verify | app-specific shared secret for the legacy `verifyReceipt` API |
| `APPLE_ALLOW_SANDBOX` | Apple, optional | `true` accepts sandbox receipts and notifications (App Review, TestFlight). Off by default because sandbox purchases are free to create |
| `IAP_MONTHLY_PRODUCT_IDS`, `IAP_YEARLY_PRODUCT_IDS` | verify (both stores) | comma-separated store product ids; a product must belong to the plan the client claims |
| `GOOGLE_PLAY_PACKAGE_NAME` | Google verify and webhook | the only Android package accepted |
| `GOOGLE_PLAY_CREDENTIALS` | Google verify | path to a service-account key with access to the Play Developer API. Not `GOOGLE_APPLICATION_CREDENTIALS`, which is the Firebase account |
| `GOOGLE_PUBSUB_AUDIENCE`, `GOOGLE_PUBSUB_SERVICE_ACCOUNT_EMAIL` | Google webhook | audience and service account configured on the Pub/Sub push subscription |

`verify` still uses Apple's legacy `verifyReceipt`, which Apple has deprecated; it sits behind the `AppleReceiptVerifier` interface so the App Store Server API / StoreKit 2 signed transactions (already verifiable with `AppleJwsVerifier`) can replace it.
