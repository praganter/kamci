FROM grafana/tempo:3.1.1
COPY tempo.yml /etc/tempo.yaml
USER root
RUN mkdir -p /var/tempo/blocks /var/tempo/wal && chown -R 10001:10001 /var/tempo
USER 10001