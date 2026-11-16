FROM grafana/grafana:12.4.1
COPY grafana-datasources.yml /etc/grafana/provisioning/datasources/datasource.yaml
COPY provisioning/dashboards/dashboard-provider.yaml /etc/grafana/provisioning/dashboards/dashboard-provider.yaml
COPY dashboards/ /etc/grafana/provisioning/dashboards/