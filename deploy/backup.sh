#!/usr/bin/env bash
# DB 백업. 14일치 보관. cron으로 매일 실행
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a
mkdir -p backups
FILE="backups/suuphanyang-$(date +%Y%m%d-%H%M).sql.gz"
docker compose -f docker-compose.prod.yml exec -T mysql \
  mysqldump -uroot -p"$DB_ROOT_PASSWORD" --single-transaction suuphanyang | gzip > "$FILE"
find backups -name "*.sql.gz" -mtime +14 -delete
echo "백업 완료: $FILE"
