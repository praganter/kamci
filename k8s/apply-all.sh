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

echo "==> PriorityClasses (must exist before any pod references them)"
apply infra/priorityclass.yaml

echo "==> Infra (ConfigMap once, then rest)"
apply infra/prometheus-config.yaml
apply infra/*.yaml

echo "==> Backend"
apply be/*.yaml

echo "==> Observability"
apply observability/grafana/configmaps/*.yaml observability/grafana/promtail-config-patch.yaml

echo "==> All done!"
