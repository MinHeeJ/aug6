import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementAchievementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    apiRequest: vi.fn(),
  };
});
const session = vi.hoisted(() => ({
  roles: ["R01"],
  userId: 7,
}));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      ...session,
      name: "교원",
      employeeNo: "T007",
    },
  }),
}));
const root = "/api/business/employment-rate-improvements";
const row = {
  achievementId: 11,
  managementNo: "EMP-001",
  teacherUserId: 7,
  teacherName: "교원",
  evaluationYear: 2026,
  certificationStatus: "DRAFT",
  managementItemCode: "EMP_IMPROVEMENT",
  achievementDate: "2026-04-10",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-05",
  mockExamQuestionPeriod: "4월 첫째 주",
  attachmentIds: ["opaque-existing-token"],
};
const envelope = <T,>(data: T) => ({
  success: true,
  data,
  meta: {},
});

function mockApi(
  selected = row,
  options: {
    failDetail?: boolean;
    failSave?: boolean;
    warning?: boolean;
    empty?: boolean;
    failList?: boolean;
  } = {},
) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method === "POST" || init?.method === "PUT") {
      if (
        (init.method === "POST" && path !== root) ||
        (init.method === "PUT" && path !== `${root}/11`)
      ) {
        throw new Error(`Unexpected mutation: ${path} ${init.method}`);
      }
      if (options.failSave)
        throw new ApiClientError(409, "입력기간이 종료되었습니다.");
      return envelope({
        achievement: selected,
        occurredDateWarning: !!options.warning,
        warningMessage: options.warning ? "업적발생일을 확인하세요." : null,
      });
    }
    if (path === `${root}/11`) {
      if (options.failDetail) throw new Error("상세 재조회 실패");
      return envelope(selected);
    }
    if (path.startsWith(`${root}?`)) {
      if (options.failList) throw new Error("목록 조회 실패");
      const query = new URLSearchParams(path.split("?")[1]);
      return envelope({
        achievements: options.empty ? [] : [selected],
        page: Number(query.get("page")),
        pageSize: Number(query.get("pageSize")),
        totalElements: options.empty ? 0 : 41,
      });
    }
    throw new Error(`Unexpected API: ${path} ${init?.method ?? "GET"}`);
  });
}
async function selectDetail() {
  await screen.findByText("EMP-001");
  fireEvent.click(
    screen.getByRole("button", {
      name: "EMP-001 상세",
    }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("관리항목 코드")).toHaveValue(
      "EMP_IMPROVEMENT",
    ),
  );
}
async function fillNew() {
  await screen.findByText("EMP-001");
  fireEvent.change(screen.getByLabelText("관리항목 코드"), {
    target: {
      value: "EMP_IMPROVEMENT",
    },
  });
  fireEvent.change(screen.getByLabelText("업적발생일"), {
    target: {
      value: "2026-04-10",
    },
  });
}

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT", () => {
  beforeEach(() => {
    session.roles = ["R01"];
    session.userId = 7;
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("reaches the screen through the canonical authenticated application route", async () => {
    mockApi();
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-improvement-achievements",
    );
    const { AppRouter } = await import("../../app/router");
    render(<AppRouter />);
    await screen.findByRole("heading", { name: "취업률 제고 실적 관리" });
    await screen.findByText("EMP-001");
    expect(
      screen.getByTestId("employment-rate-improvement-page"),
    ).toBeInTheDocument();
    window.history.replaceState({}, "", "/");
  });

  it("reads persisted detail, preserves opaque attachments, and paginates", async () => {
    mockApi();
    render(<EmploymentRateImprovementAchievementPage />);
    expect(
      screen.getByText("실적 목록을 불러오는 중입니다."),
    ).toBeInTheDocument();
    await selectDetail();
    expect(
      screen.getByTestId("employment-rate-improvement-page"),
    ).toHaveAttribute(
      "data-screen-id",
      "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
    );
    expect(screen.getByLabelText("특강 시작일")).toHaveValue("2026-04-01");
    expect(screen.getByLabelText("특강 종료일")).toHaveValue("2026-04-05");
    expect(screen.getByLabelText("모의평가 출제기간")).toHaveValue(
      "4월 첫째 주",
    );
    expect(screen.getByText("opaque-existing-token")).toBeInTheDocument();
    expect(screen.getByLabelText("평가연도")).toHaveAttribute("readonly");
    expect(screen.getByLabelText("인증상태")).toHaveAttribute("readonly");
    expect(screen.getByLabelText("소유자")).toHaveAttribute("readonly");
    fireEvent.click(
      screen.getByRole("button", {
        name: "다음",
      }),
    );
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${root}?page=1&pageSize=20`),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("페이지 크기")).toBeEnabled(),
    );
    fireEvent.change(screen.getByLabelText("페이지 크기"), {
      target: {
        value: "50",
      },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${root}?page=0&pageSize=50`),
    );
  });

  it("blocks missing fields, reversed lecture dates, and cancelled confirmation", async () => {
    mockApi();
    render(<EmploymentRateImprovementAchievementPage />);
    await screen.findByText("EMP-001");
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(screen.getByText("관리항목 코드를 입력하세요.")).toBeInTheDocument();
    expect(window.confirm).not.toHaveBeenCalled();
    await fillNew();
    fireEvent.change(screen.getByLabelText("특강 시작일"), {
      target: {
        value: "2026-04-05",
      },
    });
    fireEvent.change(screen.getByLabelText("특강 종료일"), {
      target: {
        value: "2026-04-01",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(
      screen.getByText("특강 종료일은 시작일 이후여야 합니다."),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("특강 종료일"), {
      target: {
        value: "2026-04-06",
      },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => !!init?.method),
    ).toBe(false);
  });

  it("creates with only request fields, requeries detail, and shows the server date warning", async () => {
    mockApi(row, {
      warning: true,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await fillNew();
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 상세 정보를 다시 조회했습니다.");
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      ).toBeEnabled(),
    );
    expect(screen.getByText("업적발생일을 확인하세요.")).toBeInTheDocument();
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(call?.[0]).toBe(root);
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "EMP_IMPROVEMENT",
      achievementDate: "2026-04-10",
      attachmentIds: [],
    });
    expect(apiRequest).toHaveBeenCalledWith(`${root}/11`);
    expect(screen.getByLabelText("모의평가 출제기간")).toHaveValue(
      "4월 첫째 주",
    );
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
  });

  it("updates the selected id and retains server attachment tokens", async () => {
    mockApi();
    render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    fireEvent.change(screen.getByLabelText("모의평가 출제기간"), {
      target: {
        value: "수정기간",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 상세 정보를 다시 조회했습니다.");
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      ).toBeEnabled(),
    );
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe(`${root}/11`);
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "EMP_IMPROVEMENT",
      achievementDate: "2026-04-10",
      specialLectureStartDate: "2026-04-01",
      specialLectureEndDate: "2026-04-05",
      mockExamQuestionPeriod: "수정기간",
      attachmentIds: ["opaque-existing-token"],
    });
    expect(screen.getByLabelText("모의평가 출제기간")).toHaveValue(
      "4월 첫째 주",
    );
  });

  it.each([
    "SUBMITTED",
    "DEPARTMENT_CONFIRMED",
    "CERTIFIED",
    "EVALUATION_CONFIRMED",
    "DELETED",
  ])("locks %s and allows an explicit new draft", async (status) => {
    mockApi({
      ...row,
      certificationStatus: status,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    expect(screen.getByLabelText("특강 시작일")).toBeDisabled();
    fireEvent.click(
      screen.getByRole("button", {
        name: "신규 등록",
      }),
    );
    expect(screen.getByLabelText("관리항목 코드")).toBeEnabled();
  });

  it("makes other owners and R02 viewers read-only", async () => {
    mockApi({
      ...row,
      teacherUserId: 8,
    });
    const { unmount } = render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    unmount();
    session.roles = ["R02"];
    render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", {
        name: "신규 등록",
      }),
    ).toBeDisabled();
  });

  it("allows R09 to open and create achievements and edit another owner's draft", async () => {
    session.roles = ["R09"];
    session.userId = 9;
    mockApi();
    render(<EmploymentRateImprovementAchievementPage />);
    await fillNew();
    expect(
      screen.getByRole("button", {
        name: "신규 등록",
      }),
    ).toBeEnabled();
    expect(screen.getByLabelText("관리항목 코드")).toBeEnabled();
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 상세 정보를 다시 조회했습니다.");
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      ).toBeEnabled(),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      root,
      expect.objectContaining({
        method: "POST",
      }),
    );
    await selectDetail();
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "저장",
        }),
      ).toBeEnabled(),
    );
    expect(screen.getByLabelText("소유자")).toHaveValue("교원 (7)");
    expect(screen.getByLabelText("인증상태")).toHaveValue("작성");
    expect(screen.getByLabelText("모의평가 출제기간")).toBeEnabled();
    fireEvent.change(screen.getByLabelText("모의평가 출제기간"), {
      target: {
        value: "관리자 수정기간",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 상세 정보를 다시 조회했습니다.");
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      ).toBeEnabled(),
    );
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe(`${root}/11`);
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "EMP_IMPROVEMENT",
      achievementDate: "2026-04-10",
      specialLectureStartDate: "2026-04-01",
      specialLectureEndDate: "2026-04-05",
      mockExamQuestionPeriod: "관리자 수정기간",
      attachmentIds: ["opaque-existing-token"],
    });
  });

  it.each([
    "SUBMITTED",
    "DEPARTMENT_CONFIRMED",
    "CERTIFIED",
    "EVALUATION_CONFIRMED",
    "DELETED",
  ])("keeps another owner's %s achievement locked for R09", async (status) => {
    session.roles = ["R09"];
    session.userId = 9;
    mockApi({
      ...row,
      certificationStatus: status,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    expect(screen.getByLabelText("특강 시작일")).toBeDisabled();
    expect(
      screen.getByRole("button", {
        name: "신규 등록",
      }),
    ).toBeEnabled();
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => !!init?.method),
    ).toBe(false);
  });

  it("does not read APIs for an unauthorized role", () => {
    session.roles = ["R07"];
    render(<EmploymentRateImprovementAchievementPage />);
    expect(
      screen.getByText("취업률 제고 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("preserves user input after a conflict", async () => {
    mockApi(row, {
      failSave: true,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await selectDetail();
    fireEvent.change(screen.getByLabelText("모의평가 출제기간"), {
      target: {
        value: "보존할 값",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("입력기간이 종료되었습니다.");
    expect(screen.getByLabelText("모의평가 출제기간")).toHaveValue("보존할 값");
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeEnabled();
  });

  it("keeps multiple server validation messages for one addressable field", async () => {
    mockApi();
    const original = vi.mocked(apiRequest).getMockImplementation()!;
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method) {
        throw new ApiClientError(400, "입력값 확인", {
          code: "VALIDATION_ERROR",
          message: "입력값 확인",
          fields: [
            {
              field: "managementItemCode",
              message: "유효한 코드가 아닙니다.",
            },
            {
              field: "managementItemCode",
              message: "교육영역 코드가 필요합니다.",
            },
          ],
        });
      }
      return original(path, init);
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await fillNew();
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("교육영역 코드가 필요합니다.");
    expect(screen.getByText("유효한 코드가 아닙니다.")).toBeInTheDocument();
    expect(screen.getByLabelText("관리항목 코드")).toHaveValue(
      "EMP_IMPROVEMENT",
    );
  });

  it("does not permit duplicate creation after a successful save whose detail read fails", async () => {
    mockApi(row, {
      failDetail: true,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await fillNew();
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText(
      "저장은 완료되었으나 상세 재조회에 실패했습니다. 상세 정보를 다시 선택하세요.",
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      ).toBeEnabled(),
    );
    expect(screen.getByText("상세 재조회 실패")).toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
  });

  it("blocks a stale form after a failed detail selection", async () => {
    mockApi(row, {
      failDetail: true,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await fillNew();
    fireEvent.click(
      screen.getByRole("button", {
        name: "EMP-001 상세",
      }),
    );
    await screen.findByText("상세 재조회 실패");
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
  });

  it("renders empty and list error states without fabricating rows", async () => {
    mockApi(row, {
      empty: true,
    });
    const { unmount } = render(<EmploymentRateImprovementAchievementPage />);
    await screen.findByText("등록된 취업률 제고 실적이 없습니다.");
    unmount();
    mockApi(row, {
      failList: true,
    });
    render(<EmploymentRateImprovementAchievementPage />);
    await screen.findByText("목록 조회 실패");
    expect(screen.queryByText("EMP-001")).not.toBeInTheDocument();
  });
});
