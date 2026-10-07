import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  authApi,
  type CurrentUser,
} from "../../api/apiClient";
import { AuthProvider } from "../../app/AuthProvider";
import { AppRouter } from "../../app/router";
import {
  EmploymentRateImprovementPage,
  type EmploymentRateImprovement,
} from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    apiRequest: vi.fn(),
    authApi: { ...actual.authApi, me: vi.fn() },
  };
});
const path = "/api/business/employment-rate-improvements";
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  employeeNo: "E101",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 191,
      menuName: "취업률 제고 실적 관리",
      displayOrder: 1,
      url: "/faculty/education/employment-rate-improvements",
      screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
      children: [],
    },
  ],
};
const row: EmploymentRateImprovement = {
  achievementId: 91,
  managementNo: "ERI-91",
  teacherUserId: 101,
  teacherName: "교원",
  organizationCode: "DEPT",
  evaluationYear: "2026",
  managementItemCode: "employment-rate-improvements",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-10",
  mockExamQuestionPeriod: "4월",
  attachmentIds: [],
};
const page = {
  achievements: [row],
  totalElements: 1,
  managementItems: [
    {
      managementItemCode: row.managementItemCode,
      managementItemName: "취업률 제고",
    },
  ],
};
function response(data: unknown) {
  return { success: true, data, meta: {} };
}

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(authApi.me).mockResolvedValue(
    response(user) as Awaited<ReturnType<typeof authApi.me>>,
  );
  vi.spyOn(window, "confirm").mockReturnValue(true);
  vi.mocked(apiRequest).mockImplementation(async (url, init) => {
    if (init?.method)
      return response({
        achievement: row,
        occurredDateWarning: false,
      }) as never;
    if (url === `${path}/91`) return response(row) as never;
    return response(page) as never;
  });
});
afterEach(() => {
  vi.restoreAllMocks();
  window.history.replaceState({}, "", "/");
});

describe("취업률 제고 실적", () => {
  it("renders the existing canonical route for an authenticated menu owner", async () => {
    window.history.replaceState({}, "", user.menus[0].url);
    render(
      <AuthProvider>
        <AppRouter />
      </AuthProvider>,
    );
    expect(
      await screen.findByRole("heading", { name: "취업률 제고 실적 관리" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("ERI-91")).toBeInTheDocument();
  });

  it("loads selected detail and PUTs that ID without sending immutable fields", async () => {
    render(<EmploymentRateImprovementPage user={user} />);
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-91"),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("employment-improvement-mock-period"),
      ).toHaveValue("4월"),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("employment-improvement-save"),
      ).not.toBeDisabled(),
    );
    fireEvent.change(screen.getByTestId("employment-improvement-mock-period"), {
      target: { value: "수정" },
    });
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("저장되었습니다.");
    const write = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(write?.[0]).toBe(`${path}/91`);
    const payload = JSON.parse(String(write?.[1]?.body));
    expect(payload.mockExamQuestionPeriod).toBe("수정");
    for (const key of [
      "achievementId",
      "teacherUserId",
      "evaluationYear",
      "achievementStatus",
      "managementNo",
    ]) {
      expect(payload).not.toHaveProperty(key);
    }
    expect(apiRequest).toHaveBeenCalledWith(expect.stringContaining("page=0"));
  });

  it("creates via POST after validation and displays the server date warning", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_url, init) =>
      init?.method
        ? (response({
            achievement: row,
            occurredDateWarning: true,
            warningMessage: "평가대상 기간 밖 저장 완료",
          }) as never)
        : (response(page) as never),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("ERI-91");
    fireEvent.change(screen.getByTestId("employment-improvement-item"), {
      target: { value: row.managementItemCode },
    });
    fireEvent.change(screen.getByTestId("employment-improvement-date"), {
      target: { value: row.achievementDate },
    });
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("평가대상 기간 밖 저장 완료");
    expect(apiRequest).toHaveBeenCalledWith(
      path,
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("blocks reversed or incomplete special lecture periods locally", async () => {
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("ERI-91");
    fireEvent.change(screen.getByTestId("employment-improvement-item"), {
      target: { value: row.managementItemCode },
    });
    fireEvent.change(screen.getByTestId("employment-improvement-date"), {
      target: { value: row.achievementDate },
    });
    fireEvent.change(screen.getByTestId("employment-improvement-start"), {
      target: { value: "2026-04-11" },
    });
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("특강기간을 함께 입력하고 날짜 순서를 확인하세요.");
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
  });

  it("disables editing and attachment mutation for a confirmed detail", async () => {
    vi.mocked(apiRequest).mockImplementation(
      async (url) =>
        response(
          url === `${path}/91`
            ? { ...row, achievementStatus: "EVALUATION_CONFIRMED" }
            : page,
        ) as never,
    );
    render(<EmploymentRateImprovementPage user={user} />);
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-91"),
    );
    await screen.findByText("확정 또는 제출 상태 실적은 수정할 수 없습니다.");
    expect(screen.getByTestId("employment-improvement-save")).toBeDisabled();
    expect(screen.getByTestId("employment-improvement-date")).toBeDisabled();
  });

  it("handles loading, empty, failure, field errors and role-denied states", async () => {
    vi.mocked(apiRequest).mockResolvedValueOnce(
      response({ ...page, achievements: [] }) as never,
    );
    const mounted = render(<EmploymentRateImprovementPage user={user} />);
    expect(screen.getByText(/불러/)).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.queryByTestId("employment-improvement-row-91"),
      ).not.toBeInTheDocument(),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "관리항목 오류", {
        code: "VALIDATION_ERROR",
        message: "관리항목 오류",
        fields: [
          {
            field: "managementItemCode",
            message: "활성 관리항목을 선택하세요.",
          },
        ],
      }),
    );
    fireEvent.click(screen.getByTestId("employment-improvement-refresh"));
    await screen.findByText("활성 관리항목을 선택하세요.");
    mounted.unmount();
    render(
      <EmploymentRateImprovementPage user={{ ...user, roles: ["R07"] }} />,
    );
    expect(
      screen.getByText("취업률 제고 실적 권한이 없습니다"),
    ).toBeInTheDocument();
  });

  it("changes page size and keeps the search query API-backed", async () => {
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("ERI-91");
    fireEvent.change(screen.getByTestId("employment-improvement-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${path}?page=0&pageSize=50`),
    );
    fireEvent.change(
      screen.getByTestId("employment-improvement-search-input"),
      { target: { value: "ERI-91" } },
    );
    fireEvent.click(screen.getByTestId("employment-improvement-search-button"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        expect.stringContaining("managementNo=ERI-91"),
      ),
    );
  });
});
