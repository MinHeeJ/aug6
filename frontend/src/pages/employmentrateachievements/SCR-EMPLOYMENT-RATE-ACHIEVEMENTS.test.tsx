import { describe, expect, it } from "vitest";
import {
  createEmploymentRateAchievementState,
  getEmploymentRateAchievementRouteContract,
  reduceEmploymentRateAchievementState,
} from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS route contract and state handling", () => {
  it("declares the authorized faculty route and relative employment-rate operations", () => {
    expect(getEmploymentRateAchievementRouteContract()).toEqual({
      route: "/faculty/education/employment-rate-achievements",
      screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
      operations: [
        "listEmploymentRateAchievements",
        "createEmploymentRateAchievement",
        "updateEmploymentRateAchievement",
        "downloadEmploymentRateAchievements",
        "uploadEmploymentRateAchievementsExcel",
        "createEmploymentRateBulkJob",
        "getEmploymentRateBulkJob",
      ],
    });
  });

  it("preserves loading, empty, ready, error, permission, and success states", () => {
    let state = createEmploymentRateAchievementState();
    expect(state.status).toBe("loading");
    state = reduceEmploymentRateAchievementState(state, { type: "empty" });
    expect(state.status).toBe("empty");
    state = reduceEmploymentRateAchievementState(state, {
      type: "error",
      message: "조회 실패",
    });
    expect(state).toEqual({ status: "error", message: "조회 실패" });
    state = reduceEmploymentRateAchievementState(state, { type: "permission" });
    expect(state.status).toBe("permission");
    state = reduceEmploymentRateAchievementState(state, {
      type: "success",
      message: "저장되었습니다.",
    });
    expect(state).toEqual({ status: "success", message: "저장되었습니다." });
  });
});
