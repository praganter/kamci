#!/bin/bash
set -e

echo "==> Namespace"
kubectl apply -f namespace.yaml

echo "==> Secrets"
kubectl apply \
  -f sealed-secret.yaml \
  -f sealed-firebase-secret.yaml \
  -f sealed-registry-credentials.yaml

echo "==> PriorityClasses (must exist before any pod references them)"
kubectl apply -f infra/priorityclass.yaml

echo "==> Infra (ConfigMap once, then rest)"
kubectl apply -f infra/prometheus-config.yaml
kubectl apply -f infra/

echo "==> Backend"
kubectl apply -f be/

echo "==> Observability"
kubectl apply \
  -f observability/grafana/configmaps/ \
  -f observability/grafana/promtail-config-patch.yaml

echo "==> All done!"
