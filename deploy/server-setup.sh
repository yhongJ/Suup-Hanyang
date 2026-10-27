#!/usr/bin/env bash
# 서버 초기 설정. 최초 1회 실행
set -euo pipefail

echo "== 1. 패키지 업데이트"
sudo apt-get update -y
sudo apt-get upgrade -y

echo "== 2. Docker 설치 (공식 설치 스크립트)"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
fi
sudo usermod -aG docker "$USER"

echo "== 3. 스왑 2GB (메모리 2GB 서버에서 빌드 중 메모리 부족 방지)"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "== 4. 서버 시간대를 서울로"
sudo timedatectl set-timezone Asia/Seoul

echo
echo "완료! 'docker' 권한을 적용하려면 한 번 로그아웃했다가 다시 접속하세요 (exit 후 재접속)."
