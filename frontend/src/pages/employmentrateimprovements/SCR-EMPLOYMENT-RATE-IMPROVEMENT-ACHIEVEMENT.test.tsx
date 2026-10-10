import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateImprovementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const route = "/faculty/education/employment-rate-improvements";
const user: CurrentUser = {
  userId: 101,
  loginId: "professor1",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 81,
      menuName: "취업률 제고 실적 관리",
      url: route,
      displayOrder: 1,
      children: [],
    },
  ],
};
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user,
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));
const row = {
  achievementId: 81,
  managementNo: "ERI-001",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  specialLectureStartDate: "2026-04-10",
  specialLectureEndDate: "2026-04-17",
  mockExamQuestionPeriod: "모의고사 출제",
  achievementStatus: "DRAFT",
  attachmentRef: null,
};
const result = {
  achievements: [row],
  totalElements: 1,
  managementItemCodes: [row.managementItemCode],
  canCreate: true,
  canUpdate: true,
};
const response = <T,>(data: T) => ({ success: true, data, meta: {} });

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
  vi.spyOn(window, "confirm").mockReturnValue(true);
  window.history.replaceState({}, "", "/");
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method)
      return response({ achievement: row, occurredDateWarning: false });
    return response(path.endsWith("/81") ? row : result);
  });
});

describe("취업률 제고 실적", () => {
  it("authorized navigation reaches the existing-shell screen", async () => {
    window.history.replaceState({}, "", route);
    render(<AppRouter />);
    expect(
      await screen.findByTestId("employment-rate-improvement-page"),
    ).toBeInTheDocument();
    expect(await screen.findByTestId("improvement-row-81")).toBeInTheDocument();
  });

  it("fetches detail and preserves year in PUT while refreshing after success", async () => {
    render(<EmploymentRateImprovementPage user={user} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(
        screen.getByTestId("improvement-mockExamQuestionPeriod"),
      ).toHaveValue("모의고사 출제"),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements/81",
    );
    expect(screen.getByTestId("improvement-evaluationYear")).toBeDisabled();
    fireEvent.change(screen.getByTestId("improvement-mockExamQuestionPeriod"), {
      target: { value: "수정 내역" },
    });
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("저장되었습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/employment-rate-improvements/81");
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({
      mockExamQuestionPeriod: "수정 내역",
    });
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "evaluationYear",
    );
    expect(window.confirm).toHaveBeenCalled();
  });

  it("POST uses create-only payload and shows date warning", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_path, init) =>
      init?.method === "POST"
        ? response({
            achievement: row,
            occurredDateWarning: true,
            warningMessage: "발생일 기간 밖 경고",
          })
        : response(_path.endsWith("/81") ? row : result),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByTestId("improvement-row-81");
    for (const [key, value] of Object.entries({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: row.specialLectureStartDate,
      specialLectureEndDate: row.specialLectureEndDate,
      mockExamQuestionPeriod: row.mockExamQuestionPeriod,
    })) {
      const testId =
        key === "managementItemCode"
          ? "improvement-management-item"
          : `improvement-${key}`;
      fireEvent.change(screen.getByTestId(testId), { target: { value } });
    }
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("발생일 기간 밖 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(call?.[0]).toBe("/api/business/employment-rate-improvements");
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "achievementId",
    );
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks %s rows",
    async (status) => {
      vi.mocked(apiRequest).mockImplementation(async (path) =>
        response(
          path.endsWith("/81") ? { ...row, achievementStatus: status } : result,
        ),
      );
      render(<EmploymentRateImprovementPage user={user} />);
      fireEvent.click(await screen.findByTestId("improvement-detail-81"));
      await screen.findByTestId("improvement-lock");
      expect(screen.getByTestId("improvement-save")).toBeDisabled();
      expect(screen.getByTestId("improvement-attachmentRef")).toBeDisabled();
    },
  );

  it("R02 gets read-only form", async () => {
    render(
      <EmploymentRateImprovementPage user={{ ...user, roles: ["R02"] }} />,
    );
    await screen.findByTestId("improvement-row-81");
    expect(screen.getByTestId("improvement-save")).toBeDisabled();
  });

  it("different-owner detail cannot be edited", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) =>
      response(path.endsWith("/81") ? { ...row, teacherUserId: 102 } : result),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-81"));
    await screen.findByText("본인의 실적만 수정할 수 있습니다.");
    expect(screen.getByTestId("improvement-save")).toBeDisabled();
  });

  it("empty results do not render placeholder rows", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      response({ ...result, achievements: [], totalElements: 0 }),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("조회된 실적이 없습니다");
    expect(screen.queryByTestId("improvement-row-81")).not.toBeInTheDocument();
  });

  it("permission error is explicit", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "범위 밖 요청"),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("접근 권한이 없습니다");
    expect(screen.getByTestId("improvement-save")).toBeDisabled();
  });

  it("failed request offers a query retry", async () => {
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(500, "처리 실패"),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    await screen.findByText("처리 실패");
    fireEvent.click(screen.getByTestId("improvement-search"));
    await screen.findByTestId("improvement-row-81");
  });

  it("renders server field errors without replacing the form", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) =>
      response(path.endsWith("/81") ? row : result),
    );
    render(<EmploymentRateImprovementPage user={user} />);
    fireEvent.click(await screen.findByTestId("improvement-detail-81"));
    await waitFor(() =>
      expect(
        screen.getByTestId("improvement-mockExamQuestionPeriod"),
      ).toHaveValue(row.mockExamQuestionPeriod),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 확인", {
        code: "VALIDATION_ERROR",
        message: "입력 확인",
        fields: [{ field: "managementItemCode", message: "활성 항목만 가능" }],
      }),
    );
    fireEvent.click(screen.getByTestId("improvement-save"));
    await screen.findByText("활성 항목만 가능");
  });
});
