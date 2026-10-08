import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EmploymentRateImprovementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
import {
  employmentRateImprovementApi as api,
  type EmploymentRateImprovement,
} from "./employmentRateImprovementApi";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import { AppRouter } from "../../app/router";

const auth = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: auth.user,
    logout: vi.fn(),
    login: vi.fn(),
  }),
}));
vi.mock("./employmentRateImprovementApi", async () => {
  const actual = await vi.importActual<
    typeof import("./employmentRateImprovementApi")
  >("./employmentRateImprovementApi");
  return {
    ...actual,
    employmentRateImprovementApi: {
      list: vi.fn(),
      get: vi.fn(),
      save: vi.fn(),
    },
  };
});
const faculty: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 300,
      menuName: "취업률 제고 실적 관리",
      displayOrder: 1,
      children: [],
      url: "/faculty/employment-rate-improvement-achievements",
      screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
    },
  ],
};
const row: EmploymentRateImprovement = {
  achievementId: 82,
  teacherUserId: 101,
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
  specialLectureStartDate: "2026-04-10",
  specialLectureEndDate: "2026-04-17",
  mockExamQuestionPeriod: "출제기간 원문",
  attachmentRef: null,
};
function list(
  rows: EmploymentRateImprovement[] = [row],
  items = [row.managementItemCode],
) {
  return {
    success: true,
    data: {
      achievements: rows,
      totalElements: rows.length,
      page: 0,
      pageSize: 20,
      managementItems: items,
    },
    meta: {},
  };
}

describe("취업률 제고 실적", () => {
  beforeEach(() => {
    vi.mocked(api.list).mockReset().mockResolvedValue(list());
    vi.mocked(api.get)
      .mockReset()
      .mockResolvedValue({ success: true, data: row, meta: {} });
    vi.mocked(api.save)
      .mockReset()
      .mockResolvedValue({
        success: true,
        data: { achievement: row, occurredDateWarning: false },
        meta: {},
      });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    auth.user = faculty;
    window.history.replaceState({}, "", "/");
  });

  it("authorized menu navigation reaches the registered screen", async () => {
    render(<AppRouter />);
    const link = screen.getAllByText("취업률 제고 실적 관리")[0].closest("a");
    expect(link).not.toBeNull();
    fireEvent.click(link!);
    await screen.findByTestId("employment-improvement-page");
    expect(window.location.pathname).toBe(
      "/faculty/employment-rate-improvement-achievements",
    );
    expect(api.list).toHaveBeenCalledWith(0, 20, "");
  });

  it("lists API rows and loads detail using selected identity", async () => {
    render(<EmploymentRateImprovementPage user={faculty} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-period")).toHaveValue(
        "출제기간 원문",
      ),
    );
    expect(api.get).toHaveBeenCalledWith(82);
  });

  it("creates with POST identity omitted and refreshes after success", async () => {
    render(<EmploymentRateImprovementPage user={faculty} />);
    await screen.findByTestId("improvement-detail-82");
    fireEvent.change(screen.getByTestId("improvement-item"), {
      target: { value: row.managementItemCode },
    });
    fireEvent.change(screen.getByTestId("improvement-date"), {
      target: { value: "2026-04-10" },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("저장되었습니다.");
    expect(api.save).toHaveBeenCalledWith(
      undefined,
      expect.objectContaining({ achievementDate: "2026-04-10" }),
    );
    expect(api.list).toHaveBeenCalledTimes(2);
  });

  it("updates the selected path ID without owner year or status in payload and shows warning", async () => {
    vi.mocked(api.save).mockResolvedValue({
      success: true,
      data: {
        achievement: row,
        occurredDateWarning: true,
        warningMessage: "평가대상 기간 밖 경고",
      },
      meta: {},
    });
    render(<EmploymentRateImprovementPage user={faculty} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-period")).toHaveValue(
        "출제기간 원문",
      ),
    );
    fireEvent.change(screen.getByTestId("improvement-period"), {
      target: { value: "새 원문" },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("평가대상 기간 밖 경고");
    const [id, input] = vi.mocked(api.save).mock.calls[0];
    expect(id).toBe(82);
    expect(input.mockExamQuestionPeriod).toBe("새 원문");
    expect(input).not.toHaveProperty("evaluationYear");
    expect(input).not.toHaveProperty("teacherUserId");
    expect(input).not.toHaveProperty("achievementStatus");
  });

  it("required values block saving", async () => {
    render(<EmploymentRateImprovementPage user={faculty} />);
    await screen.findByTestId("improvement-detail-82");
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("관리항목은 필수입니다.");
    expect(api.save).not.toHaveBeenCalled();
  });

  it("cancelled confirmation makes no API change", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<EmploymentRateImprovementPage user={faculty} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-period")).toHaveValue(
        "출제기간 원문",
      ),
    );
    fireEvent.click(screen.getByTestId("improvement-save"));
    expect(api.save).not.toHaveBeenCalled();
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks the %s detail",
    async (state) => {
      vi.mocked(api.get).mockResolvedValue({
        success: true,
        data: { ...row, achievementStatus: state },
        meta: {},
      });
      render(<EmploymentRateImprovementPage user={faculty} />);
      fireEvent.click(await screen.findByTestId("improvement-detail-82"));
      await screen.findByText(
        "현재 상태 또는 소유권에 따라 수정할 수 없습니다.",
      );
      expect(screen.getByTestId("improvement-save")).toBeDisabled();
      expect(screen.getByTestId("improvement-period")).toBeDisabled();
    },
  );

  it("department role is read only", async () => {
    render(
      <EmploymentRateImprovementPage user={{ ...faculty, roles: ["R02"] }} />,
    );
    await screen.findByTestId("improvement-detail-82");
    expect(screen.queryByTestId("improvement-save")).not.toBeInTheDocument();
    expect(screen.getByTestId("improvement-period")).toBeDisabled();
  });

  it("R07 cannot access the route", () => {
    auth.user = { ...faculty, roles: ["R07"] };
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-improvement-achievements",
    );
    render(<AppRouter />);
    expect(
      screen.getByText("취업률 제고 실적 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(api.list).not.toHaveBeenCalled();
  });

  it("R09 retains route admission", async () => {
    render(
      <EmploymentRateImprovementPage user={{ ...faculty, roles: ["R09"] }} />,
    );
    await screen.findByTestId("improvement-detail-82");
    expect(screen.getByTestId("improvement-save")).toBeEnabled();
  });

  it("empty API rows show empty state", async () => {
    vi.mocked(api.list).mockResolvedValue(list([]));
    render(<EmploymentRateImprovementPage user={faculty} />);
    await screen.findByText("데이터 없음");
  });

  it("no DB selectable settings disables save", async () => {
    vi.mocked(api.list).mockResolvedValue(list([], []));
    render(<EmploymentRateImprovementPage user={faculty} />);
    await screen.findByText("입력 가능한 관리항목 설정이 없습니다.");
    expect(screen.getByTestId("improvement-save")).toBeDisabled();
  });

  it("request errors leave retry controls available", async () => {
    vi.mocked(api.list).mockRejectedValue(new Error("조회 실패"));
    render(<EmploymentRateImprovementPage user={faculty} />);
    await screen.findByText("조회 실패");
    expect(screen.getByTestId("improvement-search")).toBeEnabled();
  });

  it("server object fields are shown without altering existing shared API client", async () => {
    vi.mocked(api.save).mockRejectedValue(
      new ApiClientError(400, "검증 실패", {
        code: "VALIDATION_ERROR",
        message: "검증 실패",
        fields: { managementItemCode: "입력기간 설정을 확인하세요." } as never,
      }),
    );
    render(<EmploymentRateImprovementPage user={faculty} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-period")).toHaveValue(
        "출제기간 원문",
      ),
    );
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("입력기간 설정을 확인하세요.");
  });
});
