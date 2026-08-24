export type Phase7ScreenReadiness = {
  screenId: string;
  route: string;
  usesExistingAdminShell: boolean;
  usesDesignTokens: boolean;
  hasKoreanTerminology: boolean;
  requiredStates: Array<
    "loading" | "empty" | "error" | "permission" | "success"
  >;
  kwcagChecks: Array<
    | "semantic-labels"
    | "keyboard-focus"
    | "aria-live-result"
    | "required-field-indicator"
  >;
};

const REQUIRED_STATES: Phase7ScreenReadiness["requiredStates"] = [
  "loading",
  "empty",
  "error",
  "permission",
  "success",
];

const KWCAG_CHECKS: Phase7ScreenReadiness["kwcagChecks"] = [
  "semantic-labels",
  "keyboard-focus",
  "aria-live-result",
  "required-field-indicator",
];

export function getPhase7PageBudgetBytes() {
  return 3 * 1024 * 1024;
}

export function getPhase7UiReadinessChecklist(): Phase7ScreenReadiness[] {
  return [
    readiness("SCR-FILE-POLICY-MGMT", "/admin/file-policies"),
    readiness("SCR-ATTACHMENT-METADATA", "/admin/attachments"),
    readiness("SCR-ATTACHMENT-DELETE", "/admin/attachments/delete"),
    readiness("SCR-ATTACHMENT-INTEGRITY", "/admin/attachment-integrity"),
  ];
}

function readiness(screenId: string, route: string): Phase7ScreenReadiness {
  return {
    screenId,
    route,
    usesExistingAdminShell: true,
    usesDesignTokens: true,
    hasKoreanTerminology: true,
    requiredStates: REQUIRED_STATES,
    kwcagChecks: KWCAG_CHECKS,
  };
}
