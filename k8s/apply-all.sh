#!/bin/bash
set -e

# Placeholders substituted in manifests (see README). Required: REGISTRY, API_HOST, GRAFANA_HOST.
: "${REGISTRY:?e.g. registry.example.com:3000/myorg}"
: "${API_HOST:?e.g. api.example.com}"
: "${GRAFANA_HOST:?e.g. grafana.example.com}"
export IMAGE_TAG="${IMAGE_TAG:-v0.1.0}"
VARS='${REGISTRY} ${IMAGE_TAG} ${API_HOST} ${GRAFANA_HOST}'
apply() { for f in "$@"; do envsubst "$VARS" < "$f" | kubectl apply -f -; done; }

echo "==> Namespace"
apply namespace.yaml

echo "==> Secrets"
# sealed-*.yaml are generated with kubeseal and git-ignored (templates: secrets/*.example.yaml)
apply sealed-secret.yaml sealed-firebase-secret.yaml sealed-registry-credentials.yaml
# Optional: Google Play service account for Android purchase verification (auth-service starts without it).
if [ -f sealed-google-play-secret.yaml ]; then apply sealed-google-play-secret.yaml; else echo "    (no sealed-google-play-secret.yaml, Android purchase verification stays off)"; fi

echo "==> PriorityClasses (must exist before any pod references them)"
apply infra/priorityclass.yaml

echo "==> Infra (ConfigMap once, then rest)"
apply infra/prometheus-config.yaml
# Services use fail-fast config import: config-server must be Ready before anything else starts.
apply infra/config-server.yaml
kubectl -n kamci rollout status deploy/config-server --timeout=300s
apply infra/*.yaml
kubectl -n kamci rollout status deploy/eureka-server --timeout=300s

echo "==> Backend"
apply be/*.yaml

echo "==> Observability"
apply observability/grafana/configmaps/*.yaml observability/grafana/promtail-config-patch.yaml

echo "==> All done!"
