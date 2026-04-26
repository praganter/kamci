#!/usr/bin/env bash
# Regenerates grafana-dashboards ConfigMap from source JSON files.
# Run from the k3s-manifest/ directory:
#   bash observability/grafana/configmaps/generate-dashboards-cm.sh | kubectl apply -f -
#
# Or dry-run to inspect:
#   bash observability/grafana/configmaps/generate-dashboards-cm.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DASHBOARD_DIR="${SCRIPT_DIR}/../dashboards"

kubectl create configmap grafana-dashboards \
  --namespace=kamci \
  --from-file=cluster-node-overview.json="${DASHBOARD_DIR}/cluster-node-overview.json" \
  --from-file=service-health-red.json="${DASHBOARD_DIR}/service-health-red.json" \
  --from-file=logs-explorer.json="${DASHBOARD_DIR}/logs-explorer.json" \
  --from-file=request-tracing.json="${DASHBOARD_DIR}/request-tracing.json" \
  --from-file=ingress-traefik.json="${DASHBOARD_DIR}/ingress-traefik.json" \
  --dry-run=client \
  -o yaml
