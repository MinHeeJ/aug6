#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000}"
COOKIE_JAR="${COOKIE_JAR:-/tmp/common-foundation-auth-cookies.txt}"
rm -f "$COOKIE_JAR"

require_status() {
  local expected="$1"
  local actual="$2"
  local label="$3"
  if [ "$actual" != "$expected" ]; then
    echo "FAIL $label: expected HTTP $expected but got $actual" >&2
    exit 1
  fi
  echo "OK $label: HTTP $actual"
}

health_body="$(mktemp)"
health_status="$(curl -sS -o "$health_body" -w '%{http_code}' "$BASE_URL/api/health")"
require_status "200" "$health_status" "health"
grep -q '"success":true' "$health_body"
grep -q '"status":"UP"' "$health_body"

unauth_body="$(mktemp)"
unauth_status="$(curl -sS -o "$unauth_body" -w '%{http_code}' "$BASE_URL/api/auth/me")"
require_status "401" "$unauth_status" "me without session"
grep -q '"code":"UNAUTHENTICATED"' "$unauth_body"

login_body="$(mktemp)"
login_status="$(curl -sS -c "$COOKIE_JAR" -o "$login_body" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"admin","password":"admin"}' \
  "$BASE_URL/api/auth/login")"
require_status "200" "$login_status" "login admin/admin"
grep -q '"loginId":"admin"' "$login_body"
grep -q '"R09"' "$login_body"

auth_body="$(mktemp)"
auth_status="$(curl -sS -b "$COOKIE_JAR" -o "$auth_body" -w '%{http_code}' "$BASE_URL/api/auth/me")"
require_status "200" "$auth_status" "me with session"
grep -q '"loginId":"admin"' "$auth_body"
grep -q '"R09"' "$auth_body"

logout_body="$(mktemp)"
logout_status="$(curl -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" -o "$logout_body" -w '%{http_code}' -X POST "$BASE_URL/api/auth/logout")"
require_status "200" "$logout_status" "logout"
grep -q '"success":true' "$logout_body"

post_logout_body="$(mktemp)"
post_logout_status="$(curl -sS -b "$COOKIE_JAR" -o "$post_logout_body" -w '%{http_code}' "$BASE_URL/api/auth/me")"
require_status "401" "$post_logout_status" "me after logout"
grep -q '"code":"UNAUTHENTICATED"' "$post_logout_body"

echo "auth-health smoke passed"
