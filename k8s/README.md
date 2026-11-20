# k8s manifests

Kubernetes (k3s) manifests for the Kamci debt-tracking backend. They document how the system was deployed to a
real single-node cluster. Environment-specific values (registry, hostnames, secrets) are placeholders.

## Topology

Namespace `kamci`, Traefik (k3s default) as ingress.

| Layer | Workloads |
|---|---|
| Ingress | Traefik `IngressRoute` (HTTPS, Let's Encrypt via `infra/traefik-config.yaml`) -> `gateway-server`; Grafana via plain `Ingress` |
| Platform (`infra/`) | `config-server`, `eureka-server`, `gateway-server`, `redis` |
| Backend (`be/`) | `auth-service`, `debt-service`, `notification-service`, `cache-service`, `mongodb`, `rabbitmq` |
| Observability (`infra/monitoring.yaml`, `observability/`) | Prometheus (+ rules), Alertmanager, Tempo, Loki, Promtail, Grafana with provisioned dashboards |
| Storage | PVCs (RWO, `Recreate` strategy): mongo, redis, prometheus, tempo, loki, grafana |
| Scheduling | `infra/priorityclass.yaml`: high priority for data stores/core, low for observability |

Central Spring config lives in `config/` in this monorepo, served by `config-server`.

## Placeholders

Manifests use `envsubst` variables: `${REGISTRY}` (e.g. `registry.example.com/myorg`), `${IMAGE_TAG}`,
`${API_HOST}`, `${GRAFANA_HOST}`. `apply-all.sh` and the CI workflow substitute only these four.
Own images are pinned to a version tag (`IMAGE_TAG`, default `v0.1.0`), never `:latest`.

## Secrets

Real secrets are not in the repo. Templates: `secrets/*.example.yaml` (`app-secrets`, `firebase-secret`,
`registry-credentials`). Seal them against your cluster's sealed-secrets controller:

```sh
cp secrets/app-secrets.example.yaml secrets/app-secrets.yaml   # edit: replace every CHANGE_ME
kubeseal --format yaml < secrets/app-secrets.yaml > sealed-secret.yaml
# same for firebase-secret -> sealed-firebase-secret.yaml, registry-credentials -> sealed-registry-credentials.yaml
```

`sealed-*.yaml` and `secrets/*.yaml` (except `*.example.yaml`) are git-ignored. Without sealed-secrets,
`kubectl apply -f secrets/<name>.yaml` the plain Secrets directly. `JWT_SECRET_KEY` must match the value in `config/`.

## Apply order

`apply-all.sh`: namespace -> secrets -> priority classes -> prometheus-config -> config-server (waits for rollout) -> rest of infra (waits for eureka) -> be -> grafana configmaps/promtail.
Services start with `fail-fast` config import, so without a Ready config-server their pods CrashLoop.

```sh
export REGISTRY=registry.example.com/myorg API_HOST=api.example.com GRAFANA_HOST=grafana.example.com IMAGE_TAG=v0.1.0
./apply-all.sh
```

## CI

[`.github/workflows/k8s.yml`](../.github/workflows/k8s.yml) validates the manifests on every push and pull request
that touches `k8s/`: it renders the four placeholders with dummy values and runs `kubeconform -strict`. It does not
deploy anything; deploying is the manual `apply-all.sh` flow above.

## Try it on your own cluster

1. k3s (or any cluster with Traefik) and the sealed-secrets controller (optional).
2. Build/push the service images from `backend/` and `infra/` to your registry, tag them `IMAGE_TAG`.
3. Create secrets (above), point DNS for `API_HOST`/`GRAFANA_HOST` at the node, export the variables, run `./apply-all.sh`.
4. `kubectl -n kamci get pods` and `kubectl -n kamci rollout status deploy/gateway-server`.

## Deploy checklist

- [ ] The backend moved to Spring Boot 4.0.x (Jackson 3). Only deploy images that include the `JacksonConfig`
      fix; use a **new** tag (>= `v0.1.0`), do not overwrite an older one.
- [ ] `config-server` Ready before any `be/*` rollout (services are fail-fast; `apply-all.sh` enforces it, manual applies must too).
- [ ] All pods Ready, `config-server` and `eureka-server` first.
- [ ] `GET /rates` through the gateway returns data: cache-service takes its Redis/exchange-rate settings only from the central `cache-service.yml`.
- [ ] Smoke test through the gateway: register -> login -> create a debt.
- [ ] Gateway sees real client IPs (Traefik `externalTrafficPolicy: Local`), rate limit works.
- [ ] Grafana dashboards load; Prometheus targets are up.
