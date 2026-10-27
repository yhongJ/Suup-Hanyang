#!/usr/bin/env bash
# 최신 코드 반영
set -euo pipefail
cd "$(dirname "$0")"
git pull
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f >/dev/null   # 이전 이미지 정리
echo "반영 완료. 로그 확인: docker compose -f docker-compose.prod.yml logs -f app"
