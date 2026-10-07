import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  apiRequest,
  ApiClientError,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const principal = vi.hoisted(() => ({
  user: {
    userId: 101,
    loginId: "faculty",
    employeeNo: "E0101",
    name: "교원",
    roles: ["R01"],
    menus: [
      {
        menuId: 993,
        menuName: "취업률 실적 관리",
        displayOrder: 1,
        children: [],
        url: "/faculty/education/employment-rate-achievements",
        screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENT",
      },
    ],
  },
}));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: principal.user,
    logout: vi.fn(),
  }),
}));
const base = "/api/business/employment-rate-achievements";
const row = {
  achievementId: 88,
  managementNo: "persisted-management-no",
  evaluationYear: "2026",
  managementItemCode: "item",
  achievementDate: "2026-04-10",
  achievementName: "취업 지원",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const user = (roles: string[]): CurrentUser => ({ ...principal.user, roles });
const ok = (data: unknown) => ({ success: true, data, meta: {} });

describe("취업률 실적 화면", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    window.history.replaceState({}, "", "/");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest).mockImplementation(async (path) => {
      if (path.includes("management-items"))
        return ok([
          { managementItemCode: "item", managementItemName: "운영항목" },
        ]) as never;
      if (path === `${base}/88`) return ok(row) as never;
      return ok({ achievements: [row], totalElements: 1 }) as never;
    });
  });

  it("authorized canonical direct route renders the real screen through the existing shell", async () => {
    window.history.replaceState(
      {},
      "",
      "/faculty/education/employment-rate-achievements",
    );
    render(<AppRouter />);
    expect(
      await screen.findByTestId("employment-rate-page"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("persisted-management-no"),
    ).toBeInTheDocument();
  });

  it("fetches selected detail then uses PUT with no mutable owner or evaluation year", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (path.includes("management-items"))
        return ok([
          { managementItemCode: "item", managementItemName: "운영항목" },
        ]) as never;
      if (init?.method === "PUT")
        return ok({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "기간 밖 경고",
        }) as never;
      if (path === `${base}/88`) return ok(row) as never;
      return ok({ achievements: [row], totalElements: 1 }) as never;
    });
    render(<EmploymentRateAchievementPage user={user(["R01"])} />);
    fireEvent.click(await screen.findByTestId("employment-detail-88"));
    await waitFor(() =>
      expect(screen.getByTestId("employment-name")).toHaveValue("취업 지원"),
    );
    fireEvent.change(screen.getByTestId("employment-name"), {
      target: { value: "수정된 실적" },
    });
    fireEvent.click(screen.getByTestId("employment-save"));
    await screen.findByText("기간 밖 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find((entry) => entry[1]?.method === "PUT");
    expect(call?.[0]).toBe(`${base}/88`);
    expect(JSON.parse(call?.[1]?.body as string)).toEqual({
      managementItemCode: "item",
      achievementDate: "2026-04-10",
      achievementName: "수정된 실적",
      attachmentIds: [],
    });
  });

  it("confirmed detail is readonly and save is disabled", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) => {
      if (path === `${base}/88`)
        return ok({
          ...row,
          achievementStatus: "EVALUATION_CONFIRMED",
        }) as never;
      if (path.includes("management-items")) return ok([]) as never;
      return ok({ achievements: [row], totalElements: 1 }) as never;
    });
    render(<EmploymentRateAchievementPage user={user(["R01"])} />);
    fireEvent.click(await screen.findByTestId("employment-detail-88"));
    await screen.findByText("확정 또는 처리중 실적은 수정할 수 없습니다.");
    expect(screen.getByTestId("employment-save")).toBeDisabled();
    expect(screen.getByTestId("employment-date")).toBeDisabled();
  });

  it("R07 gets wizard and policy diagnostics without ordinary list or invented target counts", async () => {
    vi.mocked(apiRequest).mockResolvedValue(ok([]) as never);
    render(<EmploymentRateAchievementPage user={user(["R07"])} />);
    expect(
      screen.queryByTestId("employment-individual-tab"),
    ).not.toBeInTheDocument();
    await screen.findByText("업로드 이력이 없습니다.");
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.every(([path]) => !path.startsWith(`${base}?`)),
    ).toBe(true);
    fireEvent.click(screen.getByTestId("employment-bulk-tab"));
    expect(
      screen.getByText(/생성자격·삭제상태 정책 미승인/),
    ).toBeInTheDocument();
    expect(screen.queryByText(/대상 3건/)).not.toBeInTheDocument();
  });

  it("empty and forbidden API states are user-visible", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      ok({ achievements: [], totalElements: 0 }) as never,
    );
    const { unmount } = render(
      <EmploymentRateAchievementPage user={user(["R01"])} />,
    );
    await screen.findByText("조회조건에 맞는 실적이 없습니다.");
    unmount();
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "범위 권한 없음"),
    );
    render(<EmploymentRateAchievementPage user={user(["R01"])} />);
    await screen.findByText("역할 및 데이터 범위 권한을 확인하세요.");
  });

  it("R02 can read but cannot find the save action", async () => {
    render(<EmploymentRateAchievementPage user={user(["R02"])} />);
    await screen.findByText("persisted-management-no");
    expect(screen.queryByTestId("employment-save")).not.toBeInTheDocument();
    expect(screen.getByTestId("employment-date")).toBeDisabled();
  });
});
