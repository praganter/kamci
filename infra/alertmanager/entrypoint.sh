#!/bin/sh
set -e
if [ -n "$ALERT_WEBHOOK_URL" ]; then receiver=webhook; else receiver=null; fi
sed -e "s|__RECEIVER__|$receiver|" -e "s|__WEBHOOK_URL__|${ALERT_WEBHOOK_URL:-http://localhost/unused}|" \
  /etc/alertmanager/alertmanager.yml.tmpl > /tmp/alertmanager.yml
exec /bin/alertmanager --config.file=/tmp/alertmanager.yml --storage.path=/alertmanager "$@"
