# API runtime repeated failures

- source_fingerprint: `e0155c2a22951e658e85b01b1d2057877a1c86c33522ac93f0d904fa33751368`
- contract_fingerprint: `99c97198fdb8809394071603511c7a05ddd8ded13c5b2561ac92eb3551e79f14`

## Repeated failures

### GET /api/business/student-guidance-achievements (get-api-business-student-guidance-achievements)

- failure_signature: `220ece63f5b96258576bfccdae0dec009c0011cdb97c57f4ef5b9f85218980fa`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/student-guidance-achievements/excel-template (get-api-business-student-guidance-achievements-excel-template)

- failure_signature: `b4d2a52f66eac4287840125563a21669b92f599bac6e30fb58d9555e7251b98a`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/student-guidance-achievements/excel-upload-histories (get-api-business-student-guidance-achievements-excel-upload-histories)

- failure_signature: `36e9090b1281d12eea621b62671ff6bf65edc0507f96c911b9286f93c1cc3e7e`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/admin/school-info (get-api-admin-school-info)

- failure_signature: `e819b90b5af100be56d894984adf69232fa152fc0b78243862ca3042d5123140`
- category: `http_status_mismatch`
- reason: expected [200], got 502
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-evaluation-achievements (get-api-business-lecture-evaluation-achievements)

- failure_signature: `aac9c11412d023be39742a53f35d26fb6953ef285ca53d9c194b7283e5c74f0c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-achievements (get-api-business-lecture-achievements)

- failure_signature: `09b4c123da0f22012fa8e26f19337b2e531d0491d643fb1010ca3a511828301c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/degree-completion-achievements (get-api-business-degree-completion-achievements)

- failure_signature: `edb2fe3301b9356ac693961134a5b32785766bbb77570d1f3aff84d9b9fb806c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

## Coverage

- required_operations: 7
- executed_operations: 7
