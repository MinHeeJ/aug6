import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementManagementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";

const auth = vi.hoisted(() => ({
  user: { userId: 101, name: "교원", roles: ["R01"] },
}));
vi.mock("../../app/AuthProvider", () => ({ useAuth: () => auth }));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const base = "/api/business/employment-rate-improvements";
const row = {
  achievementId: 77,
  managementNo: "ERI-test",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2025",
  managementItemCode: "FR-029",
  achievementDate: "2025-04-10",
  achievementStatus: "DRAFT",
  specialLectureStartDate: "2025-04-01",
  specialLectureEndDate: "2025-04-03",
  mockExamQuestionPeriod: "기존 출제기간",
  attachmentIds: [],
};
const list = {
  achievements: [row],
  totalElements: 1,
  managementItems: [
    {
      managementItemId: 99,
      code: "FR-029",
      name: "취업률 제고",
      evaluationYear: "2025",
      requiredYn: "Y",
      teacherEditableYn: "Y",
      dataType: "TEXT",
    },
  ],
};
const ok = <T,>(data: T) => ({ success: true, data, meta: {} });

function respond(detail = row) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method === "PUT" || init?.method === "POST") {
      return ok({
        achievement: { ...detail, mockExamQuestionPeriod: "수정 출제기간" },
        occurredDateWarning: true,
        warningMessage: "평가대상 기간 밖 경고와 함께 저장되었습니다.",
      }) as never;
    }
    return ok(path === `${base}/77` ? detail : list) as never;
  });
}

describe("취업률 제고 실적", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiRequest).mockReset();
    auth.user = { userId: 101, name: "교원", roles: ["R01"] };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    respond();
  });

  it("loads scoped rows, requests selected detail and PUTs only approved mutable fields", async () => {
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("ERI-test");
    fireEvent.click(screen.getByTestId("employment-improvement-detail-77"));
    await waitFor(() =>
      expect(
        screen.getByTestId("employment-improvement-mock-exam-question-period"),
      ).toHaveValue("기존 출제기간"),
    );
    fireEvent.change(
      screen.getByTestId("employment-improvement-mock-exam-question-period"),
      {
        target: { value: "수정 출제기간" },
      },
    );
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("평가대상 기간 밖 경고와 함께 저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(`${base}/77`);
    const mutation = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(mutation?.[0]).toBe(`${base}/77`);
    expect(JSON.parse(String(mutation?.[1]?.body))).toEqual({
      managementItemCode: "FR-029",
      achievementDate: "2025-04-10",
      specialLectureStartDate: "2025-04-01",
      specialLectureEndDate: "2025-04-03",
      mockExamQuestionPeriod: "수정 출제기간",
      attachmentIds: [],
    });
    expect(window.confirm).toHaveBeenCalled();
    expect(apiRequest).toHaveBeenCalledTimes(4);
  });

  it("create uses collection POST and client validation prevents an empty save", async () => {
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("ERI-test");
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    expect(window.confirm).not.toHaveBeenCalled();
    expect(screen.getByText("업적발생일을 입력하세요.")).toBeInTheDocument();
    fireEvent.change(
      screen.getByTestId("employment-improvement-achievement-date"),
      {
        target: { value: "2025-04-10" },
      },
    );
    fireEvent.change(
      screen.getByTestId("employment-improvement-management-item"),
      { target: { value: "FR-029" } },
    );
    fireEvent.change(
      screen.getByTestId("employment-improvement-mock-exam-question-period"),
      {
        target: { value: "새 출제기간" },
      },
    );
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await waitFor(() =>
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(
            ([path, init]) => path === base && init?.method === "POST",
          ),
      ).toBe(true),
    );
  });

  it.each(["NUMBER", "BOOLEAN"])(
    "does not use lecture dates as a required %s value",
    async (dataType) => {
      vi.mocked(apiRequest).mockResolvedValue(
        ok({
          ...list,
          managementItems: list.managementItems.map((item) => ({
            ...item,
            dataType,
          })),
        }),
      );
      render(<EmploymentRateImprovementManagementPage />);
      await screen.findByText("ERI-test");
      fireEvent.change(
        screen.getByTestId("employment-improvement-achievement-date"),
        {
          target: { value: "2025-04-10" },
        },
      );
      fireEvent.change(
        screen.getByTestId("employment-improvement-management-item"),
        { target: { value: "FR-029" } },
      );
      fireEvent.change(
        screen.getByTestId("employment-improvement-special-lecture-start-date"),
        {
          target: { value: "2025-04-01" },
        },
      );
      fireEvent.change(
        screen.getByTestId("employment-improvement-special-lecture-end-date"),
        {
          target: { value: "2025-04-03" },
        },
      );
      fireEvent.click(screen.getByTestId("employment-improvement-save"));
      expect(
        screen.getByText("관리항목의 필수 실적값을 입력하세요."),
      ).toBeInTheDocument();
      expect(window.confirm).not.toHaveBeenCalled();
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([, init]) => init?.method === "POST"),
      ).toBe(false);
    },
  );

  it("locks confirmed data and shows readonly identity and status", async () => {
    respond({ ...row, achievementStatus: "EVALUATION_CONFIRMED" });
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("ERI-test");
    fireEvent.click(screen.getByTestId("employment-improvement-detail-77"));
    await screen.findByText(/본인의 작성중 실적만/);
    expect(screen.getByTestId("employment-improvement-save")).toBeDisabled();
    expect(
      screen.getByTestId("employment-improvement-special-lecture-start-date"),
    ).toBeDisabled();
    expect(screen.getByTestId("employment-improvement-status")).toBeDisabled();
  });

  it("R02 can read but cannot mutate", async () => {
    auth.user = { ...auth.user, roles: ["R02"] };
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("ERI-test");
    expect(screen.getByTestId("employment-improvement-save")).toBeDisabled();
    expect(screen.getByTestId("employment-improvement-new")).toBeDisabled();
  });

  it("submits current search filters and page size and handles empty results", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      ok({ ...list, achievements: [], totalElements: 0 }),
    );
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("조회된 취업률 제고 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("employment-improvement-filter-teacher-name"),
      { target: { value: "이교원" } },
    );
    fireEvent.click(screen.getByTestId("employment-improvement-search"));
    await waitFor(() =>
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([path]) => path.includes("teacherName=")),
      ).toBe(true),
    );
    fireEvent.change(screen.getByTestId("employment-improvement-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([path]) => path.includes("pageSize=50")),
      ).toBe(true),
    );
  });

  it("shows permission errors and field errors without pretending a failed save succeeded", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "조회 권한이 없습니다."),
    );
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("해당 실적의 조회 또는 처리 권한이 없습니다.");
    expect(screen.getByTestId("employment-improvement-save")).toBeDisabled();
  });

  it("keeps original form on 409 and renders server validation", async () => {
    respond();
    render(<EmploymentRateImprovementManagementPage />);
    await screen.findByText("ERI-test");
    fireEvent.click(screen.getByTestId("employment-improvement-detail-77"));
    await waitFor(() =>
      expect(
        screen.getByTestId("employment-improvement-mock-exam-question-period"),
      ).toHaveValue("기존 출제기간"),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(409, "입력기간 밖입니다."),
    );
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("입력기간 밖입니다.");
    expect(
      screen.getByTestId("employment-improvement-mock-exam-question-period"),
    ).toHaveValue("기존 출제기간");
    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력값을 확인하세요.", {
        code: "VALIDATION_ERROR",
        message: "입력값을 확인하세요.",
        fields: [
          {
            field: "mockExamQuestionPeriod",
            message: "관리항목 데이터 형식을 확인하세요.",
          },
        ],
      }),
    );
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("관리항목 데이터 형식을 확인하세요.");
  });
});
