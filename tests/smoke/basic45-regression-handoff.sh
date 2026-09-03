#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

required_gitignore=(
  "node_modules/"
  ".next/"
  "dist/"
  "build/"
  "coverage/"
  ".vite/"
  ".cache/"
  "*.log"
  "*.local"
)
for pattern in "${required_gitignore[@]}"; do
  grep -Fxq "$pattern" .gitignore || {
    echo "FAIL: .gitignore missing $pattern" >&2
    exit 1
  }
done

for context in backend frontend; do
  for pattern in node_modules .next dist build coverage .git; do
    grep -Fxq "$pattern" "$context/.dockerignore" || {
      echo "FAIL: $context/.dockerignore missing $pattern" >&2
      exit 1
    }
  done
done

python3 tests/smoke/basic45-requirement-accounting.py

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  docker compose -f infra/docker-compose.yml config >/tmp/basic45-compose-config.yml
  if [[ "${BASIC45_RUN_DOCKER_UP:-0}" == "1" ]]; then
    docker compose -f infra/docker-compose.yml up -d --wait
    curl -fsS http://127.0.0.1:8080/api/health >/tmp/basic45-health.json
    grep -Eq '"(status|success)"' /tmp/basic45-health.json || {
      echo "FAIL: /api/health response does not contain status or success" >&2
      cat /tmp/basic45-health.json >&2
      exit 1
    }
  else
    echo "docker compose config OK; set BASIC45_RUN_DOCKER_UP=1 to run docker compose up -d --wait and /api/health smoke"
  fi
else
  echo "docker compose unavailable; config/up/health runtime verification deferred"
fi

echo "BASIC-45 regression handoff smoke OK"
