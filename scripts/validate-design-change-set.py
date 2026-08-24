#!/usr/bin/env python3
"""Validate approved design change inputs for a code-generation phase.

The script checks that a staged design-doc-change-set.json declares the six
canonical design documents, that the effective documents are present, and that
changed documents have a materialized patch or an approved effective-document
entry. Pass the staged spec/input directory as the first argument.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_DOCUMENTS = {
    "data-model.md",
    "contracts/openapi.yaml",
    "research.md",
    "quickstart.md",
    "architecture.md",
    "ui-design.md",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        payload = json.load(handle)
    if not isinstance(payload, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return payload


def local_patch_candidates(spec_dir: Path, document_path: str, declared_patch_ref: str | None) -> list[Path]:
    candidates: list[Path] = []
    if declared_patch_ref:
        candidates.append(spec_dir / declared_patch_ref)
        candidates.append(Path(declared_patch_ref))
    candidates.append(spec_dir / "design-doc-patches" / f"{document_path}.diff")
    candidates.append(spec_dir / "design-doc-patches" / document_path.replace("/", "-").replace(".yaml", ".diff"))
    candidates.append(spec_dir / "design-doc-patches" / f"{document_path.replace('/', '-')}.diff")
    return candidates


def validate(spec_dir: Path) -> list[str]:
    errors: list[str] = []
    change_set_path = spec_dir / "design-doc-change-set.json"
    approved_path = spec_dir / "approved-change-set.json"
    effective_dir = spec_dir / "effective-design-docs"
    baseline_dir = spec_dir / "canonical-design-docs"

    if not change_set_path.is_file():
        return [f"missing {change_set_path}"]

    change_set = read_json(change_set_path)
    documents = change_set.get("documents")
    if not isinstance(documents, list):
        return ["design-doc-change-set.json must contain a documents array"]

    seen = {document["path"] for document in documents if isinstance(document, dict) and isinstance(document.get("path"), str)}
    missing = REQUIRED_DOCUMENTS - seen
    extra = seen - REQUIRED_DOCUMENTS
    if missing:
        errors.append(f"missing canonical document entries: {', '.join(sorted(missing))}")
    if extra:
        errors.append(f"unexpected canonical document entries: {', '.join(sorted(extra))}")

    approved_by_path: dict[str, dict[str, Any]] = {}
    if approved_path.is_file():
        approved = read_json(approved_path)
        for item in approved.get("documents", []):
            if isinstance(item, dict) and isinstance(item.get("path"), str):
                approved_by_path[item["path"]] = item

    for document in documents:
        if not isinstance(document, dict):
            errors.append("documents array contains a non-object entry")
            continue
        document_path = document.get("path")
        status = document.get("status")
        if not isinstance(document_path, str):
            errors.append("document entry is missing path")
            continue
        if document_path not in REQUIRED_DOCUMENTS:
            continue

        effective_path = effective_dir / document_path
        baseline_path = baseline_dir / document_path
        if not effective_path.is_file():
            errors.append(f"{document_path}: missing effective document {effective_path}")
            continue
        if not baseline_path.is_file():
            errors.append(f"{document_path}: missing canonical baseline {baseline_path}")
            continue
        if sha256(effective_path) == sha256(baseline_path):
            errors.append(f"{document_path}: effective document is identical to canonical baseline")

        approved_doc = approved_by_path.get(document_path, {})
        expected_sha = approved_doc.get("effective_sha256")
        if isinstance(expected_sha, str) and sha256(effective_path) != expected_sha:
            errors.append(f"{document_path}: effective_sha256 does not match approved-change-set.json")

        if status != "changed":
            errors.append(f"{document_path}: expected status=changed, found {status!r}")

        declared_patch_ref = document.get("patch_ref") if isinstance(document.get("patch_ref"), str) else None
        patch_exists = any(candidate.is_file() for candidate in local_patch_candidates(spec_dir, document_path, declared_patch_ref))
        approved_has_effective = document_path in approved_by_path and isinstance(approved_by_path[document_path].get("effective_sha256"), str)
        if not patch_exists and not approved_has_effective:
            errors.append(f"{document_path}: no local patch file or approved effective hash found")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate staged design change-set materialization")
    parser.add_argument("spec_dir", type=Path, help="Directory containing design-doc-change-set.json")
    args = parser.parse_args()

    errors = validate(args.spec_dir.resolve())
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("Design change-set validation passed: six canonical documents are staged and effective.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
