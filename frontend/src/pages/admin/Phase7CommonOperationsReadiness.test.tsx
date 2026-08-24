import { describe, expect, it } from "vitest";
import {
  reduceAttachmentIntegrityState,
  createEmptyAttachmentIntegrityState,
} from "./SCR-ATTACHMENT-INTEGRITY";
import { getFilePolicyRouteContract } from "./SCR-FILE-POLICY-MGMT";
import { getAttachmentMetadataRouteContract } from "./SCR-ATTACHMENT-METADATA";
import { getAttachmentDeleteRouteContract } from "./SCR-ATTACHMENT-DELETE";
import { getAttachmentIntegrityRouteContract } from "./SCR-ATTACHMENT-INTEGRITY";
import {
  getPhase7UiReadinessChecklist,
  getPhase7PageBudgetBytes,
} from "./phase7Readiness";

describe("Phase 7 common validation and operations readiness", () => {
  it("announces long-running integrity completion and user-facing result guidance", () => {
    const completed = reduceAttachmentIntegrityState(
      createEmptyAttachmentIntegrityState(),
      {
        type: "completed",
        check: {
          checkId: 3001,
          status: "COMPLETED",
          startedBy: 1,
          startedAt: "2026-08-24T09:00:00",
          completedAt: "2026-08-24T09:00:12",
          findingCount: 3,
          anomalyTypes: [
            "MISSING_BUSINESS_REF",
            "MISSING_STORAGE_FILE",
            "DUPLICATE_FILE",
          ],
        },
      },
    );

    expect(completed.status).toBe("success");
    expect(completed.showProgress).toBe(false);
    expect(completed.message).toContain("완료");
    expect(completed.message).toContain("3건");
  });

  it("documents standard layout, KWCAG, result states and Korean terminology for all four new screens", () => {
    const checklist = getPhase7UiReadinessChecklist();
    const screenIds = checklist.map((screen) => screen.screenId);

    expect(screenIds).toEqual([
      "SCR-FILE-POLICY-MGMT",
      "SCR-ATTACHMENT-METADATA",
      "SCR-ATTACHMENT-DELETE",
      "SCR-ATTACHMENT-INTEGRITY",
    ]);
    for (const screen of checklist) {
      expect(screen.usesExistingAdminShell).toBe(true);
      expect(screen.usesDesignTokens).toBe(true);
      expect(screen.hasKoreanTerminology).toBe(true);
      expect(screen.requiredStates).toEqual(
        expect.arrayContaining([
          "loading",
          "empty",
          "error",
          "permission",
          "success",
        ]),
      );
      expect(screen.kwcagChecks).toEqual(
        expect.arrayContaining([
          "semantic-labels",
          "keyboard-focus",
          "aria-live-result",
          "required-field-indicator",
        ]),
      );
    }
  });

  it("keeps main page budgets under 3MB", () => {
    expect(getPhase7PageBudgetBytes()).toBe(3 * 1024 * 1024);
  });

  it("keeps route contracts aligned with the four generated screens", () => {
    expect(getFilePolicyRouteContract().route).toBe("/admin/file-policies");
    expect(getAttachmentMetadataRouteContract().route).toBe(
      "/admin/attachments",
    );
    expect(getAttachmentDeleteRouteContract().route).toBe(
      "/admin/attachments/delete",
    );
    expect(getAttachmentIntegrityRouteContract().route).toBe(
      "/admin/attachment-integrity",
    );
  });
});
