#!/usr/bin/env python3
"""BASIC-45 phase 7 requirement accounting guard.

This repository-local check verifies the durable OpenAPI fixtures and the
handoff accounting document. It intentionally avoids runner-owned input
fixtures so generated tests remain reusable after cleanup.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
OPENAPI_FILES = [
    ROOT / "backend/src/main/resources/contracts/openapi.yaml",
    ROOT / "backend/src/test/resources/contracts/openapi.yaml",
]
ACCOUNTING = ROOT / "docs/basic45-regression-handoff.md"

EXPECTED_OPERATIONS = {
    "listEvaluationMaterialGenerationTargets": {"REQ-1461"},
    "createEvaluationMaterialGeneration": {"REQ-1462", "REQ-1463", "REQ-1479", "REQ-1480"},
    "listEvaluationMaterialDeletionTargets": {"REQ-1481", "REQ-1482"},
    "createEvaluationMaterialDeletion": {"REQ-1483", "REQ-1490", "REQ-1497", "REQ-1498", "REQ-1499"},
    "listScoreRecalculationTargets": {"REQ-1500", "REQ-1502"},
    "createScoreRecalculation": {"REQ-1501", "REQ-1508", "REQ-1515", "REQ-1516", "REQ-1517"},
    "listFinalEvaluationConfirmations": {"REQ-1518"},
    "createFinalEvaluationConfirmation": {"REQ-1519", "REQ-1531", "REQ-1536", "REQ-1538"},
    "updateFinalEvaluationConfirmationCancel": {"REQ-1520", "REQ-1528", "REQ-1532", "REQ-1537"},
    "listEvaluationBatchResults": {"REQ-1539", "REQ-1540", "REQ-1541", "REQ-1547", "REQ-1548", "REQ-1553"},
    "listEvaluationBatchResultErrors": {"REQ-1540", "REQ-1546", "REQ-1547", "REQ-1549", "REQ-1554"},
}
PHASE7_REQUIREMENTS = {"REQ-1459", "REQ-1391", "REQ-1388"}


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    for path in OPENAPI_FILES:
        if not path.exists():
            fail(f"OpenAPI fixture missing: {path}")
        text = path.read_text(encoding="utf-8")
        for operation_id, reqs in EXPECTED_OPERATIONS.items():
            if f"operationId: {operation_id}" not in text:
                fail(f"{path} missing operationId {operation_id}")
            op_index = text.index(f"operationId: {operation_id}")
            next_op = text.find("operationId:", op_index + 1)
            chunk = text[op_index: next_op if next_op != -1 else len(text)]
            missing = sorted(req for req in reqs if req not in chunk)
            if missing:
                fail(f"{path} operation {operation_id} missing requirements {missing}")

    if not ACCOUNTING.exists():
        fail(f"handoff accounting document missing: {ACCOUNTING}")
    doc = ACCOUNTING.read_text(encoding="utf-8")
    if re.search(r"\b(TODO|MISSING|UNCOVERED)\b", doc):
        fail("handoff accounting document contains unresolved marker")
    for req in PHASE7_REQUIREMENTS:
        if req not in doc:
            fail(f"handoff accounting missing {req}")
    for task in ("T060", "T061", "T062"):
        if task not in doc:
            fail(f"handoff accounting missing {task}")
    print("BASIC-45 requirement accounting OK")


if __name__ == "__main__":
    main()
