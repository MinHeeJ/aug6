import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({ status: "authenticated", user, logout: vi.fn() }),
}));
const row = {
  achievementId: 81,
  managementNo: "ERI-TEST",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-10",
  mockExamQuestionPeriod: "4월",
  certificationStatus: "DRAFT",
  attachmentIds: [],
};
const search = {
  achievements: [row],
  totalElements: 1,
  page: 0,
  pageSize: 20,
  managementItems: [
    {
      managementItemCode: row.managementItemCode,
      managementItemName: "취업률 제고",
      teacherEditableYn: "Y",
      requiredYn: "Y",
      dataType: "TEXT",
      evaluationYear: "2026",
    },
  ],
};
const success = (data: unknown) => ({ success: true, data, meta: {} });

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  window.history.replaceState({}, "", "/");
});

describe("취업률 제고 화면", () => {
  it("reaches the real screen through the canonical route and its alias", async () => {
    vi.mocked(apiRequest).mockResolvedValue(success(search));
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-improvement-achievements",
    );
    const view = render(<AppRouter />);
    expect(await screen.findByText("ERI-TEST")).toBeInTheDocument();
    view.unmount();
    window.history.replaceState(
      {},
      "",
      "/faculty/education/employment-rate-improvements",
    );
    render(<AppRouter />);
    expect(await screen.findByText("ERI-TEST")).toBeInTheDocument();
  });

  it("loads list and selected detail from separate approved operations", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(success(search))
      .mockResolvedValueOnce(success(row));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.click(screen.getByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-mock-period")).toHaveValue("4월"),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements/81",
    );
  });

  it("creates without identity fields and refreshes after save", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(success(search))
      .mockResolvedValueOnce(
        success({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "평가기간 밖 경고",
        }),
      )
      .mockResolvedValueOnce(success(search));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.change(screen.getByTestId("improvement-management-item"), {
      target: { value: row.managementItemCode },
    });
    fireEvent.change(screen.getByTestId("improvement-achievementDate"), {
      target: { value: row.achievementDate },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("평가기간 밖 경고");
    const [, init] = vi.mocked(apiRequest).mock.calls[1];
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: null,
      specialLectureEndDate: null,
      mockExamQuestionPeriod: "",
      attachmentIds: [],
    });
    expect(apiRequest).toHaveBeenCalledTimes(3);
  });

  it("updates the selected path id and preserves server-owned identity", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(success(search))
      .mockResolvedValueOnce(success(row))
      .mockResolvedValueOnce(
        success({
          achievement: { ...row, mockExamQuestionPeriod: "5월" },
          occurredDateWarning: false,
        }),
      )
      .mockResolvedValueOnce(success(search));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.click(screen.getByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-mock-period")).toHaveValue("4월"),
    );
    fireEvent.change(screen.getByTestId("improvement-mock-period"), {
      target: { value: "5월" },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("저장되었습니다.");
    const [path, init] = vi.mocked(apiRequest).mock.calls[2];
    expect(path).toBe("/api/business/employment-rate-improvements/81");
    expect(init?.method).toBe("PUT");
    const payload = JSON.parse(String(init?.body));
    expect(payload.mockExamQuestionPeriod).toBe("5월");
    expect(payload.evaluationYear).toBeUndefined();
    expect(payload.teacherUserId).toBeUndefined();
    expect(payload.achievementId).toBeUndefined();
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks %s records",
    async (status) => {
      vi.mocked(apiRequest)
        .mockResolvedValueOnce(success(search))
        .mockResolvedValueOnce(
          success({ ...row, certificationStatus: status }),
        );
      render(<EmploymentRateImprovementsPage user={user} />);
      await screen.findByText("ERI-TEST");
      fireEvent.click(screen.getByTestId("improvement-detail-81"));
      await screen.findByText(
        "확정 또는 제출 상태·소유권 제한으로 수정할 수 없습니다.",
      );
      expect(screen.getByTestId("improvement-save")).toBeDisabled();
      expect(screen.getByTestId("improvement-management-item")).toBeDisabled();
    },
  );

  it("does not show mutation controls for a read-only role", async () => {
    vi.mocked(apiRequest).mockResolvedValue(success(search));
    render(
      <EmploymentRateImprovementsPage user={{ ...user, roles: ["R02"] }} />,
    );
    await screen.findByText("ERI-TEST");
    expect(screen.queryByTestId("improvement-save")).not.toBeInTheDocument();
    expect(screen.getByTestId("improvement-achievementDate")).toBeDisabled();
  });

  it("does not fetch individual operations for R07", () => {
    render(
      <EmploymentRateImprovementsPage user={{ ...user, roles: ["R07"] }} />,
    );
    expect(
      screen.getByText("취업률 제고 실적 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("administrator override remains admitted", async () => {
    vi.mocked(apiRequest).mockResolvedValue(success(search));
    render(
      <EmploymentRateImprovementsPage user={{ ...user, roles: ["R09"] }} />,
    );
    await screen.findByText("ERI-TEST");
    expect(screen.getByTestId("improvement-save")).toBeEnabled();
  });

  it("blocks incomplete and reversed-date forms without sending mutations", async () => {
    vi.mocked(apiRequest).mockResolvedValue(success(search));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.click(screen.getByTestId("improvement-save"));
    expect(screen.getByText("관리항목을 선택하세요.")).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("improvement-management-item"), {
      target: { value: row.managementItemCode },
    });
    fireEvent.change(screen.getByTestId("improvement-achievementDate"), {
      target: { value: row.achievementDate },
    });
    fireEvent.change(
      screen.getByTestId("improvement-specialLectureStartDate"),
      { target: { value: "2026-05-01" } },
    );
    fireEvent.change(screen.getByTestId("improvement-specialLectureEndDate"), {
      target: { value: "2026-04-01" },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    expect(
      screen.getByText("종료일은 시작일 이후여야 합니다."),
    ).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });

  it("cancelled confirmation does not save", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(success(search))
      .mockResolvedValueOnce(success(row));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.click(screen.getByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-mock-period")).toHaveValue("4월"),
    );
    fireEvent.click(screen.getByTestId("improvement-save"));
    expect(apiRequest).toHaveBeenCalledTimes(2);
  });

  it("shows field-object errors without losing entered values", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(success(search))
      .mockResolvedValueOnce(success(row))
      .mockRejectedValueOnce(
        new ApiClientError(400, "항목 오류", {
          code: "VALIDATION_ERROR",
          message: "항목 오류",
          fields: { managementItemCode: "관리항목 설정 오류" } as unknown as [],
        }),
      );
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.click(screen.getByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(screen.getByTestId("improvement-mock-period")).toHaveValue("4월"),
    );
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("관리항목 설정 오류");
    expect(screen.getByTestId("improvement-mock-period")).toHaveValue("4월");
  });

  it("supports loading and empty responses", async () => {
    let complete!: (data: ReturnType<typeof success>) => void;
    vi.mocked(apiRequest).mockImplementation(
      () =>
        new Promise((resolve) => {
          complete = resolve;
        }),
    );
    render(<EmploymentRateImprovementsPage user={user} />);
    expect(screen.getByText("조회 중")).toBeInTheDocument();
    complete(success({ ...search, achievements: [], totalElements: 0 }));
    await screen.findByText("조회 결과가 없습니다");
  });

  it("reports request errors and allows retry", async () => {
    vi.mocked(apiRequest)
      .mockRejectedValueOnce(new Error("서버 오류"))
      .mockResolvedValueOnce(success(search));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("서버 오류");
    fireEvent.click(screen.getByTestId("improvement-search"));
    await screen.findByText("ERI-TEST");
  });

  it("reports server permission failures", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "권한 없음"),
    );
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("취업률 제고 실적 권한이 없습니다");
  });

  it("uses real filter state and page size in list requests", async () => {
    vi.mocked(apiRequest).mockResolvedValue(success(search));
    render(<EmploymentRateImprovementsPage user={user} />);
    await screen.findByText("ERI-TEST");
    fireEvent.change(screen.getByTestId("improvement-filter"), {
      target: { value: "ERI-TEST" },
    });
    fireEvent.click(screen.getByTestId("improvement-search"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/employment-rate-improvements?page=0&pageSize=20&managementNo=ERI-TEST",
      ),
    );
    fireEvent.change(screen.getByTestId("improvement-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/employment-rate-improvements?page=0&pageSize=50&managementNo=ERI-TEST",
      ),
    );
  });
});
