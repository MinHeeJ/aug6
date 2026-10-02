import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { MastersDoctoralGraduationAchievementManagementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row = {
  achievementId: 101,
  managementNo: "B77-DC-001",
  teacherName: "교원",
  managementItemCode: "DEGREE_COMPLETION",
  certificationStatus: "DRAFT",
  students: [
    {
      degreeCompletionStudentId: 1001,
      degreeType: "MASTER" as const,
      studentName: "홍길동",
      thesisTitle: "교육과정 연구",
      degreeAwardedDate: "2026-02-20",
    },
  ],
};

describe("SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("renders the approved route with the persisted student detail sub-table", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
      meta: {},
    });

    render(<MastersDoctoralGraduationAchievementManagementPage />);

    await screen.findByText("B77-DC-001");
    expect(
      screen.getByTestId("degree-completion-achievement-page"),
    ).toHaveAttribute(
      "data-screen-id",
      "SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT",
    );
    fireEvent.click(screen.getByTestId("degree-completion-detail-button"));
    expect(screen.getByTestId("degree-completion-thesis-title-0")).toHaveValue(
      "교육과정 연구",
    );
    fireEvent.change(screen.getByTestId("degree-completion-page-size-select"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(vi.mocked(apiRequest)).toHaveBeenLastCalledWith(
        "/api/business/degree-completion-achievements?page=0&pageSize=50",
      ),
    );
  });

  it("saves all student detail fields and refreshes the list", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      });

    render(<MastersDoctoralGraduationAchievementManagementPage />);

    await screen.findByText("B77-DC-001");
    fireEvent.click(screen.getByTestId("degree-completion-detail-button"));
    fireEvent.click(screen.getByTestId("degree-completion-save-button"));

    await screen.findByText(
      "저장되었습니다. 지도학생 세부내역을 다시 조회했습니다.",
    );
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith(
      "/api/business/degree-completion-achievements",
      expect.objectContaining({ method: "POST" }),
    );
    await waitFor(() => expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(3));
  });

  it("locks student details and save when a confirmed achievement is selected", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [{ ...row, certificationStatus: "EVALUATION_CONFIRMED" }],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });

    render(<MastersDoctoralGraduationAchievementManagementPage />);

    await screen.findByText("B77-DC-001");
    fireEvent.click(screen.getByTestId("degree-completion-detail-button"));

    expect(
      screen.getByTestId("degree-completion-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("degree-completion-save-button")).toBeDisabled();
    expect(
      screen.getByTestId("degree-completion-student-name-0"),
    ).toBeDisabled();
  });
});
