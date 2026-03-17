#!/bin/bash
set -e

# dt-network yoksa yarat — tüm mikroservisler bu network'ü paylaşır
if ! docker network ls --format '{{.Name}}' | grep -q '^dt-network$'; then
    echo "dt-network bulunamadı, oluşturuluyor..."
    docker network create dt-network
    echo "dt-network oluşturuldu."
else
    echo "dt-network zaten mevcut."
fi

echo "Servisler başlatılıyor..."
docker-compose up -d "$@"