import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateAchievementsPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";
import { AppRouter } from "../../app/router";

const session = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: session.user,
    logout: vi.fn(),
    login: vi.fn(),
  }),
}));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const row = {
  achievementId: 9,
  managementNo: "ERA-selected",
  teacherUserId: 101,
  evaluationYear: "2026",
  managementItemCode: "item-from-api",
  achievementDate: "2026-05-01",
  achievementName: "실적 원본",
  attachmentIds: [],
  achievementStatus: "DRAFT",
};
const listed = {
  achievements: [row],
  totalElements: 1,
  managementItems: [
    {
      managementItemCode: "item-from-api",
      managementItemName: "설정된 관리항목",
      teacherEditableYn: "Y",
      requiredYn: "Y",
      dataType: "TEXT",
    },
  ],
};
const ok = (data: unknown) => ({ success: true, data, meta: {} });
const user = (...roles: string[]): CurrentUser => ({
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles,
  menus: [
    {
      menuId: 99,
      menuName: "취업률 실적",
      url: "/faculty/employment-rate-achievements",
      displayOrder: 1,
      children: [],
    },
  ],
});

describe("employment rate achievements", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.mocked(apiRequest).mockImplementation(
      async (path) => ok(path.endsWith("/9") ? row : listed) as never,
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);
    session.user = user("R01");
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-achievements",
    );
  });

  it("loads real list options and reads selected detail before PUT", async () => {
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("ERA-selected");
    fireEvent.click(screen.getByTestId("detail-9"));
    await waitFor(() =>
      expect(screen.getByTestId("achievement-name")).toHaveValue("실적 원본"),
    );
    fireEvent.change(screen.getByTestId("achievement-name"), {
      target: { value: "수정됨" },
    });
    vi.mocked(apiRequest).mockImplementation(
      async (path, init) =>
        ok(
          init?.method === "PUT"
            ? {
                achievement: { ...row, achievementName: "수정됨" },
                occurredDateWarning: false,
              }
            : path.endsWith("/9")
              ? row
              : listed,
        ) as never,
    );
    fireEvent.click(screen.getByTestId("save"));
    await screen.findByText("저장되었습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/employment-rate-achievements/9");
    const payload = JSON.parse(call?.[1]?.body as string);
    expect(payload.achievementName).toBe("수정됨");
    expect(payload).not.toHaveProperty("evaluationYear");
    expect(payload).not.toHaveProperty("teacherUserId");
    expect(payload).not.toHaveProperty("achievementId");
  });

  it("new uses POST and warning remains visible after refresh", async () => {
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("ERA-selected");
    fireEvent.click(screen.getByTestId("new"));
    fireEvent.change(screen.getByTestId("management-item"), {
      target: { value: "item-from-api" },
    });
    fireEvent.change(screen.getByTestId("achievement-date"), {
      target: { value: "2025-12-20" },
    });
    vi.mocked(apiRequest).mockImplementation(
      async (_, init) =>
        ok(
          init?.method === "POST"
            ? {
                achievement: row,
                occurredDateWarning: true,
                warningMessage: "발생일 경고",
              }
            : listed,
        ) as never,
    );
    fireEvent.click(screen.getByTestId("save"));
    await screen.findByText("발생일 경고");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("locks confirmed detail and prevents every field change", async () => {
    const confirmed = { ...row, achievementStatus: "EVALUATION_CONFIRMED" };
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        ok(
          path.endsWith("/9")
            ? confirmed
            : { ...listed, achievements: [confirmed] },
        ) as never,
    );
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("ERA-selected");
    fireEvent.click(screen.getByTestId("detail-9"));
    await waitFor(() => expect(screen.getByTestId("save")).toBeDisabled());
    expect(screen.getByTestId("achievement-date")).toBeDisabled();
    expect(screen.getByTestId("achievement-name")).toBeDisabled();
  });

  it("R02 reads but has no mutation or upload controls", async () => {
    render(<EmploymentRateAchievementsPage user={user("R02")} />);
    await screen.findByText("ERA-selected");
    expect(screen.queryByTestId("save")).not.toBeInTheDocument();
    expect(screen.queryByTestId("excel-tab")).not.toBeInTheDocument();
    expect(screen.getByTestId("management-item")).toBeDisabled();
  });

  it("R07 only does not call individual APIs", () => {
    render(<EmploymentRateAchievementsPage user={user("R07")} />);
    expect(apiRequest).not.toHaveBeenCalled();
    expect(screen.queryByTestId("individual-tab")).not.toBeInTheDocument();
    expect(screen.getByTestId("excel-panel")).toBeInTheDocument();
  });

  it("missing required fields do not save", async () => {
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("ERA-selected");
    fireEvent.click(screen.getByTestId("save"));
    await screen.findByText("managementItemCode: 관리항목은 필수입니다.");
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("shows empty list", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      ok({ ...listed, achievements: [], totalElements: 0 }) as never,
    );
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("조회된 실적이 없습니다");
  });

  it("shows request failure and object field errors", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(400, "관리항목 오류", {
        code: "VALIDATION_ERROR",
        message: "관리항목 오류",
        fields: { managementItemCode: "알 수 없는 항목" } as never,
      }),
    );
    render(<EmploymentRateAchievementsPage user={user("R01")} />);
    await screen.findByText("managementItemCode: 알 수 없는 항목");
  });

  it("shows permission denied state", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "범위 없음"),
    );
    render(<EmploymentRateAchievementsPage user={user("R04")} />);
    await screen.findByText("범위 또는 권한이 없습니다");
  });

  it("Excel upload confirms before sending multipart and shows zero writes and diagnostic token on 400", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({
        success: false,
        error: {
          message: "전체 0건 반영",
          fields: {
            "row.3": "중복",
            errorFileToken: "opaque-error",
            savedCount: "0",
          },
        },
      }),
    });
    vi.stubGlobal("fetch", fetcher);
    render(<EmploymentRateAchievementsPage user={user("R07")} />);
    fireEvent.change(screen.getByTestId("upload-file"), {
      target: { files: [new File(["bytes"], "valid.xlsx")] },
    });
    fireEvent.click(screen.getByTestId("upload"));
    await screen.findByText("errorFileToken: opaque-error");
    expect(fetcher).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads",
      expect.objectContaining({ method: "POST", body: expect.any(FormData) }),
    );
    expect(window.confirm).toHaveBeenCalled();
  });

  it("bulk preview is not a target list, changing conditions invalidates confirmation, 409 does not show success", async () => {
    render(<EmploymentRateAchievementsPage user={user("R07")} />);
    fireEvent.click(screen.getByTestId("bulk-tab"));
    fireEvent.change(screen.getByTestId("evaluation-year"), {
      target: { value: "2026" },
    });
    fireEvent.click(screen.getByTestId("preview"));
    expect(screen.getByTestId("execute")).toBeEnabled();
    fireEvent.change(screen.getByTestId("target-condition"), {
      target: { value: '{"department":"a"}' },
    });
    expect(screen.getByTestId("execute")).toBeDisabled();
    fireEvent.click(screen.getByTestId("preview"));
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(409, "정책 미승인"),
    );
    fireEvent.click(screen.getByTestId("execute"));
    await screen.findByText("정책 미승인");
    expect(screen.queryByText("처리 완료")).not.toBeInTheDocument();
  });

  it("job result uses entered ID and displays unprocessed targets", async () => {
    render(<EmploymentRateAchievementsPage user={user("R07")} />);
    fireEvent.click(screen.getByTestId("bulk-tab"));
    fireEvent.change(screen.getByTestId("job-id"), { target: { value: "53" } });
    vi.mocked(apiRequest).mockResolvedValue(
      ok({
        jobId: "53",
        totalCount: 1,
        processedCount: 0,
        unprocessedCount: 1,
        items: [
          {
            itemId: 73,
            targetUserId: 211,
            processedYn: "N",
            unprocessedReason: "평가확정",
          },
        ],
      }) as never,
    );
    fireEvent.click(screen.getByTestId("job-query"));
    await screen.findByText("211: N 평가확정");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/bulk-jobs/53",
    );
  });

  it("authorized database menu navigates through real AppRouter to destination", async () => {
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    const links = screen.getAllByRole("link", {
      name: "취업률 실적",
      hidden: true,
    });
    fireEvent.click(links[0]);
    await screen.findByTestId("employment-page");
    expect(window.location.pathname).toBe(
      "/faculty/employment-rate-achievements",
    );
  });

  it("compatibility alias renders the same page", async () => {
    window.history.replaceState(
      {},
      "",
      "/faculty/education/employment-rate-achievements",
    );
    render(<AppRouter />);
    await screen.findByTestId("employment-page");
  });
});
