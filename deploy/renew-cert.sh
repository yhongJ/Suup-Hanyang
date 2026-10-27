#!/usr/bin/env bash
# 인증서 갱신. cron으로 매일 실행 (만료 30일 전부터 갱신)
set -euo pipefail
cd "$(dirname "$0")"
docker compose -f docker-compose.prod.yml run --rm certbot renew --quiet
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
