# kamci config

Central [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/reference/) files for the kamci microservices.
The config server serves them in `native` mode, directly from this folder.

## Files

| File | Applies to |
|---|---|
| `common-config.yml` | Every service: MongoDB, RabbitMQ, Eureka client, tracing, logging, actuator |
| `gateway-server.yml` | API gateway: routes, public endpoints, rate limits |
| `auth-service.yml` | Auth service (port 8082): JWT settings |
| `debt-service.yml` | Debt service (port 8081) |
| `notification-service.yml` | Notification service (port 8083): SMTP, password-reset deep link |
| `cache-service.yml` | Cache service (port 8084): Redis, exchange-rate source |
| `eureka-server.yml` | Service registry (port 8761) |

Names follow `<application>.yml`, so a client with `spring.application.name=auth-service` and
`spring.cloud.config.name=auth-service,common-config` gets both files merged.

## Profiles

Defaults are production-like (log level `INFO`, tracing sampling `0.1`).
Documents guarded by `on-profile: local | dev` raise this to `DEBUG` logs and sampling `1.0`
(gateway also logs routing/load-balancer decisions).
The `local` profile of the services normally runs with the config server disabled
(`SPRING_CLOUD_CONFIG_ENABLED=false`) and uses each service's own `application-local.yml`.

## Environment variables

| Variable | Used by | Default | Notes |
|---|---|---|---|
| `MONGO_DB_URI` | all | `mongodb://dev:dev@mongodb:27017/kamci?...` (dev only) | Always set it outside local development |
| `SPRING_RABBITMQ_HOST` / `_PORT` | all | `localhost` / `5672` | |
| `SPRING_RABBITMQ_USERNAME` / `_PASSWORD` | all | none | required |
| `EUREKA_URI` | all | `http://eureka-server:8761/eureka` | |
| `JWT_SECRET` | auth, gateway | none | required; must be identical in both |
| `MAIL_HOST` / `MAIL_PORT` | notification | `smtp.gmail.com` / `587` | |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | notification | none | required |
| `PASSWORD_RESET_DEEP_LINK` | notification | `kamci://reset-password` | |
| `APP_REDIS_HOST` / `_PORT` / `_PASSWORD` | auth, cache | `redis` / `6379` / empty | |
| `EXCHANGE_BASE_CURRENCIES` | cache | `USD,EUR,TRY,GBP` | |
| `ADMIN_SECRET_KEY` | auth | empty | read directly by the service |
| `INTERNAL_API_TOKEN` | all | public dev default | Shared `X-Internal-Token` for service-to-service calls; always set it outside local development |
| `APPLE_BUNDLE_ID` | auth | empty | Bundle ID the Apple receipts must belong to; read directly by the service (not served by the config server). If empty, Apple purchase verification returns an explicit error |
| `APPLE_SHARED_SECRET` (`apple.store.shared-secret`) | auth | empty | App Store shared secret used to verify auto-renewable subscription receipts; read directly by the service. If empty, Apple purchase verification returns an explicit error |
| `GOOGLE_PLAY_PACKAGE_NAME` | auth | empty | Android package name Play purchases must belong to; read directly by the service. If empty, Google purchase verification returns an explicit error |
| `GOOGLE_PLAY_CREDENTIALS` | auth | empty | Path to the Play service-account key file used to call the Play Developer API; read directly by the service. If empty, Google purchase verification returns an explicit error |
| `GOOGLE_PUBSUB_AUDIENCE` | auth | empty | Expected audience of the Google Pub/Sub push token; read directly by the service. If empty, the Google webhook answers 403 |
| `GOOGLE_PUBSUB_SERVICE_ACCOUNT_EMAIL` | auth | empty | Service account that signs the Pub/Sub push token; read directly by the service. If empty, the Google webhook answers 403 |

## Native mode

The config server needs the `native` profile and this folder as its search location:

```yaml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: file:/config   # mount this folder at /config
```

`docker compose up` in the monorepo root does this for you. Config changes need a service
restart (or `/actuator/refresh`) to take effect.

## Gateway notes

* Public (no JWT) endpoints are listed under `endpoints.publicEndpoints` in `gateway-server.yml`.
* Any path containing an `internal` segment (e.g. `/auth/internal/**`) is rejected by the gateway,
  so inter-service endpoints stay unreachable from outside.
* Service-discovery route auto-creation is disabled; routes are declared explicitly.
