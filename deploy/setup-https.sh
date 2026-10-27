#!/usr/bin/env bash
# 인증서 발급 후 https로 전환. 최초 1회 실행
# 전제: DNS 연결 완료, .env 작성 완료
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a
: "${DOMAIN:?.env 에 DOMAIN 이 없어요}"
: "${CERTBOT_EMAIL:?.env 에 CERTBOT_EMAIL 이 없어요}"

COMPOSE="docker compose -f docker-compose.prod.yml"
mkdir -p nginx/active certbot/www certbot/conf

echo "== 1. http 임시 설정으로 시작"
cp nginx/http.conf.template nginx/active/default.conf.template
$COMPOSE up -d --build

echo "== 2. 인증서 발급 (Let's Encrypt)"
$COMPOSE run --rm certbot certonly --webroot -w /var/www/certbot \
  -d "$DOMAIN" -d "www.$DOMAIN" \
  --email "$CERTBOT_EMAIL" --agree-tos --no-eff-email

echo "== 3. https 설정으로 전환"
cp nginx/https.conf.template nginx/active/default.conf.template
$COMPOSE restart nginx

echo
echo "완료! https://$DOMAIN 에 접속해 보세요."
