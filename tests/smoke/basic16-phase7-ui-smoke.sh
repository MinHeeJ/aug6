#!/usr/bin/env bash
set -euo pipefail

FRONTEND_DIST="${FRONTEND_DIST:-frontend/dist}"
PAGE_BUDGET_BYTES="${PAGE_BUDGET_BYTES:-3145728}"
REQUIRED_ROUTES=(
  "/admin/file-policies"
  "/admin/attachments"
  "/admin/attachments/delete"
  "/admin/attachment-integrity"
)
REQUIRED_SCREEN_IDS=(
  "SCR-FILE-POLICY-MGMT"
  "SCR-ATTACHMENT-METADATA"
  "SCR-ATTACHMENT-DELETE"
  "SCR-ATTACHMENT-INTEGRITY"
)

for path in \
  frontend/src/pages/admin/SCR-FILE-POLICY-MGMT.tsx \
  frontend/src/pages/admin/SCR-ATTACHMENT-METADATA.tsx \
  frontend/src/pages/admin/SCR-ATTACHMENT-DELETE.tsx \
  frontend/src/pages/admin/SCR-ATTACHMENT-INTEGRITY.tsx; do
  grep -q "data-screen-id" "${path}"
  grep -Eq "data-testid=|data-testid\"" "${path}"
  grep -Eq "role=\"(status|alert|dialog)\"|aria-live|aria-label|htmlFor" "${path}"
done

for route in "${REQUIRED_ROUTES[@]}"; do
  grep -q "${route}" frontend/src/app/router.tsx frontend/src/pages/LoginPage.tsx frontend/src/pages/admin/*.tsx
done

for screen_id in "${REQUIRED_SCREEN_IDS[@]}"; do
  grep -q "${screen_id}" frontend/src/pages/admin/*.tsx
done

if [[ -d "${FRONTEND_DIST}" ]]; then
  total_bytes="$(find "${FRONTEND_DIST}" -type f \( -name '*.js' -o -name '*.css' -o -name '*.html' \) -printf '%s\n' | awk '{sum += $1} END {print sum + 0}')"
  if (( total_bytes > PAGE_BUDGET_BYTES )); then
    echo "frontend page asset budget exceeded: ${total_bytes} > ${PAGE_BUDGET_BYTES}" >&2
    exit 1
  fi
  echo "frontend page asset budget ok: ${total_bytes}/${PAGE_BUDGET_BYTES} bytes"
else
  echo "${FRONTEND_DIST} not found; run npm run build in frontend before page-size smoke"
fi

echo "BASIC-16 phase7 UI/accessibility/static smoke passed"
