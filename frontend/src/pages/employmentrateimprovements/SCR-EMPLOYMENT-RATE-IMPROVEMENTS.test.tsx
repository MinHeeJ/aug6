import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const teacher: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 81,
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  specialLectureStartDate: "2026-04-10",
  specialLectureEndDate: "2026-04-12",
  mockExamQuestionPeriod: "4월 출제",
  achievementDetail: "{}",
  attachmentIds: '["opaque"]',
  achievementStatus: "DRAFT",
};
function list() {
  return {
    success: true,
    data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
    meta: {},
  };
}

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("loads selected domain detail then PUTs the selected id without changing evaluation year", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "발생일 기간 경고",
        },
        meta: {},
      })
      .mockResolvedValueOnce(list());
    render(<EmploymentRateImprovementsPage user={teacher} />);
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-81"),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("모의시험 출제기간")).toHaveValue(
        "4월 출제",
      ),
    );
    expect(screen.getByLabelText("평가연도")).toHaveAttribute("readonly");
    fireEvent.change(screen.getByLabelText("모의시험 출제기간"), {
      target: { value: "5월 출제" },
    });
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("발생일 기간 경고");
    const putCall = vi
      .mocked(apiRequest)
      .mock.calls.find((call) => call[1]?.method === "PUT");
    expect(putCall?.[0]).toBe("/api/business/employment-rate-improvements/81");
    expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({
      mockExamQuestionPeriod: "5월 출제",
      attachmentIds: ["opaque"],
    });
    expect(JSON.parse(String(putCall?.[1]?.body))).not.toHaveProperty(
      "evaluationYear",
    );
  });

  it("creates via POST and blocks missing required fields before confirming", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list());
    render(<EmploymentRateImprovementsPage user={teacher} />);
    await screen.findByTestId("employment-improvement-detail-81");
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    expect(screen.getByText("관리항목을 입력하세요.")).toBeVisible();
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.filter((call) => call[1]?.method === "POST"),
    ).toHaveLength(0);
    fireEvent.change(screen.getByLabelText("관리항목 *"), {
      target: { value: "EMPLOYMENT_RATE_IMPROVEMENT" },
    });
    fireEvent.change(screen.getByLabelText("업적발생일 *"), {
      target: { value: "2026-04-11" },
    });
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: { achievement: row, occurredDateWarning: false },
      meta: {},
    });
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    await screen.findByText("저장되었습니다.");
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(
          (call) =>
            call[1]?.method === "POST" &&
            call[0] === "/api/business/employment-rate-improvements",
        ),
    ).toBe(true);
  });

  it("keeps confirmed detail read-only and disables saving", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce({
        success: true,
        data: { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
        meta: {},
      });
    render(<EmploymentRateImprovementsPage user={teacher} />);
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-81"),
    );
    await screen.findByText(/평가확정 실적은 잠겨/);
    expect(screen.getByTestId("employment-improvement-save")).toBeDisabled();
    expect(screen.getByLabelText("특강 종료일")).toBeDisabled();
  });

  it("shows a department reader detail but never write controls", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce({ success: true, data: row, meta: {} });
    render(
      <EmploymentRateImprovementsPage user={{ ...teacher, roles: ["R02"] }} />,
    );
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-81"),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("모의시험 출제기간")).toHaveValue(
        "4월 출제",
      ),
    );
    expect(
      screen.queryByTestId("employment-improvement-save"),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("특강 시작일")).toBeDisabled();
  });

  it("R07 cannot auto-call individual list or detail", () => {
    render(
      <EmploymentRateImprovementsPage user={{ ...teacher, roles: ["R07"] }} />,
    );
    expect(screen.getByText(/접근 권한이 없습니다/)).toBeVisible();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("shows empty state", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [],
        page: 0,
        pageSize: 20,
        totalElements: 0,
      },
      meta: {},
    });
    render(<EmploymentRateImprovementsPage user={teacher} />);
    expect(await screen.findByText("데이터 없음")).toBeVisible();
  });

  it("shows failed requests instead of stale rows", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    render(<EmploymentRateImprovementsPage user={teacher} />);
    expect(await screen.findByText("조회 실패")).toBeVisible();
  });

  it("shows permission denial returned by server", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "접근 거부"),
    );
    render(<EmploymentRateImprovementsPage user={teacher} />);
    expect(await screen.findByText(/접근 권한이 없습니다/)).toBeVisible();
  });

  it("renders server field-level validation", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce({ success: true, data: row, meta: {} });
    render(<EmploymentRateImprovementsPage user={teacher} />);
    fireEvent.click(
      await screen.findByTestId("employment-improvement-detail-81"),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("모의시험 출제기간")).toHaveValue(
        "4월 출제",
      ),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 확인", {
        code: "VALIDATION_ERROR",
        message: "입력 확인",
        fields: [
          { field: "managementItemCode", message: "관리항목이 비활성입니다." },
        ],
      }),
    );
    fireEvent.click(screen.getByTestId("employment-improvement-save"));
    expect(await screen.findByText("관리항목이 비활성입니다.")).toBeVisible();
  });
});
