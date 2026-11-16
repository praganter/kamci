# kamci infrastructure

Platform services for the kamci microservices: API gateway, service discovery, central config and an
observability stack. Spring Boot 4.0.9 / Spring Cloud 2025.1.4, Java 17.

**Start order:** config-server → eureka-server → gateway-server → business services
(services start with `configserver:` + `fail-fast`, so they will not come up until config-server is healthy;
compose `depends_on: service_healthy` enforces this).

## gateway-server (Spring Cloud Gateway, WebFlux)

All external traffic enters here. Filter chain, in order:

1. `ForwardedHeaderStrippingFilter` drops client-supplied `X-Forwarded-*`, `Forwarded`, `X-Real-IP`, unless the TCP peer
   is in `gateway.trusted-proxies` (CIDR list, env `GATEWAY_TRUSTEDPROXIES`, empty by default).
2. `RateLimitFilter` token bucket (Bucket4j) per client IP and rule path. Rules come from `rate-limit.rules` in the
   central config. Over the limit: `429` with `error.ratelimit.exceeded`. With a trusted proxy the key is the rightmost
   `X-Forwarded-For` hop, otherwise the TCP peer.
3. `AuthenticationFilter`
   - Any path containing an `internal` segment (decoded, case-insensitive) returns `404 error.route.not_found`.
     Inter-service endpoints are therefore never reachable from outside.
   - `user-id` / `user-premium` request headers are removed on every route. On protected routes they are set from the
     verified token (`sub`, `premium` claim), so clients cannot spoof identity.
   - Routes not listed in `endpoints.publicEndpoints` need `Authorization: Bearer <access token>` (HS256,
     `jwt.secret`, claim `type=ACCESS`). Errors: `error.auth.header_missing | header_invalid | token_expired | token_invalid`.

Errors use one JSON envelope `{timestamp,status,code,message,path,traceId,errors}` shared with the backend services.
Swagger/api-docs, Eureka and service-id (discovery locator) paths are neither public nor routed.
Routes, public endpoints and rate limits live in the central config (`gateway-server.yml`); Cloud Gateway 5 expects
routes under `spring.cloud.gateway.server.webflux.*`.

## eureka-server

Netflix Eureka registry (port 8761). Services register with `prefer-ip-address`; the gateway resolves `lb://SERVICE`
targets through it.

## config-server

Spring Cloud Config Server (port 8888).

| Mode | How | Source |
|---|---|---|
| native | `SPRING_PROFILES_ACTIVE=native` | folder `CONFIG_NATIVE_LOCATIONS` (default `file:/config`; compose mounts `./config` or `$CONFIG_DIR`) |
| git (default, no flag) | nothing | repo `CONFIG_GIT_URI` |

`/actuator/health` is the container healthcheck.

## Observability

| Component | Port | Role |
|---|---|---|
| Prometheus | 9090 | scrapes `/actuator/prometheus` of every service; rules in `prometheus/rules/` (recording: request/error rates; alerting: restarts, OOM, disk, 5xx rate) |
| Alertmanager | 9093 | routes alerts. Set `ALERT_WEBHOOK_URL` to deliver to a webhook; unset keeps alerts in its UI. An e-mail receiver example is in `alertmanager/alertmanager.yml.tmpl` |
| Loki + Promtail | 3100 | container logs |
| Tempo | 3200 / 9411 | traces (Zipkin protocol) |
| Grafana | 3001 | datasources for Prometheus, Tempo and Loki, plus the `kamci-endpoints` dashboard, provisioned |

Validate rules and config:

```
docker run --rm --entrypoint promtool -v "$PWD/prometheus:/p:ro" prom/prometheus:v3.10.0 check rules /p/rules/alerting-rules.yml /p/rules/recording-rules.yml
docker run --rm --entrypoint promtool -v "$PWD/prometheus:/p:ro" prom/prometheus:v3.10.0 check config /p/prometheus.yml
```

## Run

```
cp .env.example .env        # set JWT_SECRET_KEY (same value as the business services)
docker compose up -d --build --wait                          # core: config-server, eureka, gateway
docker compose --profile observability up -d --build --wait  # core + Prometheus, Alertmanager, Loki, Promtail, Tempo, Grafana
```

Env: `JWT_SECRET_KEY` (required), `GF_SECURITY_ADMIN_PASSWORD`, `JAVA_OPTS`, `CONFIG_DIR`, `CONFIG_SERVER_PROFILE`,
`CONFIG_GIT_URI`, `ALERT_WEBHOOK_URL`.

## Tests

```
mvn verify        # tests + JaCoCo report in <module>/target/site/jacoco
```

- gateway: full filter chain with the real gateway config (valid / invalid / expired / refresh token, `*/internal/**`
  incl. encoded variants, identity header stripping, rate limit 429, public endpoint list, discovery locator off).
  `src/test/resources/dt-config/gateway-server.yml` is a copy of the central config and must be kept in sync.
- eureka-server: context load and health.
- config-server: native profile serves properties from a folder, health is UP.
