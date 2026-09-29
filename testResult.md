# API runtime repeated failures

- source_fingerprint: `1bb5d88580d35144119af9c4d875f6688e6b70d4dc8f27fb0bc53b747e1ea73b`
- contract_fingerprint: `fa5dbff963e6bcba3ce0ca26c08cf260fc8d01008b37123a3c7ae2fc8a954ac9`

## Repeated failures

### GET /api/business/degree-completion-achievements (get-api-business-degree-completion-achievements)

- failure_signature: `edb2fe3301b9356ac693961134a5b32785766bbb77570d1f3aff84d9b9fb806c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-achievements (get-api-business-lecture-achievements)

- failure_signature: `09b4c123da0f22012fa8e26f19337b2e531d0491d643fb1010ca3a511828301c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-evaluation-achievements (get-api-business-lecture-evaluation-achievements)

- failure_signature: `aac9c11412d023be39742a53f35d26fb6953ef285ca53d9c194b7283e5c74f0c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

## Coverage

- required_operations: 7
- executed_operations: 7
