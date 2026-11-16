FROM grafana/loki:2.9.10
COPY loki-config.yml /etc/loki/local-config.yaml
USER root
RUN mkdir -p /var/loki/chunks /var/loki/rules && chown -R 10001:10001 /var/loki
USER 10001