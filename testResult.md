# API runtime repeated failures

- source_fingerprint: `4b7f7eab98a6a082ccaeea3faba563e440c84c2458bfe7a0ef2940382295c086`
- contract_fingerprint: `b17f9968c64a6f4fa7c7fe003fb9496aab754092632db2e8c71326a993744334`

## Repeated failures

### GET /api/admin/school-info (get-api-admin-school-info)

- failure_signature: `e819b90b5af100be56d894984adf69232fa152fc0b78243862ca3042d5123140`
- category: `http_status_mismatch`
- reason: expected [200], got 502
- action: same cause reached the second failure; no third retry or repair request

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

### GET /api/business/student-guidance-achievements (get-api-business-student-guidance-achievements)

- failure_signature: `220ece63f5b96258576bfccdae0dec009c0011cdb97c57f4ef5b9f85218980fa`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

## Coverage

- required_operations: 10
- executed_operations: 10
