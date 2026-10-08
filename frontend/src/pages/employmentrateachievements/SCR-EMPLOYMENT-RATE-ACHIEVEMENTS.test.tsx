import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import { EmploymentRateAchievementsPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";
import { ADMIN_ROUTES, canAccessAdminRoute } from "../LoginPage";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({ useAuth: vi.fn() }));
const teacher = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 900,
      menuName: "취업률 실적 관리",
      url: "/faculty/education/employment-rate-achievements",
      displayOrder: 1,
      children: [],
    },
  ],
};
const row = {
  achievementId: 301,
  teacherUserId: 101,
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_ACHIEVEMENT",
  achievementDate: "2026-04-10",
  achievementName: "취업률 원본",
  achievementDetail: "{}",
  achievementStatus: "DRAFT",
};
function auth(roles: string[]) {
  vi.mocked(useAuth).mockReturnValue({
    status: "authenticated",
    user: { ...teacher, roles },
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  });
}

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    auth(["R01"]);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [row], totalElements: 1 },
      meta: {},
    });
  });

  it("has an authorized entry in existing menu navigation", () => {
    const entry = ADMIN_ROUTES.find(
      (r) => r.screenId === "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
    );
    expect(entry?.path).toBe("/faculty/education/employment-rate-achievements");
    expect(canAccessAdminRoute(teacher, entry!.path)).toBe(true);
    expect(canAccessAdminRoute({ ...teacher, menus: [] }, entry!.path)).toBe(
      false,
    );
  });

  it("loads list and fetches selected detail with the selected ID", async () => {
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 원본");
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: row,
      meta: {},
    });
    fireEvent.click(screen.getByTestId("detail-301"));
    await waitFor(() =>
      expect(screen.getByTestId("input-achievementName")).toHaveValue(
        "취업률 원본",
      ),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/301",
    );
    expect(screen.getByTestId("input-evaluationYear")).toBeDisabled();
  });

  it("uses PUT for selected records and retains immutable evaluation year", async () => {
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 원본");
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: row,
      meta: {},
    });
    fireEvent.click(screen.getByTestId("detail-301"));
    await waitFor(() =>
      expect(screen.getByTestId("input-achievementName")).toHaveValue(
        "취업률 원본",
      ),
    );
    fireEvent.change(screen.getByTestId("input-achievementName"), {
      target: { value: "수정" },
    });
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: {
        achievement: { ...row, achievementName: "수정" },
        occurredDateWarning: true,
        warningMessage: "기간 밖 경고",
      },
      meta: {},
    });
    fireEvent.click(screen.getByTestId("save-button"));
    await screen.findByText("기간 밖 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find((c) => c[1]?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/employment-rate-achievements/301");
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({
      evaluationYear: "2026",
      achievementName: "수정",
    });
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "achievementStatus",
    );
  });

  it("blocks confirmed record edits and attachment changes", async () => {
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 원본");
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
      meta: {},
    });
    fireEvent.click(screen.getByTestId("detail-301"));
    await screen.findByText(/현재 상태에서는 수정할 수 없습니다/);
    expect(screen.getByTestId("save-button")).toBeDisabled();
    expect(screen.getByTestId("input-attachmentRef")).toBeDisabled();
  });

  it("R04 reads but does not see single mutation controls", async () => {
    auth(["R04"]);
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 원본");
    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId("excel-tab")).not.toBeInTheDocument();
  });

  it("R07 never automatically calls list/detail/save", async () => {
    auth(["R07"]);
    render(<EmploymentRateAchievementsPage />);
    expect(screen.getByTestId("excel-panel")).toBeInTheDocument();
    expect(screen.queryByTestId("records-tab")).not.toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("bulk-tab"));
    expect(screen.getByTestId("bulk-execute")).toBeDisabled();
  });

  it("empty list renders an empty state", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], totalElements: 0 },
      meta: {},
    });
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("조회 조건에 맞는 결과가 없습니다.");
  });

  it("failed requests surface error without fake rows", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("조회 실패");
    expect(screen.queryByTestId("achievement-row-301")).not.toBeInTheDocument();
  });

  it("permission denial has a dedicated state", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "접근 거부"),
    );
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("접근 거부");
  });

  it("requires mandatory fields before confirmation or POST", async () => {
    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 원본");
    fireEvent.click(screen.getByTestId("save-button"));
    await screen.findByText("평가연도·관리항목·발생일을 입력하세요.");
    expect(
      vi.mocked(apiRequest).mock.calls.some((c) => c[1]?.method === "POST"),
    ).toBe(false);
  });

  it("preview uses supporting bulk-jobs route and cannot execute unapproved policy", async () => {
    auth(["R07"]);
    render(<EmploymentRateAchievementsPage />);
    fireEvent.click(screen.getByTestId("bulk-tab"));
    fireEvent.change(screen.getByTestId("bulk-year"), {
      target: { value: "2026" },
    });
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: {
        executable: false,
        reason: "정책 미승인",
        targets: { achievements: [], totalElements: 0 },
      },
      meta: {},
    });
    fireEvent.click(screen.getByTestId("preview-button"));
    await screen.findByText(/정책 미승인 \/ 대상 0건/);
    expect(screen.getByTestId("bulk-execute")).toBeDisabled();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/bulk-jobs/preview",
      expect.objectContaining({ method: "POST" }),
    );
  });
});
